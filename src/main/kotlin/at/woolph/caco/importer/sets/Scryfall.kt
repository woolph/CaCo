package at.woolph.caco.importer.sets

import at.woolph.libs.json.useJsonReader
import at.woolph.libs.json.getJsonObjectArray
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.json.*

val patternPromoCollectorNumber = Regex("(s|★|c)$")

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
        Triple(it.groupValues[1], it.groupValues[2].toInt(), it.groupValues[3])
    } ?: throw IllegalArgumentException("collectors number does not match the regex $pattern")
    return String.format("%s%03d%s", prefix, number, suffix)
}

fun importSet(setCode: String): CardSet {
    println("importing set $setCode")
    Thread.sleep(1000) // delay queries to scryfall api (to prevent overloading service)
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

fun queryPagedData(startingUri: String, processData: (JsonObject) -> Unit) {
    var nextURL: URL? = URL(startingUri)

    while(nextURL!=null) {
        println("requesting $nextURL")
        Thread.sleep(1000) // delay queries to scryfall api (to prevent overloading service)
        val conn = nextURL.openConnection() as HttpURLConnection
        nextURL = null
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw RuntimeException("Failed : HTTP error code : ${conn.responseCode}")
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

fun CardSet.importCardsOfSet() {
    queryPagedData("https://api.scryfall.com/cards/search?q=set%3A${this.shortName}&unique=prints&order=set") {
        if(it.getString("object") == "card") {
            val numberInSetImported = paddingCollectorNumber(it.getString("collector_number"))
            Card.find { Cards.set.eq(this@importCardsOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
                name = it.getString("name")
                arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
                rarity = it.getString("rarity").parseRarity()
                promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
                        it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
                cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
            } ?: Card.new {
                set = this@importCardsOfSet
                numberInSet = numberInSetImported
                name = it.getString("name")
                arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
                rarity = it.getString("rarity").parseRarity()
                promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
                        it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
                cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
            }
        }
    }
}

fun CardSet.importCardsOfSetAdditionalLanguage(language: String): Boolean {
    try{
        queryPagedData("https://api.scryfall.com/cards/search?q=lang%3A$language%20set%3A${this.shortName}&unique=prints&order=set") {
            if(it.getString("object") == "card") {
                val numberInSetImported = paddingCollectorNumber(it.getString("collector_number"))
                Card.find { Cards.set.eq(this@importCardsOfSetAdditionalLanguage.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
                    nameDE = it.getPrintedName()
                } ?: Card.new {
                    set = this@importCardsOfSetAdditionalLanguage
                    numberInSet = numberInSetImported
                    name = it.getString("name")
                    nameDE = it.getPrintedName()
                    arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
                    rarity = it.getString("rarity").parseRarity()
                    promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                    image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
                            it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
                    cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
                }
            }
        }
        return true
    } catch (e: RuntimeException) {
        // TODO there may be no german card names for the set => resulting in error 404!!!
        return false
    }
}

fun CardSet.importTokensOfSet(): Boolean {
    try{
        queryPagedData("https://api.scryfall.com/cards/search?q=set%3At${this.shortName}&unique=prints&order=set") {
            if(it.getString("object") == "card" && !it.getString("collector_number").startsWith("CH")) { // ignore checklists // TODO remove checkklist-skip?!
                val numberInSetImported = paddingCollectorNumber("T" + it.getString("collector_number"))
                Card.find { Cards.set.eq(this@importTokensOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
                    name = it.getString("name")
                    arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
                    rarity = it.getString("rarity").parseRarity()
                    promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                    token = true
                    image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
                            it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
                    cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
                    // TODO update
                } ?: Card.new {
                    set = this@importTokensOfSet
                    numberInSet = numberInSetImported
                    name = it.getString("name")
                    arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
                    rarity = it.getString("rarity").parseRarity()
                    promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
                    token = true
                    image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
                            it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
                    cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
                }
            }
        }
		try {
			queryPagedData("https://api.scryfall.com/cards/search?q=set%3As${this.shortName}&unique=prints&order=set") {
				if(it.getString("object") == "card" && !it.getString("collector_number").startsWith("CH")) { // ignore checklists // TODO remove checkklist-skip?!
					val numberInSetImported = paddingCollectorNumber("S" + it.getString("collector_number"))
					Card.find { Cards.set.eq(this@importTokensOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
						name = it.getString("name")
						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
						rarity = it.getString("rarity").parseRarity()
						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
						token = true
						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
						// TODO update
					} ?: Card.new {
						set = this@importTokensOfSet
						numberInSet = numberInSetImported
						name = it.getString("name")
						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
						rarity = it.getString("rarity").parseRarity()
						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
						token = true
						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
					}
				}
			}
		} catch (e: RuntimeException) {
			e.printStackTrace()
			// TODO there may be no substitute cards for the set => resulting in error 404!!!
			return false
		}
        return true
    } catch (e: RuntimeException) {
		e.printStackTrace()
        // TODO there may be no tokens for the set => resulting in error 404!!!
        return false
    }
}

fun CardSet.importPromosOfSet(): Boolean {
    try{
        queryPagedData("https://api.scryfall.com/cards/search?q=set%3Ap${this.shortName}&unique=prints&order=set") {
            if(it.getString("object") == "card") {
				if(!it.getJsonArray("promo_types").containsString("datestamped")
						&& !it.getJsonArray("promo_types").containsString("prerelease")
						//&& !it.getJsonArray("promo_types").containsString("promopack")
						&& !it.getJsonArray("promo_types").containsString("promostamped") ) {
					val numberInSetImported = paddingCollectorNumber("P" + it.getString("collector_number"))
					Card.find { Cards.set.eq(this@importPromosOfSet.id).and(Cards.numberInSet.eq(numberInSetImported)) }.singleOrNull()?.apply {
						name = it.getString("name")
						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
						rarity = it.getString("rarity").parseRarity()
						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
						// TODO update
					} ?: Card.new {
						set = this@importPromosOfSet
						numberInSet = numberInSetImported
						name = it.getString("name")
						arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
						rarity = it.getString("rarity").parseRarity()
						promo = it.getBoolean("promo") || it.getString("collector_number").contains(patternPromoCollectorNumber)
						image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
								it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
						cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
					}
				}

            }
        }
        return true
    } catch (e: RuntimeException) {
        // TODO there may be no tokens for the set => resulting in error 404!!!
        return false
    }
}

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
