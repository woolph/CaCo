package at.woolph.caco

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Cards.set
import at.woolph.caco.importer.collection.importDeckbox
import at.woolph.caco.importer.collection.setNameMapping
import at.woolph.caco.importer.collection.toDeckboxCondition
import at.woolph.caco.importer.collection.toLanguageDeckbox
import at.woolph.caco.importer.sets.*
import at.woolph.caco.view.collection.CardPossessionModel
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.libs.files.bufferedWriter
import at.woolph.libs.files.inputStream
import at.woolph.libs.files.path
import at.woolph.libs.log.logger
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.decodeToSequence
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDSimpleFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

private val LOG by logger("at.woolph.caco.Main")

const val EXPORT_VALUE_TRADEABLES = "--exportValueTradeables"
const val ENTER_CARDS = "--enterCardsOfSet="
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

	// @TODO wish list sorting (mark specific cards needed for decks or just for collection) => sort by price + modifier based on decklist needs
	// @TODO decklist => determine wishlist sorting (decklist priority)

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

	// get needed cards for decklists
	if (args.any { it.startsWith(SHOW_NEEDED_DECKLIST) }) {
		transaction {
			val expressionSumOfCardsInDeckNeeded = DeckCards.count.sum()
			val deckNeeds = DeckCards.select(expressionSumOfCardsInDeckNeeded, DeckCards.name).groupBy(DeckCards.build, DeckCards.name)
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

	// get needed cards for decklists
	if (args.any { it.startsWith(PRINT_NEEDED_DECKLIST) }) {
		transaction {
			val fontTitle = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)
			val expressionSumOfCardsInDeckNeeded = DeckCards.count.sum()
			val deckNeeds = DeckCards.select(DeckCards.count.sum(), DeckCards.name).groupBy(DeckCards.build, DeckCards.name)
					.groupingBy { it[DeckCards.name] }.aggregate<ResultRow, String, Int> { _, accumulator: Int?, element, first ->
						if(first) element[expressionSumOfCardsInDeckNeeded]!! else max(accumulator!!, element[expressionSumOfCardsInDeckNeeded]!!)
					}.mapNotNull { (cardName, neededCount) ->
						val cardPossessionsCount = CardPossessions.card.count()
						val available = (Cards innerJoin CardPossessions).select(cardPossessionsCount)
								.where { Cards.name eq cardName }
								.groupBy(Cards.name)
								.map { it[cardPossessionsCount] }.first()

						if(neededCount-available>0) {
							Pair(cardName, neededCount - available)
						} else {
							null
						}

						// todo print archetype names for which it is wanted
						// TODO print desired sets if there's also the possibility to complete collection while building the archetype
					}


			createPdfDocument(Paths.get("C:\\Users\\001121673\\private\\magic\\wants.pdf")) {
				page(PDRectangle.A4) {
					framePagePosition(50f, 20f, 20f, 20f) {

						drawText("Needs ${set.name}", fontTitle, HorizontalAlignment.CENTER, 0f, box.upperRightY-10f, Color.BLACK)

						// TODO calc metrics for all sets (so that formatting is the same for all pages)
						val baseTable = BaseTable(642f, 842f, 0f, box.width, 0f, this@createPdfDocument.document, this@page.pdPage, true, true)
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
