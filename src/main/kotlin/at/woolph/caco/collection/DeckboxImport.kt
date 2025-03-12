package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.*
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.function.Predicate
import kotlin.collections.contains
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.text.lowercase
import kotlin.text.removeSuffix

private val DATE_FORMAT_DECKBOX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
fun importDeckbox(file: Path, notImportedOutputFile: Path = Path.of("not-imported.csv"), datePredicate: Predicate<Instant> =  Predicate { true }, clearBeforeImport: Boolean = false) {
    println("importing deckbox collection export file $file")
    transaction {
        val theListSet = ScryfallCardSet.find { ScryfallCardSets.code eq "plst" }.single()
        val knownSets = ScryfallCardSet.all().associate { it.code to it.name }

        if (clearBeforeImport) {
            println("current collection possessions are cleared!")
            CardPossessions.deleteAll()
        }

        CSVWriter(notImportedOutputFile.bufferedWriter()).use { writer ->
            CSVReader(file.bufferedReader()).use { reader ->
                val header = reader.readNext().also {
                    writer.writeNext(buildList {
                        addAll(it)
                        add("Error")
                    }.toTypedArray(), false)
                }.withIndex().associate { it.value to it.index }

                operator fun Array<String>.get(column: String): String? = header[column]?.let { this[it] }

                var countOfUnparsed = 0
                var countOfSkipped = 0
                var importedCards = 0
                generateSequence { reader.readNext() }.filter { it.size > 1 }.forEach { nextLine ->
                    try {
                        val dateAdded = nextLine["Last Updated"]
                            ?.let { DATE_FORMAT_DECKBOX.parse(it, Instant::from) }
                            ?: Instant.now()

                        if (datePredicate.test(dateAdded ?: Instant.now())) {
                            val count = nextLine["Count"]!!.toInt()
                            val (setCode, setName, isPromo, token, isTheListCard) = editionToSetName(
                                nextLine["Edition Code"]!!,
                                nextLine["Edition"]!!,
                                nextLine["Printing Note"]!!,
                                nextLine["Name"]!!,
                                nextLine["Promo"] == "promo",
                                nextLine["Artist Proof"] == "proof",
                                knownSets,
                            )
                            val printingNote = nextLine["Printing Note"]!!

                            val language = nextLine["Language"]!!.parseLanguageDeckbox()
                            val condition = when (nextLine["Condition"]) {
                                "Mint", "Near Mint" -> CardCondition.NEAR_MINT
                                "Good (Lightly Played)" -> CardCondition.EXCELLENT
                                "Played" -> CardCondition.GOOD
                                "Heavily Played" -> CardCondition.PLAYED
                                "Poor" -> CardCondition.POOR
                                else -> CardCondition.UNKNOWN
                            }

                            val stampPrereleaseDate = nextLine["Edition"]!!.startsWith("Prerelease Events: ")
                            val foil = when {
                                stampPrereleaseDate -> true
                                nextLine["Foil"] == "foil" -> true
                                else -> false
                            }
                            val stampPlaneswalkerSymbol = nextLine["Promo"] == "promo"
                            val markPlaneswalkerSymbol = isTheListCard || nextLine["Artist Proof"] == "proof"

                            val cardName = nextLine["Name"]!!.removePrefix("Token: ").let {
                                when (it) {
                                    in listOf("Plains", "Island", "Swamp", "Mountain", "Forest", "Command Tower") if setCode == "rex" -> "$it // $it"
                                    "Contortionist" if setCode == "tunf" -> "Contortionist // Contortionist"
                                    "Surgeon ~General~ Commander" -> "Surgeon General Commander"
                                    "Double-Faced Card Placeholder" -> "Double-Faced Substitute Card"
                                    "Checklist" -> {
                                        when (setCode) {
                                            "tsoi" -> when(val number = nextLine["Card Number"]!!) {
                                                "19" -> "Shadows Over Innistrad Checklist 1"
                                                "20" -> "Shadows Over Innistrad Checklist 2"
                                                else -> throw NoSuchElementException("unknown checklist card number $number")
                                            }
                                            "temn" -> "Eldritch Moon Checklist"
                                            "txln" -> "Ixalan Checklist"
                                            "trix" -> "Rivals of Ixalan Checklist"
                                            "tm19" -> "Core Set 2019 Checklist"
                                            else -> throw NoSuchElementException("unknown checklist in set $setCode")
                                        }
                                    }
                                    else -> it
                                }
                            }.let {
                                if (it.startsWith("Emblem: "))
                                    "${it.removePrefix("Emblem: ")} Emblem"
                                else {
                                    it
                                }
                            }
                            val cardNumber = mapSetNumber(nextLine["Card Number"]!!, setName).let {
                                when (cardName) {
                                    "Innistrad Checklist",
                                    "Shadows Over Innistrad Checklist 1",
                                    "Eldritch Moon Checklist",
                                    "Ixalan Checklist",
                                    "Rivals of Ixalan Checklist",
                                    "Core Set 2019 Checklist" -> "CH1"
                                    "Shadows Over Innistrad Checklist 2" -> "CH2"
                                    "War Elephant" if it == "68" -> "11" // FIXME check whether this is true
                                    "Thallid" if it == "87" -> "74a" // FIXME find a more elegant solution to this issue (e.g. for sets with this kind of set numbering, generate a deckbox numbering
                                    "Benthic Explorers" if it == "36" -> "24a"
                                    "Gorilla Shaman" if it == "106" -> "72a"
                                    "Gorilla Shaman" if it == "107" -> "72b"
                                    "Lat-Nam's Legacy" if it == "44" -> "30a"
                                    "Lat-Nam's Legacy" if it == "45" -> "30b"
                                    else if printingNote matches Regex("[a-z]") -> "$it$printingNote"
                                    else if setName == "War of the Spark Japanese Alternate Art" -> "$it★ P"
                                    else -> it
                                }
                            }.let {
                                when(setCode) {
                                    "plst" -> {
                                        if (nextLine["Artist Proof"] == "proof") {
                                            "${mapEditionCode(nextLine["Edition Code"]!!).uppercase()}-$it"
                                        } else {
                                            Card.find { Cards.set eq theListSet.id and (Cards.name eq cardName and (Cards.collectorNumber like "$printingNote-%")) }.singleOrNull()?.collectorNumber ?: throw Exception("The List Card $cardName not found")
                                        }
                                    }
                                    "prwk" -> "A%02d".format(it.toInt())
                                    "prw2" -> "B%02d".format(it.toInt())
                                    else -> it
                                }
                            }
                            val manaCost = nextLine["Cost"]
                            val rarity = nextLine["Rarity"]?.let {
                                when (it) {
                                    "MythicRare" -> Rarity.MYTHIC
                                    "Rare" -> Rarity.RARE
                                    "Uncommon" -> Rarity.UNCOMMON
                                    "Common" -> Rarity.COMMON
                                    else -> null
                                }
                            }
                            if (token && cardName.contains(" // ")) {
    //                            println("skipping $cardName because it is an Double Sided Token which is not supported")
                            } else if (cardName.startsWith("Art Card:")) {
    //                            println("skipping $cardName because it is an Art Card")
                            } else if (cardName == "Ability Punchcard") {
    //                            println("skipping $cardName cause Ability Punchcard's are not covered by scryfall")
                            } else {
                                val card = getCard(
                                    setCode,
                                    setName,
                                    cardNumber,
                                    token,
                                    cardName,
                                    rarity,
                                    manaCost,
                                    promo = isPromo,
                                )

                                repeat(count) {
                                    CardPossession.new {
                                        this.card = card
                                        this.language = language
                                        this.condition = condition
                                        this.foil = foil
                                        this.stampPrereleaseDate = stampPrereleaseDate
                                        this.stampPlaneswalkerSymbol = stampPlaneswalkerSymbol
                                        this.dateOfAddition = dateAdded
                                    }
                                    importedCards ++
                                }
                            }
                        } else {
                            println("not imported due to date restriction")
                            countOfSkipped++
                            writer.writeNext(buildList {
                                addAll(nextLine)
                                add("not imported due to date restriction")
                            }.toTypedArray(), false)
                        }
                    } catch (e: Exception) {
                        println("unable to import: ${e.message}")
                        countOfUnparsed++
                        writer.writeNext(buildList {
                            addAll(nextLine)
                            add("${e.message}")
                        }.toTypedArray(), false)
                    }
                }
                if (countOfSkipped > 0) {
                    println("$countOfSkipped were skipped due to date predicate!!")
                }
                if (countOfUnparsed > 0) {
                    println("unable to import $countOfUnparsed lines")
                } else {
                    println("all cards imported successfully ($importedCards in total)!!")
                }
            }
        }
    }
}

data class MetaInfo(
    val setCode: String,
    val setName: String,
    val promo: Boolean,
    val isToken: Boolean,
    val isTheListCard: Boolean,
    val isSubstituteCard: Boolean,
    val isSetNumberReliable: Boolean? = null,
)

val tokenPrefixes = listOf("Extras: ")
val prereleasePrefix = listOf("Prerelease Events: ")
val promoPrefixes = listOf("Promos: ", "Promo Pack: ")
val promoSuffixes = listOf(" Buy-A-Box Promo", " Open House", " Promos", " Promo Pack", " Draft Weekend", " Planeswalker Weekend")
val allPrefixes = prereleasePrefix + promoPrefixes + tokenPrefixes
val allSuffixes = promoSuffixes

val STORE_CHAMPIONSHIPS = "Store Championships"
fun determineScryfallSetName(edition: String, isPromo: Boolean, isToken: Boolean, isSubstituteCard: Boolean) = mapSetName(allSuffixes.fold(allPrefixes.fold(edition, String::removePrefix), String::removeSuffix)).let {
    when {
        isSubstituteCard -> "${it.removeSuffix(" Placeholders")} Substitute Cards"
        isPromo && it == "Commander Legends: Battle for Baldur's Gate" -> "Battle for Baldur's Gate Promos"
        isToken && it == "Commander Legends: Battle for Baldur's Gate" -> "Battle for Baldur's Gate Tokens"
        isPromo && it != IxalanTreasureChest && it != STORE_CHAMPIONSHIPS && it != GRN_RAVNICA_WEEKEND && it != RNA_RAVNICA_WEEKEND && !it.endsWith(" Standard Showdown") && !it.startsWith("Friday Night Magic ") -> "${it.removeSuffix(" Store Championship")} Promos"
        isToken -> "$it Tokens"
        else -> it
    }
}

fun editionToSetName(editionCode: String, edition: String, printingNote: String, deckboxCardName: String, isPromo: Boolean, isTheListCardStoredAsRegularVersionWithArtistProofTag: Boolean, knownSets: Map<String, String>): MetaInfo {
    val isSubstituteCard = deckboxCardName == "Double-Faced Card Placeholder"
    val printingNote = if (isTheListCardStoredAsRegularVersionWithArtistProofTag) editionCode else printingNote
    val editionCode = if (isTheListCardStoredAsRegularVersionWithArtistProofTag) "plist" else editionCode
    return when(val lowercaseEditionCode = editionCode.lowercase()) {
        "plist" -> {
            val isToken = false
            val setCode = "plst"
            val setName = knownSets[setCode] ?: throw Exception("The List Card set code $setCode unknown")

            MetaInfo(setCode, setName, isPromo, isToken, true, isSubstituteCard, isSetNumberReliable = false)
        }
        "ptc" if deckboxCardName == "Silent Sentinel" -> MetaInfo("pbng", "Born of the Gods Promos", true, false, false, false)
        "rep" if deckboxCardName == "Rukh Egg" -> MetaInfo("p8ed", "Eight Edition Promos", true, false, false, false)
        "pm20" if deckboxCardName in listOf("Plains", "Island", "Swamp", "Mountain", "Forest") -> MetaInfo("ppp1", "M20 Promo Packs", true, false, false, false)
        "prw2" if deckboxCardName == "Lavinia, Azorius Renegade" -> MetaInfo("prna", "Ravnica Allegiance Promos", true, false, false, false)
        "gpx" ->  MetaInfo("pf19", "MagicFest 2019", true, false, false, false, isSetNumberReliable = false)
        "ssp_1" if deckboxCardName == "Steel Leaf Champion" -> MetaInfo("pdom", "Dominaria Promos", true, false, false, false)
        "ssp_1" if deckboxCardName == "Ghalta, Primal Hunger" -> MetaInfo("prix", "Rivals of Ixalan Promos", true, false, false, false)
        "plgs" if deckboxCardName == "Reliquary Tower" || deckboxCardName == "Hangarback Walker" ->  MetaInfo("plg20", "Love Your LGS 2020", true, false, false, false)
        "plgs" if deckboxCardName == "Thought Vessel" || deckboxCardName == "Sol Ring" ->  MetaInfo("plg22", "Love Your LGS 2022", true, false, false, false)
        "fnmp" if deckboxCardName == "Grisly Salvage" -> MetaInfo("f13", "Friday Night Magic 2013", true, false, false, false)
        "fnmp" -> {
            val isPromo = true
            val isToken = false
            val regex = Regex("^(January|February|March|April|May|June|July|August|September|October|November|December) (?=\\d{4}$)")
            val setName = determineScryfallSetName(if (regex.find(printingNote) != null) {
                "Friday Night Magic ${printingNote.replace(regex, "")}"
            } else printingNote, isPromo, isToken, isSubstituteCard)
            val setCode = setCodeBySetName(setName, knownSets) ?: throw Exception("Card from PromoSet with Actual Set ($setName) unknown")

            MetaInfo(setCode, setName, isPromo, isToken, false, isSubstituteCard, isSetNumberReliable = false)
        }
        in promoSetCodeWithActualEditionInPrintingNode -> {
            val isPromo = true
            val isToken = false
            val setName = determineScryfallSetName(printingNote, isPromo, isToken, isSubstituteCard)
            val setCode = setCodeBySetName(setName, knownSets) ?: throw Exception("Card from PromoSet with Actual Set in PrintingNote ($setName) unknown")

            MetaInfo(setCode, setName, isPromo, isToken, false, isSubstituteCard)
        }
        else -> {
            val isPromo = isPromo || promoPrefixes.any(edition::startsWith) || promoSuffixes.any(edition::endsWith) || printingNote in promoPrintingNotes || lowercaseEditionCode in dedicatedPromoSets
            val isToken = tokenPrefixes.any(edition::startsWith)

            val setName = determineScryfallSetName(edition, isPromo, isToken, isSubstituteCard)
            val setCode = mapEditionCode(lowercaseEditionCode, isSubstituteCard).takeIf { it in knownSets }
                ?: setCodeBySetName(setName, knownSets) ?: lowercaseEditionCode

            return MetaInfo(setCode, setName, isPromo, isToken, false, isSubstituteCard)
        }
    }
}

val promoPrintingNotes = listOf("Bundle Foil Promo", "Promo", "Buy-A-Box Promo")

fun getCard(setCode: String, setName: String, cardNumber: String, token: Boolean, cardName: String, rarity: Rarity?, manaCost: String?, promo: Boolean): Card {
    fun cardByNumber(setCode: String, promo: Boolean? = null): Card? =
        (Cards innerJoin ScryfallCardSets).select(Cards.id)
            .where { ScryfallCardSets.code.eq(setCode) and (Cards.collectorNumber.eq(cardNumber)) }
            .mapNotNull { Card.findById(it[Cards.id]) }
            .singleOrNull { it.name == cardName && (promo == null || it.promo == promo) }

    fun cardByName(setCode: String, promo: Boolean = false): Card? =
        (Cards innerJoin ScryfallCardSets).select(Cards.id)
            .where { ScryfallCardSets.code.eq(setCode) and (Cards.name.eq(cardName)) }
            .mapNotNull { Card.findById(it[Cards.id]) }
            .singleOrNull { it.promo == promo }

    return cardByNumber(setCode)
        ?: cardByName(setCode, promo)
        ?: cardByNumber(setCode.removePrefix("p"), true)
        ?: cardByName(setCode.removePrefix("p"), true)
        ?: throw Exception("card $cardNumber (\"$cardName\") not found in set [$setCode] $setName")
}

fun mapEditionCode(editionCode: String, isSubstituteCard: Boolean = false): String = editionCode.lowercase().let {
        if (isSubstituteCard && it.startsWith("t")) {
            it.replaceFirst('t', 's')
        } else {
            it
        }
    }.let { setCode ->
        setCodeMapping.getOrDefault(setCode, setCode)
    }

fun setCodeBySetName(setName: String, knownSets: Map<String, String>): String? =
    knownSets.entries.firstOrNull { it.value == setName }?.key

val dedicatedPromoSets = listOf("pw23", "pw24", "ptg", "prwk", "prw2", "pro")
val promoSetCodeWithActualEditionInPrintingNode = listOf(
    "mlp",
    "lpromos",
    "bftcp",
    "fnmp",
    "mgdc",
    "rep",
    "mbp",
    "ptc",
    "ssp_1",
    "grc",
)
val setCodeMapping = mutableMapOf(
    "gu" to "ulg",
    "uz" to "usg",
    "cg" to "uds",
    "4e" to "4ed",
    "5e" to "5ed",
    "6e" to "6ed",
    "p2" to "p02",
    "pr" to "pcy",
    "an" to "arn",
    "ne" to "nem",
    "in" to "inv",
    "mm" to "mmq",
    "ap" to "apc",
    "mi" to "mir",
    "ps" to "pls",
    "ex" to "exo",
    "od" to "ody",
    "st" to "sth",
    "te" to "tmp",
    "vi" to "vis",
    "ia" to "ice",
    "wl" to "wth",
    "al" to "all",
    "fe" to "fem",
    "hm" to "hml",
    "le" to "leg",
    "dd3_jvc" to "dd2",
)

fun mapSetName(setName: String) = setNameMapping[setName] ?: setName

val IxalanTreasureChest = "XLN Treasure Chest"
val RNA_RAVNICA_WEEKEND = "RNA Ravnica Weekend"
val GRN_RAVNICA_WEEKEND = "GRN Ravnica Weekend"
/**
 * key is deckbox name
 * value is scryfall name
 */
val setNameMapping = mutableMapOf(
    "Ravnica Allegiance Guild Kit" to "RNA Guild Kit",
    "Guilds of Ravnica Guild Kit" to "GRN Guild Kit",
    "Magic 2015 Core Set" to "Magic 2015",
    "Magic 2014 Core Set" to "Magic 2014",
    "War of the Spark Japanese Alternate Art" to "War of the Spark",
    "Global Series: Jiang Yanggu and Mu Yanling" to "Global Series Jiang Yanggu & Mu Yanling",
    "Secret Lair Drop Series" to "Secret Lair Drop",
    "Modern Masters 2017 Edition" to "Modern Masters 2017",
    "Modern Masters 2015 Edition" to "Modern Masters 2015",
    "Ravnica Allegiance Weekend" to RNA_RAVNICA_WEEKEND,
    "Guilds of Ravnica Weekend" to GRN_RAVNICA_WEEKEND,
    "Modern Horizons Foil" to "Modern Horizons",
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
    "Streets of New Capenna Commander" to "New Capenna Commander",
    "Warhammer 40,000" to "Warhammer 40,000 Commander",
    "The Lord of the Rings: Tales of Middle-earth Commander" to "Tales of Middle-earth Commander",
    "Lost Caverns of Ixalan Commander" to "The Lost Caverns of Ixalan Commander",
    "2017 Ixalan Treasure Chest" to IxalanTreasureChest,
    "2023 Store Championship" to STORE_CHAMPIONSHIPS,
    "Pro Tour / Planeswalker Championship" to "Pro Tour",
    "Core Set 2019, Alayna Danner" to "M19 Standard Showdown",
    "Oversized: Commander 2011" to "Commander 2011 Oversized",
    "Oversized: Commander 2013" to "Commander 2013 Oversized",
    "Oversized: Commander 2014" to "Commander 2014 Oversized",
    "Oversized: Commander 2015" to "Commander 2015 Oversized",
    "Oversized: Commander 2016" to "Commander 2016 Oversized",
    "Oversized: Commander 2017" to "Commander 2017 Oversized",
    "Oversized: Commander 2018" to "Commander 2018 Oversized",
    "Oversized: Commander 2019" to "Commander 2019 Oversized",
    "Oversized: Commander 2020" to "Commander 2020 Oversized",
    "Oversized: Adventures in the Forgotten Realms" to "Adventures in the Forgotten Realms Oversized",
)

fun mapSetNumber(setNumber: String, setName: String) = setNumberMapping[setName]?.let { mapData ->
    setNumber.toIntOrNull()?.let { setNumberInt ->
        val delta = mapData.asSequence().firstOrNull { it.key.contains(setNumberInt) }?.value ?: 0
        setNumberInt + delta
    }.toString()
} ?: setNumber

val setNumberMapping = mutableMapOf<String, Map<IntRange, Int>>(
    "Portal Second Age" to mapOf(
        1..30 to +60,
        61..90 to +60,
        121..150 to -120,
        151..153 to +12,
        157..159 to +3,
        160..162 to -9,
        163..165 to -6,
    ),
    "Portal" to mapOf(
        1..19 to 79-1,
        20..40 to 97-20,
        41..79 to 40-41,
        80..82 to 157-80,
        83..88 to 159-83,
        89..99 to 164-89,
        100..121 to 174-100,
        122..123 to 118-122,
        124..151 to 119-124,
        152..162 to 146-152,
        163..201 to 1-163,
        202..202 to 39-202,
        203..206 to 212-203,
        207..210 to 200-207,
        211..214 to 208-211,
        215..218 to 196-215,
        219..222 to 204-219,
        ),
    "Fifth Edition" to mapOf(
        1..69 to 139-1, // diff = target number minus start
        139..207 to 277-139,
        277..345 to 1-277,
        417..420 to 446-417,
        425..428 to 434-425,
        430..433 to 442-430,
        434..437 to 430-434,
        442..445 to 438-442,
    ),
    "Fourth Edition" to mapOf(
        1..58 to 117-1,
        59..116 to 59-59,
        117..174 to 233-117,
        175..177 to 376-175,
        178..180 to 367-178,
        181..181 to 361-181,
        182..184 to 373-182,
        185..185 to 362-185,
        186..188 to 364-186,
        189..189 to 363-189,
        190..192 to 370-190,
        193..250 to 175-193,
        251..308 to 1-251,
        309..378 to 291-309,
        ),
    "Mirage" to mapOf(
        1..51 to 103-1,
        52..102 to 52-52,
        103..153 to 205-103,
        154..204 to 154-154,
        205..255 to 1-205,
        256..288 to 291-256,
        289..291 to 324-289,
        292..295 to 347-292,
        296..296 to 327-296,
        297..300 to 335-297,
        301..304 to 343-301,
        305..305 to 328-305,
        306..309 to 331-306,
        310..310 to 329-310,
        311..314 to 339-311,
        315..315 to 330-315,
        316..350 to 256-316,
        ),
    "Visions" to mapOf(
        1..25 to 51-1,
        26..50 to 26-26,
        51..75 to 101-51,
        76..100 to 76-76,
        101..125 to 1-101,
        126..140 to 126-126,
        141..159 to 141-141,
        160..167 to 160-160,
        ),
    "Weatherlight" to mapOf(
        1..29 to 59-1,
        30..58 to 30-30,
        59..87 to 117-59,
        88..116 to 88-88,
        117..145 to 1-117,
        146..163 to 146-146,
        164..167 to 164-164,
    ),
    "Tempest" to mapOf(
        1..53 to 107-1,
        54..106 to 54-54,
        107..159 to 213-107,
        160..212 to 160-160,
        213..265 to 1-213,
        266..304 to 276-266,
        305..307 to 315-305,
        308..311 to 347-308,
        312..312 to 318-312,
        313..316 to 335-313,
        317..318 to 319-317,
        319..322 to 343-319,
        323..323 to 321-323,
        324..327 to 331-324,
        328..333 to 322-328,
        334..337 to 339-334,
        338..340 to 328-338,
        341..350 to 266-341,
        ),
    "Stronghold" to mapOf(
        1..25 to 51-1,
        26..50 to 26-26,
        51..75 to 101-51,
        76..100 to 76-76,
        101..125 to 1-101,
        126..136 to 132-126,
        137..137 to 143-137,
        138..143 to 126-138,
        ),
    "Innistrad: Midnight Hunt Substitute Cards" to
        mapOf(
            80..88 to 1-80,
        ),
    "The Brothers' War Substitute Cards" to
        mapOf(
            28..28 to 1-28,
        ),
    "March of the Machine Substitute Cards" to
        mapOf(
            24..24 to 1-24,
        ),
    "The Lost Caverns of Ixalan Substitute Cards" to
        mapOf(
            101..101 to 1-101,
        ),
)

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
