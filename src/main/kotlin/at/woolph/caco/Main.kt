package at.woolph.caco

import at.charlemagne.libs.log.logger
import com.opencsv.CSVReaderHeaderAware
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.DateTimeFormat
import kotlin.system.exitProcess
import java.net.MalformedURLException
import jdk.nashorn.internal.runtime.ScriptingFunctions.readLine
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonReader
import java.io.FileReader
import com.opencsv.CSVReader
import kotlin.math.max


private val logger by logger("main")

public inline fun <R> InputStream.useJsonReader(block: (JsonReader) -> R): R {
	this.use {
		return javax.json.Json.createReader(it).use(block)
	}
}

public fun JsonObject.getJsonObjectArray(name: String) = this.getJsonArray(name).getValuesAs(JsonObject::class.java)

const val IMPORT_SET = "--importBase="
const val IMPORT_INVENTORY = "--importInventory="
const val SHOW_NEEDED_PLAYSET = "--showNeededCollection"
const val SHOW_NEEDED_FOIL = "--showNeededCollectionFoil"
const val SHOW_NEEDED_DECKLIST = "--showNeededDeck"

fun importSet(setCode: String): Set {
	println("importing set $setCode")
	Thread.sleep(2000) // delay queries to scryfall api (to prevent overloading service)
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
					return Set.new {
						shortName = it.getString("code")
						name = it.getString("name")
						dateOfRelease = DateTime.parse(it.getString("released_at"))
						officalCardCount = it.getInt("card_count")
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

fun Set.importCardsOfSet() {
	var nextURL: URL? = URL("https://api.scryfall.com/cards/search?q=set%3A${this.shortName}&unique=prints&order=set")

	while(nextURL!=null) {
		println("requesting $nextURL")
		Thread.sleep(2000) // delay queries to scryfall api (to prevent overloading service)
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
					if(it.getString("object") == "card") {
						Card.new {
							set = this@importCardsOfSet
							numberInSet = it.getString("collector_number").toInt()
							name = it.getString("name")
							rarity = it.getString("rarity").parseRarity()
							promo = it.getBoolean("promo")
							image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) } ?:
									it.getJsonObjectArray("card_faces")?.get(0)?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
							cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }
						}
					}
				}
			}
		}
		conn.disconnect()
	}
}

/**
 * @see https://github.com/JetBrains/Exposed
 */
fun main(args: Array<String>) {
	val dtf = DateTimeFormat.forPattern("YYYY-MM-dd")
	Database.connect("jdbc:h2:~/colly", driver = "org.h2.Driver")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(Sets, Cards, CardPossessions, Decks, DeckCards)
	}

	// @TODO wish list sorting (mark specific cards needed for decks or just for collection) => sort by price + modifier based on decklist needs
	// @TODO decklist => determine wishlist sorting (decklist priority)

	if(args.any { it.startsWith(IMPORT_SET) }) {
		// import card database
		transaction {
			Cards.deleteAll()
			Sets.deleteAll()

			val setCodes = args.filter { it.startsWith(IMPORT_SET) }
					.flatMap { it.removePrefix(IMPORT_SET).split(",") }

			setCodes.forEach { setCode ->
				try {
					importSet(setCode).importCardsOfSet()
				} catch(ex:Exception) {
					ex.printStackTrace()
				}
			}
		}
	}

	// import inventory
	if(args.any { it.startsWith(IMPORT_INVENTORY) }) {
		val file = args.first { it.startsWith(IMPORT_INVENTORY) }.removePrefix(IMPORT_INVENTORY)

		transaction {
			CardPossessions.deleteAll()

			val reader = CSVReader(FileReader(file))
			if (reader.readNext() != null) { // skip header
				var nextLine: Array<String>? = reader.readNext()
				while (nextLine != null) {
					try {
						val count = nextLine[0].toInt()
						val cardName = nextLine[2]
						val cardNumber = nextLine[4].toInt()
						val setName = nextLine[3].removePrefix("Prerelease Events: ")
						val prereleasePromo = nextLine[3].startsWith("Prerelease Events: ")
						val set = Set.find { Sets.name eq setName }.firstOrNull()
								?: throw Exception("set $setName not found")
						val card = Card.find { (Cards.numberInSet eq cardNumber) and (Cards.set eq set.id) }.firstOrNull()
								?: throw Exception("card #$cardNumber (\"$cardName\") not found in set $setName")
						val foil = nextLine[7] == "foil"
						val condition = when (nextLine[5]) {
							"Mint", "Near Mint" -> 1
							"Good (Lightly Played)" -> 2
							"Played" -> 3
							"Heavily Played" -> 4
							"Poor" -> 5
							else -> 0
						}
						val language = when (nextLine[6]) {
							"English" -> "en"
							"German" -> "de"
							else -> "??"
						}

						repeat(count) {
							CardPossession.new {
								this.card = card
								this.language = language
								this.condition = condition
								this.prereleasePromo = prereleasePromo
								this.foil = foil
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

	// get needed cards for playset collection
	if(args.any { it.startsWith(SHOW_NEEDED_PLAYSET) }) {
		transaction {
			Set.all().forEach { set ->
				println("${set.name}: needed cards --------------------------------------")
				set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.forEach {
					val neededCount = max(0, 4 - it.possessions.count())
					val n = if(set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
					if (neededCount > 0) {
						println("${neededCount}\t${it.name}$n\t[${it.rarity}]")
					}
				}
			}
		}
	}

	// get needed cards for foil one of collection
	if(args.any { it.startsWith(SHOW_NEEDED_FOIL) }) {
		transaction {
			Set.all().forEach { set ->
				println("${set.name}: needed cards --------------------------------------")
				set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.forEach {
					val neededCount = max(0, 1 - it.possessions.filter { it.foil }.count())
					if (neededCount > 0) {
						println("${neededCount}\t${it.name}\t[${it.rarity}]")
					}
				}
			}
		}
	}

	// get needed cards for decklists
	if(args.any { it.startsWith(SHOW_NEEDED_DECKLIST) }) {
		transaction {
			val deckNeeds = DeckCards.slice(DeckCards.count.sum(), DeckCards.name).selectAll().groupBy(DeckCards.deck, DeckCards.name)
					.groupingBy { it[DeckCards.name] }.aggregate<ResultRow, String, Long> { _, accumulator: Long?, element, first ->
						if(first) element.data[0] as Long else max(accumulator!!, element.data[0]!! as Long)
					}

			println("needed cards --------------------------------------")
			deckNeeds.forEach { (cardName, neededCount) ->

				val available = (Cards innerJoin CardPossessions).slice(CardPossessions.card.count())
						.select { Cards.name eq cardName }.groupBy(Cards.name)
						.map { it.data[0] as Long }.first()

				if(neededCount-available>0) {
					println("${neededCount - available}\t${cardName}")
				}
			}
		}
	}
}
