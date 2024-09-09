package at.woolph.caco

import at.woolph.caco.cli.*
import at.woolph.caco.cli.manabase.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.decks.DecklistPrinter
import at.woolph.caco.gui.MyApp
import at.woolph.caco.importer.collection.importDeckbox
import at.woolph.caco.importer.collection.setNameMapping
import at.woolph.caco.importer.collection.toDeckboxCondition
import at.woolph.caco.importer.collection.toLanguageDeckbox
import at.woolph.caco.importer.sets.*
import at.woolph.caco.importer.sets.LOG
import at.woolph.caco.view.collection.CardPossessionModel
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.libs.files.inputStream
import at.woolph.libs.files.path
import at.woolph.libs.pdf.*
import at.woolph.libs.prompt
import be.quodlibet.boxable.HorizontalAlignment
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.widgets.progress.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.decodeToSequence
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.launch
import java.awt.Color
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.Currency
import kotlin.io.path.*
import kotlin.math.min

class CaCoCli: NoOpCliktCommand(name="caco")

class Ui: CliktCommand(help="User interface", treatUnknownOptionsAsArgs = true) {
    val args by argument().multiple()

    override fun run() {
        launch<MyApp>(*args.toTypedArray())
    }
}


sealed class Either<out A, out B> {
    class Left<A>(val value: A): Either<A, Nothing>()
    class Right<B>(val value: B): Either<Nothing, B>()
}

class ImportScryfall: CliktCommand(name = "scryfall", help="Importing the card data from Scryfall") {
    val source by mutuallyExclusiveOptions(
        option("--bulk-data", help="which bulk data to import").convert { Either.Left(it) },
        option("--file", help="file to import").path(mustExist = true).convert { Either.Right(it) },
    ).single().default(Either.Left("default_cards"))

    override fun run() = runBlocking {
        newSuspendedTransaction {
            importSets().collect {}

            suspend fun processJson(it: InputStream) {
                jsonSerializer.decodeToSequence<ScryfallCard>(it).asFlow()
                    .filter(ScryfallCard::isNoPromoPackStampedAndNoPrereleasePackStampedVersion)
                    .collect {
                        try {
                            Card.newOrUpdate(it.id) {
                                it.update(this)
                            }
                        } catch(t: Throwable) {
                            LOG.error("error while importing card ${it.name}", t)
                        }
                    }
            }

            when(val sourceX = source) {
                is Either.Left -> downloadBulkData(sourceX.value) { processJson(it) }
                is Either.Right -> sourceX.value.inputStream().use { processJson(it) }
            }
        }
    }
}


class ImportSet: CliktCommand(name = "sets", help="Importing the card data from Scryfall") {
    val setCodes by argument(help="sets to be imported").multiple().prompt("Enter the set codes to be imported/updated")
    override fun run() = runBlocking {
        newSuspendedTransaction {
            setCodes.forEach { setCode ->
                    try {
                        echo("importing set $setCode")
                        importSet(setCode.lowercase(Locale.getDefault())).apply {
                            importCardsOfSet()
                        }
                    } catch(ex:Exception) {
                        ex.printStackTrace()
                    }
                }
        }
    }
}

class UpdatesPrices: CliktCommand(name = "prices", help="Updates the price data of the card data from Scryfall") {
    override fun run() = runBlocking {
        newSuspendedTransaction {
            downloadBulkData("default_cards") {
                jsonSerializer.decodeToSequence<ScryfallCard>(it).asFlow()
                    .filter(ScryfallCard::isNoPromoPackStampedAndNoPrereleasePackStampedVersion)
                    .collect {
                        try {
                            Card.newOrUpdate(it.id) {
                                it.update(this)
                                cardmarketUri = it.purchase_uris["cardmarket"]

                                price = it.prices["eur"]?.toDouble()
                                priceFoil = it.prices["eur_foil"]?.toDouble()
                            }
                        } catch(t: Throwable) {
                            LOG.error("error while updating price for card ${it.name}", t)
                        }
                    }
            }
        }
    }
}

class ImportDeckboxCollection: CliktCommand(name = "deckbox-collection", help="Importing the collection from an Deckbox export CSV file") {
    val file by argument(help="The file to import").path(mustExist = true).default(path(System.getProperty("user.home"), "Downloads"))
    override fun run() {
            if(file.isDirectory()) {
                file.useDirectoryEntries { entries ->
                    entries.filter { it.fileName.toString().let { it.startsWith("Inventory") && it.endsWith(".csv")} }
                        .maxByOrNull { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
                }
            } else {
                file
            }?.let {
                importDeckbox(it)
            }
    }
}

class HighValueTradables: CliktCommand(help="Print the tradables above a certain price threshold") {
    val priceThreshold by option(help="The price threshold above which cards are considered 'high value'").double().default(1.0)

    override fun run() {
        transaction {
            CardSet.all().map { set ->
                val cardsSorted = set.cards.sortedBy { it.numberInSet }.map { CardPossessionModel(it, PaperCollectionView.COLLECTION_SETTINGS) }
                set to cardsSorted.asSequence().flatMap { card ->
                    val suffixName = if (cardsSorted.asSequence().filter { it2 ->
                            it2.name.value == card.name.value  && it2.extra.value == card.extra.value
                        }.count() > 1) {
                        val numberInSetWithSameName = cardsSorted.asSequence().filter { it2 ->
                            it2.name.value == card.name.value && it2.extra.value == card.extra.value && it2.numberInSet.value < card.numberInSet.value
                        }.count() + 1
                        " (V.$numberInSetWithSameName)"
                    } else {
                        ""
                    }
                    val suffixSet = when {
                        card.promo.value -> ": Promos"
                        card.extra.value -> ": Extras"
                        else -> ""
                    }

                    val excessNonPremium = card.possessionNonPremium.value-card.possessionNonPremiumTarget.value
                    val excessPremium = card.possessionPremium.value-card.possessionPremiumTarget.value + min(0, excessNonPremium)

                    sequenceOf(
                        Quadruple(excessNonPremium, "${card.name.value}$suffixName", card.set.value!!.set.name.let { "$it$suffixSet" }, card.price.value),
                        Quadruple(excessPremium, "${card.name.value}$suffixName(Foil)", card.set.value!!.set.name.let { "$it$suffixSet" }, card.priceFoil.value),
                    )
                }.filter { it.t1 > 0 && it.t4 >= priceThreshold }.joinToString("\n") { (excess, cardName, setName, price) ->
                    "$excess $cardName ($setName) $price"
                }
            }.forEach { (set, cards) ->
                if (cards.isNotBlank()) {
                    echo("------------------------\n${set.name}\n$cards")
                    echo()
                }
            }
        }
    }
}

class EnterCards: CliktCommand(help="Interactive mode for entering several cards of one set ") {
    val set by argument(help="The set code of the cards to be entered").convert {
        transaction {
            CardSet.findById(it.lowercase()) ?: throw IllegalArgumentException("No set found for set code $it")
        }
    }
    val condition by option(help="The language of the cards")
        .convert { CardCondition.parse(it) }
        .default(CardCondition.NEAR_MINT)
        .validate { it != CardCondition.UNKNOWN }
    val language by option(help="The language of the cards")
        .convert { CardLanguage.parse(it) }
        .prompt("Select the language of the cards")
        .validate { it != CardLanguage.UNKNOWN }

    override fun run() {
        transaction {
            echo("enter cards for ${set.shortName} ${set.name}")
            echo("language = $language")
            echo("condition = $condition")

            File("./import-${set.shortName}.stdin").printWriter().use { stdinPrint ->
                data class PossessionUpdate(
                    val count: Int = 1,
                ) {
                    fun increment() = PossessionUpdate(count + 1)
                }

                val cardPossessionUpdates = mutableMapOf<Pair<Card, Boolean>, Int>()

                lateinit var prevSetNumber: String
                var setNumber = terminal.prompt("collector number")!!.also {
                    stdinPrint.println(it)
                }
                while (setNumber.isNotBlank()) {
                    fun add(setNumber: String) {
                        val foil = setNumber.endsWith("*")
                        val setNumber2 = setNumber.removeSuffix("*").toInt().toString().padStart(3, '0')
                        val card = set.cards.first { it.numberInSet == setNumber2 }
                        echo("add #${card.numberInSet} \"${card.name}\" ${if (foil) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}", trailingNewline = false)
                        cardPossessionUpdates.compute(card to foil) { _, possessionUpdate ->
                            (possessionUpdate ?: 0.also {
                                terminal.danger(" \u001b[31mNeeded for collection!\u001b[0m")
                            }) + 1
                        }
                        echo()
                    }
                    fun remove(setNumber: String) {
                        val foil = setNumber.endsWith("*")
                        val setNumber2 = setNumber.removeSuffix("*").toInt().toString().padStart(3, '0')
                        val card = set.cards.first { it.numberInSet == setNumber2 }
                        echo("removed #${card.numberInSet} \"${card.name}\" ${if (foil) " in \u001B[38:5:0m\u001B[48:5:214mf\u001B[48:5:215mo\u001B[48:5:216mi\u001B[48:5:217ml\u001B[0m" else ""}", trailingNewline = false)
                        cardPossessionUpdates.computeIfPresent(card to foil) { _, possessionUpdate ->
                            possessionUpdate - 1
                        }
                        echo()
                    }
                    prevSetNumber = when (setNumber) {
                        "+" -> { add(prevSetNumber); prevSetNumber }
                        "-" -> {  remove(prevSetNumber); prevSetNumber }
                        else -> { add(setNumber); setNumber }
                    }
                    setNumber = terminal.prompt("collector number")!!.also {
                        stdinPrint.println(it)
                    }
                }

                File("./import-${set.shortName}.csv").printWriter().use { out ->
                    out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")
                    cardPossessionUpdates.forEach { (x, possessionUpdate) ->
                        val (cardInfo, foil) = x
                        val cardName = cardInfo.name
                        val cardNumberInSet = cardInfo.numberInSet
                        val token = cardInfo.token
                        val promo = cardInfo.promo
                        val condition = CardCondition.NEAR_MINT.toDeckboxCondition()
                        val prereleasePromo = false
                        val language = language.toLanguageDeckbox()
                        val setName = setNameMapping.asSequence().firstOrNull { it.value == set.name }?.key ?: set.name.let {
                            when {
                                prereleasePromo -> "Prerelease Events: ${it}"
                                token -> "Extras: ${it}"
                                else -> it
                            }
                        }
                        if (possessionUpdate > 0) {
                            out.println("${possessionUpdate},0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,${if (foil) "foil" else ""},,,,,,,")
                        }
                    }
                }
            }
        }
    }
}

class PrintInventory: CliktCommand(name = "inventory", help="Printing inventory of certain sets") {
    val output by option().path(canBeDir = false).required()
    val sets by argument(help="The set code of the cards to be entered").convert {
        transaction {
            CardSet.findById(it.lowercase()) ?: throw IllegalArgumentException("No set found for set code $it")
        }
    }.multiple().prompt("Enter the set codes to be imported/updated")

    override fun run() = runBlocking {
        newSuspendedTransaction {
            createPdfDocument(output) {
                val fontTitle = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)
                val fontLine = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA), 6.0f)
                sets.forEach { set ->
                    echo("inventory for set ${set.name} into $output")
                    page(PDRectangle.A4) {
                        framePagePosition(50f, 20f, 20f, 20f) {
                            drawText("Inventory ${set.name}", fontTitle, HorizontalAlignment.CENTER, 0f, 10f, Color.BLACK)

                            // TODO calc metrics for all sets (so that formatting is the same for all pages)
                            set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.let {
                                frame(marginTop = fontTitle.height + 20f) {
                                    columns((it.size - 1) / 100 + 1, 100, 5f, 3.5f, fontLine) {
                                        var i = 0
                                        it.filter { !it.token }.forEach {
                                            echo("${it.name} processing")
                                            val ownedCountEN = it.possessions.filter { it.language == CardLanguage.ENGLISH }.count()
                                            val ownedCountDE = it.possessions.filter { it.language == CardLanguage.GERMAN }.count()
                                            this@columns.get(i) {
                                                drawTextWithRects("${it.rarity} ${it.numberInSet} ${it.name}", ownedCountEN, ownedCountDE)
                                            }
                                            i++
                                            echo("${it.name} done")
                                        }

                                        i++
                                        /*
                                        this@columns.get(i) {
                                            drawText("Tokens", Color.BLACK)
                                        }
                                        i++
                                        */
                                        it.filter { it.token }.forEach {
                                            val ownedCountEN = it.possessions.filter { it.language == CardLanguage.ENGLISH }.count()
                                            val ownedCountDE = it.possessions.filter { it.language == CardLanguage.GERMAN }.count()
                                            this@columns.get(i) {
                                                drawTextWithRects("T ${it.numberInSet} ${it.name}", ownedCountEN, ownedCountDE)
                                            }
                                            i++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

class PrintCollectionBinderPageView: CliktCommand(name = "collection-pages", help="Printing a set as it would look like in binder pages (this might help sorting sets where the collection numbers are missing on the card)") {
    val output by option().path(canBeDir = true, canBeFile = false).required()
    val sets by argument(help="The set code of the cards to be entered").multiple().prompt("Enter the set codes to be imported/updated")

    override fun run() = runBlocking<Unit> {
        output.createDirectories()
        CollectionPagePreview(terminal).apply {
            sets.forEach { set ->
                printLabel(set, output.resolve("${set}.pdf"))
            }
        }
    }
}

class PrintPagePositions: CliktCommand(name = "page-position", help="Printing a set as it would look like in binder pages (this might help sorting sets where the collection numbers are missing on the card)") {
    val pocketsPerPage by option(help="The number of pockets per page").int().default(9)

    override fun run() = runBlocking<Unit> {
        PagePositionCalculator(terminal, pocketsPerPage).page()
    }
}

class ImportDecklists: CliktCommand(name = "deckbox-decks", help="importing decklists") {
    val username by option(help="Deckbox username").prompt("Enter the username of the deckbox user")

    override fun run() = runBlocking<Unit> {
        DeckboxDeckImporter(terminal).importDeckboxDecks(username).collect {
            // TODO into database
        }
    }
}

class PrintDecklists: CliktCommand(name = "deckbox-decks", help="printing decklists") {
    val username by option(help="Deckbox username").prompt("Enter the username of the deckbox user")
    val output by option().path(canBeDir = true, canBeFile = true)

    override fun run() = runBlocking<Unit> {
        val progress = progressBarContextLayout<String> {
            percentage()
            progressBar()
            completed(style = terminal.theme.success)
            timeRemaining(style = TextColors.magenta)
            text { "$context" }
        }.animateInCoroutine(terminal, context = "")

        val job = launch { progress.execute() }

        val decklistPrinter = output?.let {
            if (it.isDirectory()) {
                DecklistPrinter.Pdf(it.createDirectories())
            } else {
                DecklistPrinter.PdfOneFile(it.createParentDirectories())
            }
        } ?: DecklistPrinter.Terminal(terminal)

        decklistPrinter.print(DeckboxDeckImporter(terminal, progress).importDeckboxDecks(username).take(20).toList())

        job.cancel("everything is done")
    }
}

class PrintDecklist: CliktCommand(name = "deckbox-deck", help="printing decklist") {
    val url by option(help="Deckbox decklist URL").convert { URI.create(it).toURL() }.prompt("Enter URL")
    val output by option().path(canBeDir = true, canBeFile = true)

    override fun run() = runBlocking<Unit> {
        val decklistPrinter = output?.let {
            DecklistPrinter.Pdf(it)
        } ?: DecklistPrinter.Terminal(terminal)

        DeckboxDeckImporter(terminal).importDeck(url).let { decklistPrinter.print(listOf(it)) }
    }
}

@JvmInline
value class Percentage(val value: Double) {
    override fun toString() = String.format("%1.1f%%", value * 100)
}

object Currencies {
    val USD = Currency.getInstance("USD")
    val EUR = Currency.getInstance("EUR")
    val YEN = Currency.getInstance("JPY")
}

data class CurrencyValue(val value: Double, val currency: Currency): Comparable<CurrencyValue> {
    override fun compareTo(other: CurrencyValue): Int = value.compareTo(other.value)

    override fun toString() = String.format("%.${currency.defaultFractionDigits}f%s", value, currency.symbol)
}

class PrintMissingStats: CliktCommand(name = "missing-stats", help="printing the stats of collection missings things") {
    override fun run() = runBlocking<Unit> {

        newSuspendedTransaction {
            data class MissingStats(
                val count: Int,
                val total: Int,
                val costs: CurrencyValue,
            ) {
                val completionPercentage: Percentage
                    get() = Percentage(1.0 - count.toDouble() / total)
            }
            val missingStatsPerSet = CardSet.all().filter {
                !it.digitalOnly && it.officalCardCount > 50
            }.map {
                val overallCardCount = it.cards.count { !it.token }
                val missingCardsForCollection = it.cards.filter { card -> !card.token && card.possessions.count() < 1 }
                val count = missingCardsForCollection.count()
                val costs = missingCardsForCollection.sumOf { it.price ?: 10.0 }
                it to MissingStats(count, overallCardCount, CurrencyValue(costs, Currencies.USD))
            }.sortedBy { it.second.costs }

            println("Set code\tSet name\tCompletion\tEstimated cost to complete\tMissing cards")
            missingStatsPerSet.forEach { (set, stats) ->
                println("${set.shortName.value}\t${set.name}\t${stats.completionPercentage}\t${stats.costs}\t${stats.count}")
            }
        }
    }
}

class PrintManaBase: CliktCommand(name = "generate-manabase", help="printing decklist") {
    val colorIdentity by option(help="The color identity to generate the mana base for").convert { ColorIdentity(it) }.required()

    override fun run() = runBlocking<Unit> {
        val selectionCriterion = SelectionCriterion(
            ColorIdentity.GRUUL,
            basicLandTypeFactors = 0.1,
            fastStartFactor = 1.5,
            maxPricePerCard = 25.0,
        )
        generateManabase(selectionCriterion, """
1 Abrade
1 Accorder's Shield
1 Arcane Signet
1 Battered Golem
1 Beast Within
1 Blasphemous Act
1 Blood Moon
1 Bone Saw
1 Cathar's Shield
1 Chaos Warp
1 Chrome Mox
1 Claws of Gix
1 Cloud Key
1 Daretti, Scrap Savant
1 Darksteel Relic
1 Elvish Spirit Guide
1 Everflowing Chalice
1 Finale of Devastation
1 Foundry Inspector
1 Fountain of Youth
1 Ghirapur Aether Grid
1 Goblin Engineer
1 Grapeshot
1 Haywire Mite
1 Herbal Poultice
1 Howling Mine
1 Ingenious Artillerist
1 Jeweled Amulet
1 Jeweled Lotus
1 Jhoira's Familiar
1 Liberator, Urza's Battlethopter
1 Lightning Greaves
1 Lotus Petal
1 Memnite
1 Meria, Scholar of Antiquity
1 Mishra's Bauble
1 Mox Amber
1 Mox Opal
1 Myr Retriever
1 Mystic Forge
1 Ornithopter
1 Paradise Mantle
1 Phyrexian Walker
1 Reckless Fireweaver
1 Reckless Handling
1 Sarinth Steelseeker
1 Scrap Trawler
1 Sensei's Divining Top
1 Shield Sphere
1 Shimmer Myr
1 Simian Spirit Guide
1 Sol Ring
1 Spellbook
1 Spidersilk Net
1 Stone of Erech
1 Swiftfoot Boots
1 Sylvan Library
1 Talisman of Impulse
1 The Millennium Calendar
1 Tormod's Crypt
1 Traxos, Scourge of Kroog
1 Unwinding Clock
1 Urza's Bauble
1 Veil of Summer
1 Volatile Wanderglyph
1 Welding Jar
1 Winter Orb
1 Zuran Orb
        """.trimIndent().lines().map { DecklistEntry(it.removePrefix("1 ")) }).forEach { println(it) }
    }
}

fun main(args: Array<String>) {
    Databases.init()

    CaCoCli()
        .subcommands(
            Ui(),
            NoOpCliktCommand(name = "import", help="Importing data").subcommands(
                ImportScryfall(),
                ImportDeckboxCollection(),
                UpdatesPrices(),
                ImportSet(),
                ImportDecklists(),
            ),
            NoOpCliktCommand(name = "print", help="Printing data").subcommands(
                HighValueTradables(),
                PrintInventory(),
                PrintCollectionBinderPageView(),
                PrintPagePositions(),
                PrintDecklist(),
                PrintDecklists(),
                PrintMissingStats(),
            ),
            EnterCards(),
            PrintManaBase(),
        )
        .main(args)
}
