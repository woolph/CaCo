package at.woolph.caco.importer.sets

import at.woolph.libs.json.useJsonReader
import at.woolph.libs.json.getJsonObjectArray
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.newOrUpdate
import at.woolph.libs.log.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.*
import javax.json.*
import kotlin.IllegalStateException
import kotlin.math.max
import kotlin.math.min

private val LOG by logger("at.woolph.caco.importer.sets.Scryfall")

val patternPromoCollectorNumber = Regex("([s★c])$")

/**
 * pads the given collectors number while retaining non-number prefix and non-number suffix
 * for better results when alphabetic sorting of the collector numbers
 * e.g. "T3p" returns "T003p"
 * @param collectorNumber as gained by scryfall API
 * @return padded collector number
 */
fun paddingCollectorNumber(collectorNumber: String): String {
    val pattern = Regex("^([^\\d]*)(\\d*)([^\\d]*)$")
    val (prefix, number, suffix) = pattern.find(collectorNumber)?.let {
        Triple(it.groupValues[1].let { if(it == "TCH") "CH" else it }, it.groupValues[2].toInt(), it.groupValues[3])
    } ?: throw IllegalArgumentException("collectors number \"$collectorNumber\" does not match the regex $pattern")
	val numberPadding = min(max(3 - prefix.length, 1), 3)
    return String.format("%s%0${numberPadding}d%s", prefix, number, suffix)
}

// TODO import MDFC replacements (STX, KHM, ...)
// TODO import double sided tokens as they are printed (especially those of the commander precons)
suspend fun importSet(setCode: String): CardSet = withContext(Dispatchers.IO) {
    HttpClient(CIO).use {
        val response: HttpResponse = it.get("https://api.scryfall.com/sets/$setCode")
        println("importing set $setCode")

        if (!response.status.isSuccess())
            throw Exception("request failed with status code ${response.status.description}")

        val scryfallSet = response.body<ScryfallSet>()

        if (scryfallSet.isNonDigitalSetWithCards()) {
            CardSet.newOrUpdate(scryfallSet.code) { scryfallSet.update(this@newOrUpdate) }
        } else {
            throw Exception("result is not a set or it's digital or does not have any cards")
        }
    }
}

suspend fun CardSet.update() = withContext(Dispatchers.IO) {
    HttpClient(CIO).use {
        LOG.debug("update set $this")
        val response: HttpResponse = it.get("https://api.scryfall.com/sets/${this@update.shortName}")

        if (!response.status.isSuccess())
            throw Exception("request failed with status code ${response.status.description}")

        val scryfallSet = response.body<ScryfallSet>()

        if (scryfallSet.isNonDigitalSetWithCards()) {
            scryfallSet.update(this@update)
        } else {
            throw Exception("result is not a set or it's digital or does not have any cards")
        }
    }
}
@Serializable()
data class ScryfallSet(
    val code: String,
    val parent_set_code: String?,
    val `object`: String,
    val id: String,
    val digital: Boolean,
    val card_count: Int,
    val name: String,
    val released_at: String,
    val icon_svg_uri: String,
) {
    fun isNonDigitalSetWithCards() = `object` == "set" && !digital && card_count > 0
    fun isRootSet() = parent_set_code == null || code.endsWith(parent_set_code)
    fun update(cardSet: CardSet) = cardSet.apply {
        name = this@ScryfallSet.name
        dateOfRelease = LocalDate.parse(this@ScryfallSet.released_at)
        officalCardCount = this@ScryfallSet.card_count
        icon = URI(this@ScryfallSet.icon_svg_uri)
    }
}
@Serializable
data class ScryfallSetList(
    val data: List<ScryfallSet>,
)

private fun importSetsAsIs(): Flow<ScryfallSet> = flow {
    HttpClient(CIO).use {
        val list = withContext(Dispatchers.IO) {
            LOG.debug("importing sets")
            val response: HttpResponse = it.get("https://api.scryfall.com/sets")

            if (!response.status.isSuccess())
                throw Exception("request failed with status code ${response.status.description}")

            response.body<ScryfallSetList>()
        }
        emitAll(list.data.asFlow()
            .filter(ScryfallSet::isNonDigitalSetWithCards)
        )
    }
}

fun importSets(): Flow<CardSet> = flow {
    val sets = importSetsAsIs().toList()

    sets.filter(ScryfallSet::isRootSet).forEach {
        CardSet.newOrUpdate(it.code) { it.update(this) }
    }
    sets.forEach {
        val setId = UUID.fromString(it.id)
        val setFound = CardSet.findById(it.code)
            ?: it.parent_set_code?.let { CardSet.findById(it) }
            ?: throw NoSuchElementException("no card set found for code ${it.code} or ${it.parent_set_code}")

        ScryfallCardSet.newOrUpdate(setId) {
            setCode = it.code
            name = it.name
            set = setFound
        }
    }
    emitAll(CardSet.all().asFlow())
}

fun queryPagedData(startingUri: String, processData: (JsonObject) -> Unit) {
    var nextURL: URL? = URL(startingUri)

    while(nextURL!=null) {
        LOG.debug("requesting $nextURL")
        val conn = nextURL.openConnection() as HttpURLConnection
        nextURL = null
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw Exception("Failed : HTTP error code : ${conn.responseCode}")
        }

        //conn.inputStream.bufferedReader().useLines { it.forEach { println(it) }	}
        conn.inputStream.useJsonReader {
            it.readObject().let{
                if(it.getBoolean("has_more")) {
                    nextURL = URL(it.getString("next_page"))
                }
                it.getJsonObjectArray("data").forEach {
                    processData(it)
                }
            }
        }
        conn.disconnect()
    }
}

fun CardSet.importCardsOfSet(additionalLanguages: List<String> = emptyList()) {
    LOG.info("import cards of set $this")
    // FIXME i want to represent every card as it is in reality (double sided token from commander or master sets etc.)
    val promoExclusionList = listOf("datestamped", "prerelease", "stamped")

    this.scryfallCardSets.map(ScryfallCardSet::setCode).forEach { setCode ->
        try {
            queryPagedData("https://api.scryfall.com/cards/search?q=set%3A${setCode}&unique=prints&order=set") {
                try {
                    if(it.getString("object") == "card") {
                        val scryfallCardSet = ScryfallCardSet[UUID.fromString(it.getString("set_id"))]
                        val cardId = UUID.fromString(it.getString("id"))
                        val cardName = it.getString("name")
                        val isPromo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                        val isToken = it.getString("set_type") == "token"
                        val isMemorabilia = it.getString("set_type") == "memorabilia" // art series, commander special cards (like OC21)
                        val numberInSetImported = paddingCollectorNumber(when {
                            isMemorabilia -> "M" + it.getString("collector_number")
                            isPromo && this.id.value != it.getString("set") -> it.getString("collector_number")+" P"
                            isToken -> "T" + it.getString("collector_number")
                            else -> it.getString("collector_number")
                        })
                        val isExtra = !it.getBoolean("booster")
                        val isNonfoilAvailable = it.getBoolean("nonfoil")
                        val isFoilAvailable = it.getBoolean("foil")
                        val isFullArt = it.getBoolean("full_art")
                        val isExtendedArt = it.getJsonArray("frame_effects")?.containsString("extendedart") ?: false

                        val patternSpecialDeckRestrictions = Regex("A deck can have (any number of cards|only one card|up to (\\w+) cards) named $cardName\\.")
                        val specialDeckRestrictions = it.getString("oracle_text", null)?.let { oracleText ->
                            patternSpecialDeckRestrictions.find(oracleText)?.let {
                                if (it.groupValues[1] == "any number of cards")
                                    Int.MAX_VALUE // TODO
                                else if(it.groupValues[1] == "only one card")
                                    1
                                else when(it.groupValues[2]) {
                                    "one" -> 1
                                    "two" -> 2
                                    "three" -> 3
                                    "four" -> 4
                                    "five" -> 5
                                    "six" -> 6
                                    "seven" -> 7
                                    "eight" -> 8
                                    "nine" -> 9
                                    "ten" -> 10
                                    "eleven" -> 11
                                    "twelve" -> 12
                                    "thirteen" -> 13
                                    "fourteen" -> 14
                                    "fifteen" -> 15
                                    else -> throw IllegalStateException("the following value is not recognized currently: " + it.groupValues[2])
                                }
                            }
                        }

                        val isStampedPromoPackCard = it.containsKey("promo_types") && it.getJsonArray("promo_types").containsString("promopack") && it.getJsonArray("promo_types").containsString("stamped")
                        val isPrereleaseStamped = it.containsKey("promo_types") && it.getJsonArray("promo_types").containsString("prerelease") && it.getJsonArray("promo_types").containsString("datestamped")

                        if (!isPrereleaseStamped && !isStampedPromoPackCard) {
                            Card.newOrUpdate(cardId) {
                                set = scryfallCardSet
                                numberInSet = numberInSetImported
                                name = cardName
                                arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
                                rarity = it.getString("rarity").parseRarity()
                                promo = isPromo
                                token = isToken
                                image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
                                        it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
                                cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }

                                extra = isExtra
                                nonfoilAvailable = isNonfoilAvailable
                                foilAvailable = isFoilAvailable
                                fullArt = isFullArt
                                extendedArt = isExtendedArt

                                val prices = it.getJsonObject("prices")

                                price = if (prices["eur"]?.valueType == JsonValue.ValueType.STRING)
                                    prices.getJsonString("eur").string.toDouble()
                                else null
                                priceFoil = if (prices["eur_foil"]?.valueType == JsonValue.ValueType.STRING)
                                        prices.getJsonString("eur_foil").string.toDouble()
                                    else null

                                this.specialDeckRestrictions = specialDeckRestrictions
                            }
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("exception occured with JsonObject $it", e)
                }
            }
        } catch (e: Exception) {
            LOG.error("exception occured during query https://api.scryfall.com/cards/search?q=set%3A${setCode}&unique=prints&order=set", e)
        }
    }

//    additionalLanguages.forEach { language ->
//        listOf(shortName).forEach { setCode ->
//            try {
//                queryPagedData("https://api.scryfall.com/cards/search?q=lang%3A$language%20set%3A${setCode}&unique=prints&order=set") {
//                    if(it.getString("object") == "card") {
//                        val isPromo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//                        val isToken = it.getString("set_type") == "token"
//                        val isMemorabilia = it.getString("set_type") == "memorabilia" // art series, commander special cards (like OC21)
//                        val numberInSetImported = when {
//                            isMemorabilia -> paddingCollectorNumber("M" + it.getString("collector_number"))
//                            isPromo -> paddingCollectorNumber(it.getString("collector_number")+" P")
//                            isToken -> paddingCollectorNumber("T" + it.getString("collector_number"))
//                            else -> paddingCollectorNumber(it.getString("collector_number"))
//                        }
//
//                        if (! (isPromo && promoExclusionList.any { promoExclusion -> it.getJsonArray("promo_types")?.containsString(promoExclusion) == true })) {
//                            Card.find { Cards.set.eq(this@importCardsOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
//                                nameDE = it.getPrintedName()
//                            }
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                LOG.error("exception occured during query for language $language https://api.scryfall.com/cards/search?q=lang%3A$language%20set%3A${setCode}&unique=prints&order=set", e)
//            }
//        }
//    }
}

//fun CardSet.importCardsOfSetAdditionalLanguage(language: String): Boolean {
//    try{
//        queryPagedData("https://api.scryfall.com/cards/search?q=lang%3A$language%20set%3A${this.shortName}&unique=prints&order=set") {
//            if(it.getString("object") == "card") {
//                val numberInSetImported = paddingCollectorNumber(it.getString("collector_number"))
//                Card.find { Cards.set.eq(this@importCardsOfSetAdditionalLanguage.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
//                    nameDE = it.getPrintedName()
//                } ?: Card.new {
//                    set = this@importCardsOfSetAdditionalLanguage
//                    numberInSet = numberInSetImported
//                    name = it.getString("name")
//                    nameDE = it.getPrintedName()
//                    arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//                    rarity = it.getString("rarity").parseRarity()
//                    promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//                    image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//                            it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//                    cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//                }
//            }
//        }
//        return true
//    } catch (e: RuntimeException) {
//        // TODO there may be no german card names for the set => resulting in error 404!!!
//        return false
//    }
//}
//
//fun CardSet.importTokensOfSet(): Boolean {
//    try{
//        queryPagedData("https://api.scryfall.com/cards/search?q=set%3At${this.shortName}&unique=prints&order=set") {
//            if(it.getString("object") == "card") {
//				val numberInSetImported = paddingCollectorNumber("T" + it.getString("collector_number"))
//                Card.find { Cards.set.eq(this@importTokensOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
//                    name = it.getString("name")
//                    arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//                    rarity = it.getString("rarity").parseRarity()
//                    promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//                    token = true
//                    image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//                            it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//                    cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//                    // TODO update
//                } ?: Card.new {
//                    set = this@importTokensOfSet
//                    numberInSet = numberInSetImported
//                    name = it.getString("name")
//                    arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//                    rarity = it.getString("rarity").parseRarity()
//                    promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//                    token = true
//                    image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//                            it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//                    cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//                }
//            }
//        }
//		// Substitute cards
//		try {
//			queryPagedData("https://api.scryfall.com/cards/search?q=set%3As${this.shortName}&unique=prints&order=set") {
//				if(it.getString("object") == "card") {
//					val numberInSetImported = paddingCollectorNumber("S" + it.getString("collector_number"))
//					Card.find { Cards.set.eq(this@importTokensOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
//						name = it.getString("name")
//						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//						rarity = it.getString("rarity").parseRarity()
//						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//						token = true
//						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//						// TODO update
//					} ?: Card.new {
//						set = this@importTokensOfSet
//						numberInSet = numberInSetImported
//						name = it.getString("name")
//						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//						rarity = it.getString("rarity").parseRarity()
//						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//						token = true
//						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//					}
//				}
//			}
//		} catch (e: RuntimeException) {
//			e.printStackTrace()
//			// TODO there may be no substitute cards for the set => resulting in error 404!!!
//			return false
//		}
//        return true
//    } catch (e: RuntimeException) {
//		e.printStackTrace()
//        // TODO there may be no tokens for the set => resulting in error 404!!!
//        return false
//    }
//}
//
//fun CardSet.importPromosOfSet(): Boolean {
//    try{
//        queryPagedData("https://api.scryfall.com/cards/search?q=set%3Ap${this.shortName}&unique=prints&order=set") {
//            if(it.getString("object") == "card") {
//				if(!it.getJsonArray("promo_types").containsString("datestamped")
//						&& !it.getJsonArray("promo_types").containsString("prerelease")
//						//&& !it.getJsonArray("promo_types").containsString("promopack")
//						&& !it.getJsonArray("promo_types").containsString("promostamped") ) {
//					val numberInSetImported = paddingCollectorNumber(it.getString("collector_number")+" P")
//					Card.find { Cards.set.eq(this@importPromosOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
//						name = it.getString("name")
//						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//						rarity = it.getString("rarity").parseRarity()
//						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//						// TODO update
//					} ?: Card.new {
//						set = this@importPromosOfSet
//						numberInSet = numberInSetImported
//						name = it.getString("name")
//						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
//						rarity = it.getString("rarity").parseRarity()
//						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
//						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
//								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
//						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
//					}
//				}
//
//            }
//        }
//        return true
//    } catch (e: RuntimeException) {
//        // TODO there may be no tokens for the set => resulting in error 404!!!
//        return false
//    }
//}

fun JsonArray.containsString(value: String): Boolean = this.indices.any {
	try {
		this.getString(it) == value
	} catch (e: ClassCastException) {
		false
	}
}

fun JsonObject.getPrintedName(): String? {
    if(contains("printed_name")) {
        return getString("printed_name")
    } else if(contains("card_faces")) {
        val faces = getJsonObjectArray("card_faces")
        val frontName = faces[0].getString("printed_name")
        val backName = faces[1].getString("printed_name")
        return "$frontName // $backName"
    }
    return null
}
