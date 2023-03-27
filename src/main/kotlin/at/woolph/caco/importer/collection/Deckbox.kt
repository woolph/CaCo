package at.woolph.caco.importer.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.importer.sets.paddingCollectorNumber
import com.opencsv.CSVReader
import org.jetbrains.exposed.sql.*
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

                    val setName = mapSetName(nextLine[3].removePrefix("Prerelease Events: ").removePrefix("Extras: ").removePrefix("Promos: ").removePrefix("Promo Pack: "))
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
                    val cardNumber = mapSetNumber(nextLine[4], setName).let {paddingCollectorNumber(when {
                        token -> "T$it"
                        setName == "War of the Spark Japanese Alternate Art" -> "$it★ P"
                        setName == "Jumpstart Front Cards" -> "M$it"
//                        isMemorabilia -> "M" + it.getString("collector_number")
                        isPromo -> "$it P"
                        else -> it
                    })
                    }
//                    val type = nextLine.getOrNull(15)
                    val manaCost = nextLine.getOrNull(16)
                    val rarity = nextLine.getOrNull(17)?.let { when(it) {
                        "MythicRare" -> Rarity.MYTHIC
                        "Rare" -> Rarity.RARE
                        "Uncommon" -> Rarity.UNCOMMON
                        "Common" -> Rarity.COMMON
                        else -> null
                    } }

                    if (cardName.startsWith("Art Card:")) {
                        println("skipping $cardName because it is an Art Card")
                    } else {
                        val card = getCard(setName, cardNumber, token, cardName, rarity, manaCost, interactiveMode = false)

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
                    }
                } catch (e: Exception) {
                    println("unable to import: ${e.message}")
                }
                nextLine = reader.readNext()
            }
        }
    }

    println("new setNameMapping")
    println("\tval setNameMapping = mutableMapOf(")
    setNameMapping.forEach { t, u -> println("\"$t\" to \"$u\",") }
    println("\t)")
}

fun getCard(setName: String, cardNumber: String, token: Boolean, cardName: String, rarity: Rarity?, manaCost: String?, interactiveMode: Boolean): Card {
//    val cardSetJoin = Join(ScryfallCardSets, CardSets,
//        onColumn = ScryfallCardSets.set, otherColumn = CardSets.id,
//        joinType = JoinType.INNER,
//        additionalConstraint = { CardSets.name.like(setNameMapping[setName] ?: setName) })
//
//    val cardJoin = Join(Cards, cardSetJoin,
//        onColumn = Cards.set, otherColumn = ScryfallCardSets.id,
//        joinType = JoinType.INNER,
//        additionalConstraint = { Cards.numberInSet.eq(cardNumber) })
//
//    val set = cardSetJoin.slice(ScryfallCardSets.id).selectAll().mapNotNull{
//        return@mapNotNull ScryfallCardSet.findById(it[ScryfallCardSets.id])
//    }.map{it.name}.toList()
//    val cards = cardJoin.slice(Cards.id).selectAll().mapNotNull{
//        return@mapNotNull Card.findById(it[Cards.id])
//    }.map{it.name}.toList()


    val cards = (Cards innerJoin ScryfallCardSets innerJoin CardSets).slice(Cards.id).select {
         CardSets.name.eq(setName) and Cards.numberInSet.eq(cardNumber)
    }.mapNotNull {
        return@mapNotNull Card.findById(it[Cards.id])
    }.toList()

    val card = cards.filter { it.name == cardName }.singleOrNull()

    if (card != null) {
        return card
    } else if (interactiveMode) {
        val possibleCards = Cards.slice(Cards.id).select {
            // FIXME older sets (eg. Portal Second Age have wrong setNumber values! identification via setNumber therefore not possible for these sets
            val s = Cards.numberInSet.eq(cardNumber) and Cards.token.eq(token)
            // and Cards.name.eq(cardName) // token vs. mini-game-cards result in multiples
            if (token) {
                s and Cards.name.eq(cardName)
            } else {
                val s1 = manaCost?.let { s and Cards.manaCost.eq(it) } ?: s
                rarity?.let { s1 and Cards.rarity.eq(it) } ?: s1
            }
        }.mapNotNull { Card.findById(it[Cards.id]) }.toTypedArray()

        if (possibleCards.isEmpty()) {
            throw Exception("card #$cardNumber (\"$cardName\") not found in set $setName")
        }

        println("$setName $cardNumber $cardName -> please select index: (just press enter if no card matches!):")
        possibleCards.forEachIndexed { index, card ->  println("$index ${card.set.set.name} ${card.numberInSet} ${card.name}") }

        val selectedCard = readLine()!!.toIntOrNull()?.let { possibleCards.getOrNull(it) } ?:
            throw Exception("card #$cardNumber (\"$cardName\") not found in set $setName")

        if (!setNameMapping.containsKey(setName) && setName != selectedCard.set.set.name) {
            println("should we add a new set mapping setNameMapping[\"$setName\"] = \"${selectedCard.set.set.name}\"?")
            if (readLine()?.equals("y", ignoreCase = true) == true) {
                setNameMapping[setName] = selectedCard.set.set.name
                // TODO persist setNameMapping additions
            }
        }
        return selectedCard
    } else {
        throw Exception("card $cardNumber (\"$cardName\") not found in set $setName")
    }
}

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
    "Streets of New Capenna Commander" to "New Capenna Commander",
    "Warhammer 40,000" to "Warhammer 40,000 Commander",
)

val setNumberMapping = mutableMapOf<String, Map<IntRange, Int>>(
    "Portal Second Age" to mapOf(
        1..30 to +60,
//        70..60 to 0,
        61..90 to +60,
//        91..120 to 0,
        121..150 to -120,
        151..153 to +12,
//        154..156 to 0,
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
//        70..138 to 70-70,
        139..207 to 277-139,
//        208..276 to 208-208,
        277..345 to 1-277,
//        346..409 to 346-346,
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
)

fun mapSetName(setName: String) = setNameMapping[setName] ?: setName
fun mapSetNumber(setNumber: String, setName: String) = setNumberMapping[setName]?.let { mapData ->
        setNumber.toIntOrNull()?.let { setNumberInt ->
            val delta = mapData.asSequence().firstOrNull { it.key.contains(setNumberInt) }?.value ?: 0
//            if (delta != 0) {
//                println("shifting $setNumber to ${setNumberInt+delta} for $setName")
//            }
            setNumberInt + delta
        }.toString()
    } ?: setNumber
