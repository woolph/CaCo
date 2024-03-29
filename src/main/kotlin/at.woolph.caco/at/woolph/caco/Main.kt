package at.woolph.caco

import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.datamodel.sets.Cards.set
import at.woolph.caco.datamodel.sets.Rarity
import at.woolph.caco.importer.collection.importDeckbox
import at.woolph.caco.importer.sets.*
import at.woolph.libs.log.logger
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.joda.time.DateTime
import java.awt.Color
import java.nio.file.Paths
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

private val LOG by logger("at.woolph.caco.Main")

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

/**
 *
 */
fun main(args: Array<String>) {
	Database.connect("jdbc:h2:~/caco", driver = "org.h2.Driver")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(CardSets, Cards, CardPossessions, DeckArchetypes, Builds, DeckCards, ArenaCardPossessions)
	}

	if (args.any { it.startsWith(IMPORT_SETS) }) {
		LOG.info("importing all sets and cards took {} ms", measureTimeMillis {
			transaction {
					importSets()
//						.filter { it.dateOfRelease.isAfter(DateTime.parse("2020-01-01")) }
						.forEach {
						LOG.info("importing cards for set {} cards took {} ms", it.shortName, measureTimeMillis {
						LOG.debug("${it.shortName} ${it.otherScryfallSetCodes.joinToString(",")}")
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
		transaction {
			val setCodes = args.filter { it.startsWith(IMPORT_SET) }
					.flatMap { it.removePrefix(IMPORT_SET).split(",") }

			setCodes.forEach { setCode ->
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
	if(args.any { it.startsWith(SHOW_NEEDED_PLAYSET) }) {
		transaction {
			val setCodes = args.filter { it.startsWith(SHOW_NEEDED_PLAYSET) }
					.flatMap { it.removePrefix(SHOW_NEEDED_PLAYSET).split(",") }

			setCodes.forEach { setCode ->
				CardSet.find { CardSets.shortName eq setCode.toLowerCase() }.forEach { set ->
					println("${set.name}:")
					set.cards.sortedBy { it.numberInSet }.filter { !it.promo && (it.rarity == Rarity.COMMON || it.rarity == Rarity.UNCOMMON) }.forEach {
						val neededCount = max(0, 4 - it.possessions.count())
						val n = if (set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
						if (neededCount > 0) {
							println("${neededCount} ${it.name}$n")
						}
					}
				}
			}
		}
	} else {
		// get needed cards for playset collection
		if(args.any { it.startsWith(SHOW_NEEDED_PLAYSET_ALL) }) {
			transaction {
				CardSet.all().forEach { set ->
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
	}

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
				CardSet.find { CardSets.shortName eq setCode.toLowerCase() }.forEach { set ->
					println("${set.name}: inventory --------------------------------------")
					set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.forEach {
						val ownedCount = min(4, it.possessions.count())
						val n = if (set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""

						val plus = if(it.possessions.count()>4) ICON_REDUNDANT_OWNED_CARD else ""
						println("${ICON_OWNED_CARD.repeat(ownedCount)}${ICON_NOT_OWNED_CARD.repeat(4-ownedCount)}$plus\t${it.name}$n")
					}
				}
			}
		}
	}

	val fontTitle = Font(PDType1Font.HELVETICA_BOLD, 10f)

	if (args.any { it.startsWith(PRINT_INVENTORY_PDF) }) {
		// import card database
		transaction {
			val setCodes = args.filter { it.startsWith(PRINT_INVENTORY_PDF) }
					.flatMap { it.removePrefix(PRINT_INVENTORY_PDF).split(",") }

			createPdfDocument(Paths.get("D:\\woolph\\Dropbox\\mtg-inventory.pdf")) {
				setCodes.forEach { setCode ->
					CardSet.find { CardSets.shortName eq setCode.toLowerCase() }.forEach { set ->
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
