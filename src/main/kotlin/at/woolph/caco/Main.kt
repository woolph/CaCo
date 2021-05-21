package at.woolph.caco

import at.woolph.caco.datamodel.collection.*
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.decks.DeckVariants
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.Cards.set
import at.woolph.caco.importer.collection.importDeckbox
import at.woolph.caco.importer.collection.toLanguageDeckbox
import at.woolph.caco.importer.sets.importCardsOfSet
import at.woolph.caco.importer.sets.importPromosOfSet
import at.woolph.caco.importer.sets.importSet
import at.woolph.caco.importer.sets.importTokensOfSet
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.awt.Color
import java.nio.file.Paths
import kotlin.math.max
import kotlin.math.min

const val IMPORT_SET = "--importSet="
const val IMPORT_INVENTORY = "--importInventory="
const val SHOW_NEEDED_PLAYSET_ALL = "--showNeededCollection"
const val SHOW_NEEDED_PLAYSET = "--showNeededCollection="
const val SHOW_NEEDED_FOIL = "--showNeededCollectionFoil"
const val SHOW_NEEDED_DECKLIST = "--showNeededDeck"
const val PRINT_NEEDED_DECKLIST = "--printNeededDeck"

const val REGISTER_CARDS_SET = "--enterSet"
const val LANGUAGE = "--language="
const val CONIDTION = "--condition="
const val MYSTERY = "--pwstamp"

const val PRINT_INVENTORY = "--printInventory="
const val PRINT_INVENTORY_PDF = "--printInventoryToPdf="

const val ICON_OWNED_FOIL = "\u25C9"
const val ICON_NOT_OWNED_FOIL = "\u25CE"
const val ICON_OWNED_CARD = "\u25a0" // "\u25CF"
const val ICON_NOT_OWNED_CARD = "\u25a1" // "\u25CB"
const val ICON_REDUNDANT_OWNED_CARD = "+"

/**
 * @see https://github.com/JetBrains/Exposed
 */
fun main(args: Array<String>) {
	Database.connect("jdbc:h2:~/caco", driver = "org.h2.Driver")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(CardSets, Cards, CardPossessions, DeckArchetypes, DeckVariants, Builds, DeckCards, ArenaCardPossessions)
	}

	// @TODO wish list sorting (mark specific cards needed for decks or just for collection) => sort by price + modifier based on decklist needs
	// @TODO decklist => determine wishlist sorting (decklist priority)

	val preferredLanguages = arrayOf(CardLanguage.ENGLISH, CardLanguage.GERMAN) // TODO configurable in collection settings of set?

	if(args.any { it.startsWith(REGISTER_CARDS_SET) }) {
		// import card database
		val condition = args.singleOrNull { it.startsWith(CONIDTION) }?.let { CardCondition.valueOf(it.removePrefix(CONIDTION)) } ?: CardCondition.NEAR_MINT
		val language = args.singleOrNull { it.startsWith(LANGUAGE) }?.let { CardLanguage.valueOf(it.removePrefix(LANGUAGE)) } ?: CardLanguage.ENGLISH
		val pwstamp = args.any { it.startsWith(MYSTERY) }

		val preferredLanguageIndex = preferredLanguages.indexOf(language)
		val checkLanguages = (if(preferredLanguageIndex >= 0) preferredLanguages.copyOfRange(0, preferredLanguageIndex+1) else preferredLanguages).asList()

		transaction {
			File("D:\\mystery.csv").printWriter().use { out ->
				out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")
				println("enter set code (or blank to stop)")
				var setCode = readLine()

				while(!setCode.isNullOrBlank()) {
					val cardsToBeAdded = mutableMapOf<Triple<Card, Boolean, Boolean>, Int>()
					try {
						val cardSet = CardSet.find { CardSets.shortName.eq(setCode!!) }.singleOrNull() ?: importSet(setCode!!).apply {
							importCardsOfSet()
							importTokensOfSet()
							importPromosOfSet()
						}
						println("Add to set $setCode $language $condition")

						var card: Triple<Card, Boolean, Boolean>? = null
						var input = readLine()
						while(!input.isNullOrBlank()) {
							if(input == "-") {
								card?.let{
									println("Removing ${it.first.name}")
									cardsToBeAdded[it] = cardsToBeAdded.getOrDefault(it, 1) - 1
								}
							}
							else if(input == "+") {
								card?.let{
									println("Add another ${it.first.name}"+if(it.second) "*" else "")
									cardsToBeAdded[it] = cardsToBeAdded.getOrDefault(it, 0) + 1
								}
							}
							else {
								val collectorNumber = input.removeSuffix("*").removeSuffix("/")
								val prereleaseStamp = input.endsWith("/")
								val foil = input.endsWith("*") || prereleaseStamp

								card = cardSet.cards.singleOrNull { it.numberInSet == collectorNumber }?.let { Triple(it, foil, prereleaseStamp) }

								if(card != null) {
									val addedTillNow = cardsToBeAdded.getOrDefault(card, 0)
									if (addedTillNow + CardPossession.find { CardPossessions.card.eq(card.first.id).and(CardPossessions.foil.eq(card.second)).and(CardPossessions.language.inList(checkLanguages))
													.let { if(!pwstamp) it.and(CardPossessions.markPlaneswalkerSymbol.eq(false)) else it } // check for preferred cards without pw stamp
											}.count() == 0)
										println("[NEW] Adding ${card.first.name} "+if(card.second) "*" else "")
									else
										println("Adding ${card.first.name}"+if(card.second) "*" else "")

									cardsToBeAdded[card] = addedTillNow + 1
								}
								else {
									println("$input not found")
								}
							}
							input = readLine()
						}

						cardsToBeAdded.forEach { card, count ->
							if(count > 0) {
								val cardName = card.first.name
								val cardNumberInSet = card.first.numberInSet
								val token = card.first.token
								val promo = card.first.promo
								val condition2 = when (condition) {
									CardCondition.NEAR_MINT -> "Near Mint"
									CardCondition.EXCELLENT -> "Good (Lightly Played)"
									CardCondition.GOOD -> "Played"
									CardCondition.PLAYED -> "Heavily Played"
									CardCondition.POOR -> "Poor"
									else -> throw Exception("unknown condition")
								}
								val prereleasePromo = card.third
								val language2 = language.toLanguageDeckbox()
								val setName = when {
									prereleasePromo -> "Prerelease Events: ${cardSet.name}"
									token -> "Extras: ${cardSet.name}"
									else -> cardSet.name
								}

								val foilString = if(card.second) "foil" else ""
								val proofString = if(pwstamp) "proof" else ""
								// TODO set names from scryfall to deckbox
//								Global Series Jiang Yanggu & Mu Yanling	Global Series: Jiang Yanggu and Mu Yanling
//								Modern Masters 2015	Modern Masters 2015 Edition
//								Modern Masters 2017	Modern Masters 2017 Edition
//								GRN Guild Kit	Guilds of Ravnica Guild Kit
//								RNA Guild Kit	Ravnica Allegiance Guild Kit
//								Commander 2011	Commander
//								Magic 2014	Magic 2014 Core Set
//								Magic 2015	Magic 2015 Core Set
//								Duel Decks Anthology: Jace vs. Chandra	Duel Decks Anthology, Jace vs. Chandra
//								alle Duel Decks Anthologies
								out.println("$count,0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition2,$language2,$foilString,,$proofString,,,,,")

								repeat(count) {
									CardPossession.new {
										this.card = card.first
										this.language = language
										this.condition = condition
										this.foil = card.second
										this.stampPrereleaseDate = card.third
										this.markPlaneswalkerSymbol = pwstamp
									}
								}
							}
						}
					} catch(ex:Exception) {
						ex.printStackTrace()
					}

					println("enter set code (or blank to stop)")
					setCode = readLine()
				}
			}
		}
	}

	if(args.any { it.startsWith(IMPORT_SET) }) {
		// import card database
		transaction {
			val setCodes = args.filter { it.startsWith(IMPORT_SET) }
					.flatMap { it.removePrefix(IMPORT_SET).split(",") }

			setCodes.forEach { setCode ->
				try {
					importSet(setCode.toLowerCase()).apply {
						importCardsOfSet()
						importTokensOfSet()
						importPromosOfSet()
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
						?.maxBy { it.lastModified() }
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
							println("${neededCount} ${it.name}")
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
	if(args.any { it.startsWith(SHOW_NEEDED_DECKLIST) }) {
		transaction {
			val deckNeeds = DeckCards.slice(DeckCards.count.sum(), DeckCards.name).selectAll().groupBy(DeckCards.build, DeckCards.name)
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

	if(args.any { it.startsWith(PRINT_INVENTORY) }) {
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

	if(args.any { it.startsWith(PRINT_INVENTORY_PDF) }) {
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
	if(args.any { it.startsWith(PRINT_NEEDED_DECKLIST) }) {
		transaction {
			val deckNeeds = DeckCards.slice(DeckCards.count.sum(), DeckCards.name).selectAll().groupBy(DeckCards.build, DeckCards.name)
					.groupingBy { it[DeckCards.name] }.aggregate<ResultRow, String, Long> { _, accumulator: Long?, element, first ->
						if(first) element.data[0] as Long else max(accumulator!!, element.data[0]!! as Long)
					}.mapNotNull { (cardName, neededCount) ->
						val available = (Cards innerJoin CardPossessions).slice(CardPossessions.card.count())
								.select { Cards.name eq cardName }.groupBy(Cards.name)
								.map { it.data[0] as Long }.first()

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
