package at.woolph.caco.importer.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.importer.sets.paddingCollectorNumber
import com.opencsv.CSVReader
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileReader

fun String.parseLanguageDeckbox(): CardLanguage = when (this) {
	"English" -> CardLanguage.ENGLISH
	"German" -> CardLanguage.GERMAN
	"Japanese" -> CardLanguage.JAPANESE
	"Russian" -> CardLanguage.RUSSIAN
	"Spanish" -> CardLanguage.SPANISH
	"Korean" -> CardLanguage.KOREAN
	"Italian" -> CardLanguage.ITALIAN
	"Portuguese" -> CardLanguage.PORTUGUESE
	"French" -> CardLanguage.FRENCH
	"Chinese" -> CardLanguage.CHINESE
	"Traditional Chinese" -> CardLanguage.CHINESE_TRADITIONAL
	else -> CardLanguage.UNKNOWN
}
fun CardLanguage.toLanguageDeckbox(): String = when (this) {
    CardLanguage.ENGLISH -> "English"
    CardLanguage.GERMAN -> "German"
    CardLanguage.JAPANESE -> "Japanese"
    CardLanguage.RUSSIAN -> "Russian"
    CardLanguage.SPANISH -> "Spanish"
    CardLanguage.KOREAN -> "Korean"
    CardLanguage.ITALIAN -> "Italian"
    CardLanguage.PORTUGUESE -> "Portuguese"
    CardLanguage.FRENCH -> "French"
    CardLanguage.CHINESE -> "Chinese"
    CardLanguage.CHINESE_TRADITIONAL -> "Traditional Chinese"
    else -> throw Exception("unknown language")
}

fun importDeckbox(file: File) {
    println("importing deckbox collection export file $file")
    transaction {
        CardPossessions.deleteAll()

        val reader = CSVReader(FileReader(file))
        if (reader.readNext() != null) { // skip header
            var nextLine: Array<String>? = reader.readNext()
            while (nextLine != null) {
                try {
                    val count = nextLine[0].toInt()
                    val cardName = nextLine[2]
                    val setName = nextLine[3].removePrefix("Prerelease Events: ").removePrefix("Extras: ").removePrefix("Promos: ").removePrefix("Promo Pack: ")
                    val set = CardSet.find { CardSets.name eq (setNameMapping[setName] ?: setName) }.firstOrNull()
                        ?: throw Exception("set $setName not found")
                    val token = nextLine[3].startsWith("Extras: ")
                    val language = nextLine[6].parseLanguageDeckbox()
                    val condition = when (nextLine[5]) {
                        "Mint", "Near Mint" -> CardCondition.NEAR_MINT
                        "Good (Lightly Played)" -> CardCondition.EXCELLENT
                        "Played" -> CardCondition.GOOD
                        "Heavily Played" -> CardCondition.PLAYED
                        "Poor" -> CardCondition.POOR
                        else -> CardCondition.UNKNOWN
                    }
                    val isPromo = nextLine[3].startsWith("Promos: ") || nextLine[3].startsWith("Promo Pack: ")
					val stampPrereleaseDate = nextLine[3].startsWith("Prerelease Events: ")
                    val foil = when {
						stampPrereleaseDate -> true
                        nextLine[7] == "foil" -> true
                        else -> false
                    }
					val stampPlaneswalkerSymbol = nextLine[12] == "promo"
					val markPlaneswalkerSymbol = nextLine[9] == "proof"
                    val cardNumber = paddingCollectorNumber(when {
                        token -> "T" + nextLine[4]
                        setName == "War of the Spark Japanese Alternate Art" -> nextLine[4]+"★ P"
                        setName == "Jumpstart Front Cards" -> "M" + nextLine[4]
//                        isMemorabilia -> "M" + it.getString("collector_number")
                        isPromo -> nextLine[4]+" P"
                        else -> nextLine[4]
                    })
                    val card = Card.find { (Cards.numberInSet eq cardNumber) and (Cards.set eq set.id) and (Cards.token eq token)}.firstOrNull()
                            ?: throw Exception("card #$cardNumber (\"$cardName\") not found in set $setName")

                    repeat(count) {
                        CardPossession.new {
                            this.card = card
                            this.language = language
                            this.condition = condition
                            this.foil = foil
                            this.stampPrereleaseDate = stampPrereleaseDate
                            this.stampPlaneswalkerSymbol = stampPlaneswalkerSymbol
                            this.markPlaneswalkerSymbol = markPlaneswalkerSymbol
                        }
                    }
                } catch (e: Exception) {
                    println("unable to import: ${e.message}")
                }
                nextLine = reader.readNext()
            }
        }
    }
}

val setNameMapping = mapOf(
    "Ravnica Allegiance Guild Kit" to "RNA Guild Kit",
    "Guilds of Ravnica Guild Kit" to "GRN Guild Kit",
    "Magic 2015 Core Set" to "Magic 2015",
    "Magic 2014 Core Set" to "Magic 2014",
    "War of the Spark Japanese Alternate Art" to "War of the Spark",
    "Global Series: Jiang Yanggu and Mu Yanling" to "Global Series Jiang Yanggu & Mu Yanling",
    "Secret Lair Drop Series" to "Secret Lair Drop",
    "Modern Masters 2017 Edition" to "Modern Masters 2017",
    "Modern Masters 2015 Edition" to "Modern Masters 2015",
    "Ravnica Allegiance Weekend" to "RNA Ravnica Weekend",
    "Guilds of Ravnica Weekend" to "GRN Ravnica Weekend",
    "Modern Horizons Foil" to "Modern Horizons",
    "Jumpstart Front Cards" to "Jumpstart",
    "Fate Reforged Clash Pack Promos" to "Fate Reforged Clash Pack", // TODO eventually integrate CP2 cards as promo cards to FRF
    "Commander" to "Commander 2011",
    "Duel Decks Anthology, Jace vs. Chandra" to "Duel Decks Anthology: Jace vs. Chandra",
    "Duel Decks Anthology, Divine vs. Demonic" to "Duel Decks Anthology: Divine vs. Demonic",
    "Duel Decks Anthology, Elves vs. Goblins" to "Duel Decks Anthology: Elves vs. Goblins",
    "Duel Decks Anthology, Garruk vs. Liliana" to "Duel Decks Anthology: Garruk vs. Liliana",
    "Time Spiral \"Timeshifted\"" to "Time Spiral Remastered",
    "Adventures in the Forgotten Realms Commander" to "Forgotten Realms Commander",
    "Innistrad: Midnight Hunt Commander" to "Midnight Hunt Commander",
    "Innistrad: Crimson Vow Commander" to "Crimson Vow Commander",
    "Kamigawa: Neon Dynasty Commander" to "Neon Dynasty Commander",
)
