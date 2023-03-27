package at.woolph.caco

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Cards.set
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.datamodel.sets.parseRarity
import at.woolph.caco.importer.collection.importDeckbox
import at.woolph.caco.importer.sets.containsString
import at.woolph.caco.importer.sets.importCardsOfSet
import at.woolph.caco.importer.sets.importSet
import at.woolph.caco.importer.sets.importSets
import at.woolph.caco.importer.sets.paddingCollectorNumber
import at.woolph.caco.importer.sets.patternPromoCollectorNumber
import at.woolph.caco.view.collection.CardPossessionModel
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.libs.files.bufferedWriter
import at.woolph.libs.files.inputStream
import at.woolph.libs.files.path
import at.woolph.libs.json.getJsonObjectArray
import at.woolph.libs.json.useJsonReader
import at.woolph.libs.log.logger
import at.woolph.libs.pdf.Font
import at.woolph.libs.pdf.PagePosition
import at.woolph.libs.pdf.columns
import at.woolph.libs.pdf.createPdfDocument
import at.woolph.libs.pdf.drawText
import at.woolph.libs.pdf.frame
import at.woolph.libs.pdf.page
import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.*
import javax.json.JsonNumber
import javax.json.JsonValue
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

private val LOG by logger("at.woolph.caco.Main")

const val EXPORT_VALUE_TRADEABLES = "--exportValueTradeables"
const val IMPORT_FROM_FILE = "--importFile="
const val IMPORT_PRICES = "--importPrices="
const val IMPORT_SETS = "--importSets"
const val IMPORT_SET = "--importSet="
const val IMPORT_INVENTORY = "--importInventory="
const val SHOW_NEEDED_PLAYSET_ALL = "--showNeededCollection"
const val SHOW_NEEDED_PLAYSET = "--showNeededCollection="
const val SHOW_NEEDED_FOIL = "--showNeededCollectionFoil"
const val SHOW_NEEDED_DECKLIST = "--showNeededDeck"
const val PRINT_NEEDED_DECKLIST = "--printNeededDeck"

const val PRINT_INVENTORY = "--printInventory="
const val PRINT_INVENTORY_PDF = "--printInventoryToPdf="

const val ICON_OWNED_FOIL = "\u25C9"
const val ICON_NOT_OWNED_FOIL = "\u25CE"
const val ICON_OWNED_CARD = "\u25a0" // "\u25CF"
const val ICON_NOT_OWNED_CARD = "\u25a1" // "\u25CB"
const val ICON_REDUNDANT_OWNED_CARD = "+"

fun <ID : Comparable<ID>, T: Entity<ID>> EntityClass<ID, T>.newOrUpdate(id: ID, setter: T.() -> Unit): T {
	return findById(id)?.apply(setter) ?: new(id, setter)
}

data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

/**
 *
 */
suspend fun main(args: Array<String>) {
	Databases.init()

	if (args.any { it.startsWith(EXPORT_VALUE_TRADEABLES) }) {
		path("./export-value-tradeables.txt").bufferedWriter().use { writer ->
			transaction {
				CardSet.all().map { set ->
					LOG.info("starting for set ${set.name}")
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
					}.filter { it.t1 > 0 && it.t4 >= 1.0 }.joinToString("\n") { (excess, cardName, setName, price) ->
						"$excess $cardName ($setName) $price"
					}
				}.forEach { (set, cards) ->
					LOG.info("set {$set.name} done ${cards.isNotBlank()}")
					if (cards.isNotBlank()) {
						writer.write("------------------------\n${set.name}\n$cards")
						writer.newLine()
					}
				}
			}
		}
	}

	if (args.any { it.startsWith(IMPORT_FROM_FILE) }) {
		args.filter { it.startsWith(IMPORT_FROM_FILE) }.map { it.removePrefix(IMPORT_FROM_FILE) }.singleOrNull()?.let { importFile ->
			transaction {
				importSets()
				path(importFile).inputStream().useJsonReader { rootObject ->
					rootObject.readArray().map(JsonValue::asJsonObject).forEach {
						if (it.getString("object") == "card") {
							ScryfallCardSet.findById(UUID.fromString(it.getString("set_id")))?.let { cardSet ->
								try {
									val cardId = UUID.fromString(it.getString("id"))
									val cardName = it.getString("name")
									val isPromo = it.getBoolean("promo") || it.getString("collector_number")
										.contains(patternPromoCollectorNumber)
									val isToken = it.getString("set_type") == "token"
									val isMemorabilia =
										it.getString("set_type") == "memorabilia" // art series, commander special cards (like OC21)
									val numberInSetImported = paddingCollectorNumber(
										when {
											isMemorabilia -> "M" + it.getString("collector_number")
											isPromo && cardSet.set.shortName.value != it.getString("set") -> it.getString(
												"collector_number"
											) + " P"
											isToken -> "T" + it.getString("collector_number")
											else -> it.getString("collector_number")
										}
									)
									val isExtra = !it.getBoolean("booster")
									val isNonfoilAvailable = it.getBoolean("nonfoil")
									val isFoilAvailable = it.getBoolean("foil")
									val isFullArt = it.getBoolean("full_art")
									val isExtendedArt =
										it.getJsonArray("frame_effects")?.containsString("extendedart") ?: false

									val patternSpecialDeckRestrictions =
										Regex("A deck can have (any number of cards|only one card|up to (\\w+) cards) named $cardName\\.")
									val specialDeckRestrictions = it.getString("oracle_text", null)?.let { oracleText ->
										patternSpecialDeckRestrictions.find(oracleText)?.let {
											if (it.groupValues[1] == "any number of cards")
												Int.MAX_VALUE // TODO
											else if (it.groupValues[1] == "only one card")
												1
											else when (it.groupValues[2]) {
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

									val isStampedPromoPackCard =
										it.containsKey("promo_types") && it.getJsonArray("promo_types")
											.containsString("promopack") && it.getJsonArray("promo_types")
											.containsString("stamped")
									val isPrereleaseStamped =
										it.containsKey("promo_types") && it.getJsonArray("promo_types")
											.containsString("prerelease") && it.getJsonArray("promo_types")
											.containsString("datestamped")

									if (!isPrereleaseStamped && !isStampedPromoPackCard) {
										Card.newOrUpdate(cardId) {
											set = cardSet
											numberInSet = numberInSetImported
											name = cardName
											arenaId = it["arena_id"]?.let { (it as? JsonNumber)?.intValue() }
											rarity = it.getString("rarity").parseRarity()
											promo = isPromo
											token = isToken
											image = it.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
												?: it.getJsonObjectArray("card_faces")?.get(0)
													?.getJsonObject("image_uris")?.getString("png")?.let { URI(it) }
											cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")
												?.let { URI(it) }

											manaCost = it.getString("mana_cost", null)
											type = it.getString("type_line", null)
											extra = isExtra
											nonfoilAvailable = isNonfoilAvailable
											foilAvailable = isFoilAvailable
											fullArt = isFullArt
											extendedArt = isExtendedArt

											val prices = it.getJsonObject("prices")

											price =
												if (prices["eur"]?.valueType == JsonValue.ValueType.STRING) prices.getJsonString(
													"eur"
												).string.toDouble() else null
											priceFoil =
												if (prices["eur_foil"]?.valueType == JsonValue.ValueType.STRING) prices.getJsonString(
													"eur_foil"
												).string.toDouble() else null

											this.specialDeckRestrictions = specialDeckRestrictions
										}
									}
								} catch (e: Exception) {
									LOG.warn("skip card $it due to exception", e)
								}
							}
						}
					}
				}
			}
		}
	}

	if (args.any { it.startsWith(IMPORT_PRICES) }) {
		args.filter { it.startsWith(IMPORT_PRICES) }.map { it.removePrefix(IMPORT_PRICES) }.singleOrNull()?.let { importFile ->
			path(importFile).inputStream().useJsonReader { rootObject ->
				rootObject.readArray().map(JsonValue::asJsonObject).forEach {
					if(it.getString("object") == "card") {
						Card.findById(UUID.fromString(it.getString("id")))?.apply {
							cardmarketUri = it.getJsonObject("purchase_uris")?.getString("cardmarket")?.let { URI(it) }

							price = it.getJsonObject("prices").getString("eur").toDouble()
							priceFoil = it.getJsonObject("prices").getString("eur_foil").toDouble()
						}
					}
				}
			}
		}
	}

	if (args.any { it.startsWith(IMPORT_SETS) }) {
		LOG.info("importing all sets and cards took {} ms", measureTimeMillis {
			newSuspendedTransaction {
					importSets()
//						.filter { it.dateOfRelease.isAfter(DateTime.parse("2020-01-01")) }
						.collect {
						LOG.info("importing cards for set {} cards took {} ms", it.shortName, measureTimeMillis {
						it.importCardsOfSet()
						})
					}
			}
		})
	}
	// @TODO wish list sorting (mark specific cards needed for decks or just for collection) => sort by price + modifier based on decklist needs
	// @TODO decklist => determine wishlist sorting (decklist priority)

	if(args.any { it.startsWith(IMPORT_SET) }) {
		// import card database
		newSuspendedTransaction {
			args.filter { it.startsWith(IMPORT_SET) }
				.flatMap { it.removePrefix(IMPORT_SET).split(",") }
				.forEach { setCode ->
					try {
						importSet(setCode.lowercase(Locale.getDefault())).apply {
							importCardsOfSet()
						}
					} catch(ex:Exception) {
						ex.printStackTrace()
					}
				}
		}
	}

	// import inventory
	if(args.any { it.startsWith(IMPORT_INVENTORY) }) {
		File(args.first { it.startsWith(IMPORT_INVENTORY) }.removePrefix(IMPORT_INVENTORY)).let {
			if(it.isDirectory) {
				it.listFiles()?.asList()?.onEach { println("content $it") }
						?.filter { it.name.toString().let { it.startsWith("Inventory") && it.endsWith(".csv")} }
						?.onEach {println("passed filter $it") }
						?.maxByOrNull { it.lastModified() }
			} else {
				it
			}
		}?.let { importDeckbox(it) }
	}

	// get needed cards for playset collection
//	if(args.any { it.startsWith(SHOW_NEEDED_PLAYSET) }) {
//		args.filter { it.startsWith(SHOW_NEEDED_PLAYSET) }.map { it.removePrefix(SHOW_NEEDED_PLAYSET) }.singleOrNull()?.let { setCode ->
//			transaction {
//				CardSet.findById(setCode.lowercase(Locale.getDefault()))?.let { set ->
//					println("${set.name}:")
//					val cardsSorted = set.cards.sortedBy { it.numberInSet }.map { CardPossessionModel(it, PaperCollectionView.COLLECTION_SETTINGS) }
//					cardsSorted.asSequence().map { card ->
//						val neededCount = max(0, card.possessionNonPremiumTarget.value - card.possessionNonPremium.value)
//						val suffixName = if (cardsSorted.asSequence().filter { it2 ->
//								it2.name.value == card.name.value  && it2.extra.value == card.extra.value
//							}.count() > 1) {
//							val numberInSetWithSameName = cardsSorted.asSequence().filter { it2 ->
//								it2.name.value == card.name.value && it2.extra.value == card.extra.value && it2.numberInSet.value < card.numberInSet.value
//							}.count() + 1
//							" (V.$numberInSetWithSameName)"
//						} else {
//							""
//						}
//						val suffixSet = when {
//							card.promo.value -> ": Promos"
//							card.extra.value -> ": Extras"
//							else -> ""
//						}
//						Triple(neededCount, "${card.name.value}$suffixName", card.set.value?.set?.name?.let { "$it$suffixSet" })
//					}.filter { it.first > 0 }.joinToString("\n") {
//						"${it.first} ${it.second} (${it.third})"
//					}
//				}
//			}
//		}?.let { string ->
//			println(string)
//			Toolkit.getDefaultToolkit()
//				.systemClipboard
//				.setContents(StringSelection(string), null)
//		}
//
//
////		Toolkit.getDefaultToolkit()
////			.systemClipboard
////			.setContents(
////				StringSelection( ?: ""),
////				null)
////		transaction {
////			val setCodes = args.filter { it.startsWith(SHOW_NEEDED_PLAYSET) }
////					.flatMap { it.removePrefix(SHOW_NEEDED_PLAYSET).split(",") }
////
////			setCodes.forEach { setCode ->
////				CardSet.find { CardSets.shortName eq setCode.toLowerCase() }.forEach { set ->
////					println("${set.name}:")
////					set.cards.sortedBy { it.numberInSet }.filter { !it.promo && (it.rarity == Rarity.COMMON || it.rarity == Rarity.UNCOMMON) }.forEach {
////						val neededCount = max(0, 4 - it.possessions.count())
////						val n = if (set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
////						if (neededCount > 0) {
////							println("${neededCount} ${it.name}$n")
////						}
////					}
////				}
////			}
////		}
//	} else {
//		// get needed cards for playset collection
//		if(args.any { it.startsWith(SHOW_NEEDED_PLAYSET_ALL) }) {
//			transaction {
//				CardSet.all().forEach { set ->
//					println("${set.name}: needed cards --------------------------------------")
//					set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.forEach {
//						val neededCount = max(0, 4 - it.possessions.count())
//						val n = if(set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
//						if (neededCount > 0) {
//							println("${neededCount}\t${it.name}$n\t[${it.rarity}]")
//						}
//					}
//				}
//			}
//		}
//	}

	// get needed cards for foil one of collection
	if(args.any { it.startsWith(SHOW_NEEDED_FOIL) }) {
		transaction {
			CardSet.all().forEach { set ->
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
	if (args.any { it.startsWith(SHOW_NEEDED_DECKLIST) }) {
		transaction {
			val expressionSumOfCardsInDeckNeeded = DeckCards.count.sum()
			val deckNeeds = DeckCards.slice(expressionSumOfCardsInDeckNeeded, DeckCards.name).selectAll().groupBy(DeckCards.build, DeckCards.name)
					.groupingBy { it[DeckCards.name] }.aggregate<ResultRow, String, Int> { _, accumulator: Int?, element, first ->
						if(first) element[expressionSumOfCardsInDeckNeeded]!! else max(accumulator!!, element[expressionSumOfCardsInDeckNeeded]!!)
					}

			println("needed cards --------------------------------------")
			deckNeeds.forEach { (cardName, neededCount) ->

				val cardPossessionsCount = CardPossessions.card.count()
				val available = (Cards innerJoin CardPossessions).slice(cardPossessionsCount)
						.select { Cards.name eq cardName }.groupBy(Cards.name)
						.map { it[cardPossessionsCount] }.first()

				if(neededCount-available>0) {
					println("${neededCount - available}\t${cardName}")
				}
			}
		}
	}

	if (args.any { it.startsWith(PRINT_INVENTORY) }) {
		// import card database
		transaction {
			val setCodes = args.filter { it.startsWith(PRINT_INVENTORY) }
					.flatMap { it.removePrefix(PRINT_INVENTORY).split(",") }

			setCodes.forEach { setCode ->
				CardSet.findById(setCode.toLowerCase())?.let { set ->
					println("${set.name}: inventory --------------------------------------")
					set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.forEach {
						val ownedCount = min(4, it.possessions.count()).toInt()
						val n = if (set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""

						val plus = if(it.possessions.count()>4) ICON_REDUNDANT_OWNED_CARD else ""
						println("${ICON_OWNED_CARD.repeat(ownedCount)}${ICON_NOT_OWNED_CARD.repeat(4-ownedCount)}$plus\t${it.name}$n")
					}
				}
			}
		}
	}


	if (args.any { it.startsWith(PRINT_INVENTORY_PDF) }) {
		// import card database
		transaction {
			val setCodes = args.filter { it.startsWith(PRINT_INVENTORY_PDF) }
					.flatMap { it.removePrefix(PRINT_INVENTORY_PDF).split(",") }

			val fontTitle = Font(PDType1Font.HELVETICA_BOLD, 10f)

			createPdfDocument(Paths.get("D:\\woolph\\Dropbox\\mtg-inventory.pdf")) {
				setCodes.forEach { setCode ->
					CardSet.findById(setCode.toLowerCase())?.let { set ->
						page(PDRectangle.A4) {
							frame(PagePosition.RIGHT, 50f, 20f, 20f, 20f) {
								drawText("Inventory ${set.name}", fontTitle, HorizontalAlignment.CENTER, box.upperRightY - 10f, Color.BLACK)

								// TODO calc metrics for all sets (so that formatting is the same for all pages)
								set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.let {
									frame(marginTop = fontTitle.height + 20f) {
										columns((it.size - 1) / 100 + 1, 100, 5f, 3.5f, Font(PDType1Font.HELVETICA, 6.0f)) {
											var i = 0
											it.filter { !it.token }.forEach {
												val ownedCountEN = it.possessions.filter { it.language == CardLanguage.ENGLISH }.count()
												val ownedCountDE = it.possessions.filter { it.language == CardLanguage.GERMAN }.count()
												this@columns.get(i) {
													drawTextWithRects("${it.rarity} ${it.numberInSet} ${it.name}", ownedCountEN, ownedCountDE)
												}
												i++
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

	// get needed cards for decklists
	if (args.any { it.startsWith(PRINT_NEEDED_DECKLIST) }) {
		transaction {
			val fontTitle = Font(PDType1Font.HELVETICA_BOLD, 10f)
			val expressionSumOfCardsInDeckNeeded = DeckCards.count.sum()
			val deckNeeds = DeckCards.slice(DeckCards.count.sum(), DeckCards.name).selectAll().groupBy(DeckCards.build, DeckCards.name)
					.groupingBy { it[DeckCards.name] }.aggregate<ResultRow, String, Int> { _, accumulator: Int?, element, first ->
						if(first) element[expressionSumOfCardsInDeckNeeded]!! else max(accumulator!!, element[expressionSumOfCardsInDeckNeeded]!!)
					}.mapNotNull { (cardName, neededCount) ->
						val cardPossessionsCount = CardPossessions.card.count()
						val available = (Cards innerJoin CardPossessions).slice(cardPossessionsCount)
								.select { Cards.name eq cardName }.groupBy(Cards.name)
								.map { it[cardPossessionsCount] }.first()

						if(neededCount-available>0) {
							Pair(cardName, neededCount - available)
						} else {
							null
						}

						// todo print archetype names for which it is wanted
						// TODO print desired sets if there's also the possibility to complete collection while building the archetype
					}


			createPdfDocument(Paths.get("d:\\wants.pdf")) {
				page(PDRectangle.A4) {
					frame(PagePosition.RIGHT, 50f, 20f, 20f, 20f) {

						drawText("Needs ${set.name}", fontTitle, HorizontalAlignment.CENTER, box.upperRightY-10f, Color.BLACK)

						// TODO calc metrics for all sets (so that formatting is the same for all pages)
						val baseTable = BaseTable(642f, 842f, 0f, box.width, 0f, this@createPdfDocument, this@page.pdPage, true, true)
						listOf("Dive Down" to 3, "Surge Mare" to 1).forEach {
							val row = baseTable.createRow(15f).apply {
								createCell(15f, it.second.toString(), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
								createCell(30f, it.first, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
								createCell(15f,"$2.00", HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
								createCell(15f,"[CMD] Brudiclad", HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
								createCell(15f,"1xDOM, 2xIXL", HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
							}
							//(0 until 3).forEach { val cell = row.createCell(15f, "Row $i Col ${it + 1}", HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE) }
						}
					}
				}
			}
		}
	}

	// TODO list of spare cards
	// TODO list of needs collection
	// TODO list of needs for decks
	// TODO list of high value cards
}
