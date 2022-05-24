package at.woolph.caco.importer.sets

import at.woolph.libs.json.useJsonReader
import at.woolph.libs.json.getJsonObjectArray
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.log.logger
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
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
fun importSet(setCode: String): CardSet {
    println("importing set $setCode")
    Thread.sleep(1) // delay queries to scryfall api (to prevent overloading service)
    val conn = URL("https://api.scryfall.com/sets/$setCode").openConnection() as HttpURLConnection

    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw Exception("Failed : HTTP error code : ${conn.responseCode}")
        }

        conn.inputStream.useJsonReader {
            it.readObject().let {
                if(it.getString("object") == "set") {
                    return CardSet.find { CardSets.shortName.eq(it.getString("code")) }.singleOrNull()?.apply {
                        name = it.getString("name")
                        dateOfRelease = DateTime.parse(it.getString("released_at"))
                        officalCardCount = it.getInt("card_count")
                        digitalOnly = it.getBoolean("digital")
                        icon = URI(it.getString("icon_svg_uri"))
                    } ?: CardSet.new {
                        shortName = it.getString("code")
                        name = it.getString("name")
                        dateOfRelease = DateTime.parse(it.getString("released_at"))
                        officalCardCount = it.getInt("card_count")
                        digitalOnly = it.getBoolean("digital")
                        icon = URI(it.getString("icon_svg_uri"))
                    }
                } else {
                    throw Exception("result is not a set")
                }
            }
        }
    } catch(ex:Exception) {
        throw Exception("unable to import set $setCode", ex)
    } finally {
        conn.disconnect()
    }
}

fun CardSet.update() {
    LOG.debug("update set $this")
    val conn = URL("https://api.scryfall.com/sets/${this.shortName}").openConnection() as HttpURLConnection

    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw Exception("Failed : HTTP error code : ${conn.responseCode}")
        }

        conn.inputStream.useJsonReader {
            it.readObject().let {
                if(it.getString("object") == "set" && !it.getBoolean("digital") && it.getInt("card_count") > 0) {
                    this@update.apply {
                        name = it.getString("name")
                        dateOfRelease = DateTime.parse(it.getString("released_at"))
                        officalCardCount = it.getInt("card_count")
                        icon = URI(it.getString("icon_svg_uri"))
                    }
                } else {
                    throw Exception("result is not a set or it's digital or does not have any cards")
                }
            }
        }
    } catch(ex:Exception) {
        throw Exception("unable to import sets", ex)
    } finally {
        conn.disconnect()
    }
}

fun importSets(): Iterable<CardSet> {
    LOG.debug("importing sets")
    val conn = URL("https://api.scryfall.com/sets").openConnection() as HttpURLConnection

    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw Exception("Failed : HTTP error code : ${conn.responseCode}")
        }

        conn.inputStream.useJsonReader {
            it.readObject().let {
                if(it.getString("object") == "list") {
                    val sets = it.getJsonArray("data").map(JsonValue::asJsonObject).filter { !it.getBoolean("digital") && it.getInt("card_count") > 0 }
                    sets.filter {
                        !it.containsKey("parent_set_code") || !it.getString("code").endsWith(it.getString("parent_set_code"))
                    }.forEach { // FIXME use the icon uri to identify which set's have the same set icon and group them together!!! (but how to handle cards with the "promo" set symbol? (how do i want to file them in my colleciton anyway?)
                        CardSet.find { CardSets.shortName.eq(it.getString("code")) }.singleOrNull()?.apply {
                            name = it.getString("name")
                            dateOfRelease = DateTime.parse(it.getString("released_at"))
                            officalCardCount = it.getInt("card_count")
                            icon = URI(it.getString("icon_svg_uri"))
                            otherScryfallSetCodes = sets.filter { subset -> subset.getString("parent_set_code", null) == it.getString("code") && subset.getString("code").endsWith(subset.getString("parent_set_code")) }.map { it.getString("code") }
                        } ?: CardSet.new {
                            shortName = it.getString("code")
                            name = it.getString("name")
                            dateOfRelease = DateTime.parse(it.getString("released_at"))
                            officalCardCount = it.getInt("card_count")
                            icon = URI(it.getString("icon_svg_uri"))
                            otherScryfallSetCodes = sets.filter { subset -> subset.getString("parent_set_code", null) == it.getString("code") && subset.getString("code").endsWith(subset.getString("parent_set_code")) }.map { it.getString("code") }
                        }
                    }
                } else {
                    throw Exception("result is not a list of sets")
                }
            }
        }
    } catch(ex:Exception) {
        throw Exception("unable to import sets", ex)
    } finally {
        conn.disconnect()
    }

    return CardSet.all()
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
    listOf(shortName, *otherScryfallSetCodes.toTypedArray()).forEach { setCode ->
        try {
            queryPagedData("https://api.scryfall.com/cards/search?q=set%3A${setCode}&unique=prints&order=set") {
                try {
                    if(it.getString("object") == "card") {
                        val cardName = it.getString("name")
                        val isPromo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                        val isToken = it.getString("set_type") == "token"
                        val isMemorabilia = it.getString("set_type") == "memorabilia" // art series, commander special cards (like OC21)
                        val numberInSetImported = paddingCollectorNumber(when {
                            isMemorabilia -> "M" + it.getString("collector_number")
                            isPromo && this.shortName != it.getString("set") -> it.getString("collector_number")+" P"
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
                            Card.find { Cards.set.eq(this@importCardsOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
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
                                this.specialDeckRestrictions = specialDeckRestrictions

                            } ?: Card.new {
                                set = this@importCardsOfSet
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

    additionalLanguages.forEach { language ->
        listOf(shortName).forEach { setCode ->
            try {
                queryPagedData("https://api.scryfall.com/cards/search?q=lang%3A$language%20set%3A${setCode}&unique=prints&order=set") {
                    if(it.getString("object") == "card") {
                        val isPromo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                        val isToken = it.getString("set_type") == "token"
                        val isMemorabilia = it.getString("set_type") == "memorabilia" // art series, commander special cards (like OC21)
                        val numberInSetImported = when {
                            isMemorabilia -> paddingCollectorNumber("M" + it.getString("collector_number"))
                            isPromo -> paddingCollectorNumber(it.getString("collector_number")+" P")
                            isToken -> paddingCollectorNumber("T" + it.getString("collector_number"))
                            else -> paddingCollectorNumber(it.getString("collector_number"))
                        }

                        if (! (isPromo && promoExclusionList.any { promoExclusion -> it.getJsonArray("promo_types")?.containsString(promoExclusion) == true })) {
                            Card.find { Cards.set.eq(this@importCardsOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
                                nameDE = it.getPrintedName()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.error("exception occured during query for language $language https://api.scryfall.com/cards/search?q=lang%3A$language%20set%3A${setCode}&unique=prints&order=set", e)
            }
        }
    }
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
