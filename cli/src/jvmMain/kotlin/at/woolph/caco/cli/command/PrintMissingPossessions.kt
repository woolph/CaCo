///* Copyright 2025-2026 Wolfgang Mayer */
//package at.woolph.caco.cli.command
//
//import arrow.core.split
//import at.woolph.caco.datamodel.collection.CardCondition
//import at.woolph.caco.datamodel.collection.CardLanguage
//import at.woolph.caco.datamodel.collection.CardPossession
//import at.woolph.caco.datamodel.decks.Format
//import at.woolph.caco.datamodel.sets.Card
//import at.woolph.caco.datamodel.sets.CardPrint
//import at.woolph.caco.datamodel.sets.Finish
//import at.woolph.caco.datamodel.sets.Legalities
//import at.woolph.caco.datamodel.sets.Legality
//import at.woolph.caco.datamodel.sets.ScryfallCardSet
//import at.woolph.caco.datamodel.sets.deckLimit
//import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
//import at.woolph.utils.compareToNullable
//import at.woolph.utils.currency.CurrencyValue
//import at.woolph.utils.currency.sum
//import com.github.ajalt.clikt.parameters.options.default
//import com.github.ajalt.clikt.parameters.options.option
//import com.github.ajalt.clikt.parameters.options.validate
//import com.github.ajalt.clikt.parameters.types.boolean
//import com.github.ajalt.clikt.parameters.types.double
//import com.github.ajalt.clikt.parameters.types.int
//import kotlin.collections.fold
//import kotlin.io.path.Path
//import kotlin.io.path.bufferedWriter
//import kotlin.math.max
//import kotlin.uuid.Uuid
//import kotlinx.coroutines.coroutineScope
//import kotlinx.html.HTML
//import kotlinx.html.a
//import kotlinx.html.body
//import kotlinx.html.br
//import kotlinx.html.dom.createHTMLDocument
//import kotlinx.html.h1
//import kotlinx.html.h2
//import kotlinx.html.h3
//import kotlinx.html.head
//import kotlinx.html.html
//import kotlinx.html.p
//import kotlinx.html.stream.appendHTML
//import kotlinx.html.stream.createHTML
//import kotlinx.html.style
//import kotlinx.html.table
//import kotlinx.html.td
//import kotlinx.html.th
//import kotlinx.html.thead
//import kotlinx.html.title
//import kotlinx.html.tr
//import kotlinx.html.unsafe
//import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
//import java.nio.file.Path
//import kotlin.io.path.createDirectories
//import kotlin.time.Clock
//
///**
// * the idea is to get a list of cards that are playable in one of the formats considered for building and playing
// * that I do not possess in the maximum possible deck limit
// */
//class PrintMissingPossessions : SuspendingTransactionCliktCommand(name = "missing") {
//  val printSetsWithoutPossessions by option(help = "printSetsWithoutPossessions").boolean().default(false)
//  val highValueExcessThreshold by option(help = "highValueExcessThreshold").double().default(1.0)
//
//  override suspend fun runTransaction() = coroutineScope {
//    val formatsConsideredForBuilding = setOf(
//      // TODO make this configurable
//      Format.Commander,
//      Format.PauperCommander,
//      Format.PrEDH,
//      Format.Pauper,
////      Format.Pioneer,
//    )
//
//    fun Card.deckBuildingNeeds(formats: Set<Format>): Int =
//      specialDeckRestrictions ?: formats.mapNotNull { legalities?.deckLimit(it) }.max()
//
//    val result =
//      suspendTransaction { Card.all() }
//        .filter { card -> !card.token && card.type?.contains("Basic") != true }
//        .filter { card ->
//          card.prints.sumOf { it.possessions.count() } < card.deckBuildingNeeds(
//            formatsConsideredForBuilding
//          )
//        }
//        .associateWith { card ->
//          val possessions = card.prints.sumOf { it.possessions.count() }
//          val neededForDeckBuilding = card.deckBuildingNeeds(formatsConsideredForBuilding)
//
//          val (collectionBinder, remaining) =
//            card.prints
//              .flatMap(CardPrint::possessions)
//              .groupBy { possession ->
//                CollectionId(possession.cardPrint.scryfallId, possession.finish)
//              }
//              .mapValues { it.value.sortedByDescending(CardPossession::desirability) }
//              .takeFirstOfEachKey()
//
//          generateSequence(
//            CollectionSeparation(
//              neededForDeckBuilding,
//              collectionBinder.values,
//              emptyList(),
//              remaining,
//            )
//          ) { x ->
//            val (nextToBeAddedToCollectionDuplicates, newRemaining) =
//              x.remaining.takeFirstOfEachKey(
//                x.stillNeeded
//              ) // TODO prefer secondary language if primary language already in
//            // collection binder
//
//            CollectionSeparation(
//              x.neededForDeckBuilding,
//              x.collectionBinder,
//              x.collectionDuplicates + nextToBeAddedToCollectionDuplicates.values,
//              newRemaining,
//            )
//          }
//            .first { !(it.isMoreNeeded && it.isMoreAvailable) }
//            .let {
//              CollectionExcessReportItem(
//                it.collectionBinder,
//                it.collectionDuplicates,
//                it.remaining.flatMap { (_, value) -> value },
//              )
//            }
//        }
//
//    val result2 =
//      result.values
//        .associateCardBasedReportItemWithSet()
//        .filter { (_, collectionExcessReport) -> collectionExcessReport.excess.isNotEmpty() }
//        .sortedByDescending { (set, _) -> set.releaseDate }
//
//    val sets = ScryfallCardSet.allRootSets().sortedByDescending { it.releaseDate }
//
//    fun Path.writeHtml(block: HTML.() -> Unit) =
//      bufferedWriter().use { writer ->
//        writer.appendHTML().html {
//          block()
//        }
//      }
//
//    Path("./caco-missing-report.htm").writeHtml {
//      head {
//        title { +"Collection Missing Report" }
//        style {
//          unsafe {
//            raw(
//              """
//                          table, th, td {
//                            border: 1px solid black;
//                            border-collapse: collapse;
//                          }
//                          th, td {
//                            padding: 5px;
//                          }
//                          .tradable {
//                            background-color: #a0ffa0;
//                          }
//                          """.trimIndent()
//            )
//          }
//        }
//      }
//      body {
//        h1 {
//          +"Collection Missing Report"
//        }
//        h2 {
//          +"Sets"
//        }
//
//        sets.forEach { set ->
//          h3 { +"[${set.code}] ${set.name}" }
//          table {
//            tr {
//              th { +"Set" }
//              th { +"#" }
//              th { +"Name" }
//              th { +"Needed" }
//              th { +"Price" }
//              th { +"Binder" }
//              th { +"Duplicates" }
//              th { +"Excess" }
//              th { +"Other sets containing versions in Binder or Duplicates" }
//            }
//
//            set.cardsOfSelfAndNonRootChildSets.sorted().forEach { cardPrint ->
//              val result = result[cardPrint.card]
//              val other = sequenceOf(
//                result?.binder?.filter { it.cardPrint != cardPrint }?.asSequence() ?: emptySequence(),
//                result?.duplicates?.filter { it.cardPrint != cardPrint }?.asSequence() ?: emptySequence(),
//              ).flatten().groupingBy { it.cardPrint.set.code }
//                .eachCount().entries.joinToString(",") { (set, amount) -> "${amount}x $set" }
//
//              data class TempResult(
//                val finish: Finish,
//              )
//              cardPrint.finishes.map { finish ->
//                fun Collection<CardPossession>?.toPossessionString() =
//                  this?.filter { it.cardPrint == cardPrint && it.finish == finish }
//                    ?.joinToString(",") { "${it.language}-${it.condition}" } ?: ""
//
//                TempResult(
//                  finish = finish,
//                )
//              }.forEachIndexed { index, (finish) ->
//                val collectorNumber = "${cardPrint.collectorNumber}${
//                  when (finish) {
//                    Finish.Normal -> " "
//                    Finish.Foil -> "★"
//                    Finish.Etched -> "☆"
//                  }
//                }"
//
//                val cardName = cardPrint.mergedName.takeIf { index == 0 } ?: "-//-"
//                val other = other.takeIf { index == 0 } ?: ""
//                val isHighValue = (cardPrint.prices(finish)?.value ?: Double.MAX_VALUE) >= highValueExcessThreshold
//
//                tr(classes = if (isHighValue) "tradable" else null) {
//                  td { +"${cardPrint.set.code}" }
//                  td { +"$collectorNumber" }
//                  td { +"$cardName" }
//                  td { +"${cardPrint.card.deckBuildingNeeds(formatsConsideredForBuilding)}" }
//                  td { +"${cardPrint.prices(finish) ?: "n/a"}" }
//                  td { +"$other" }
//                }
//              }
//            }
//          }
//        }
//      }
//    }
//  }
//}
