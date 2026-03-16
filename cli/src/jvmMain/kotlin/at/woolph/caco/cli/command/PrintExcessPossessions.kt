/* Copyright 2025-2026 Wolfgang Mayer */
package at.woolph.caco.cli.command

import arrow.core.split
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardPrint
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.utils.compareToNullable
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.collections.fold
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.math.max
import kotlin.uuid.Uuid

/**
 * run over every card (distinct by oracle-id or english name, because i don't care if the playset consists of cards from different sets)
 * check if I possess <=4 (or whather custom deck limit the card has) copies, if so skip the card for this report
 * otherwise for each card variation (set, normal/alternative art/frame, nonfoil/foil) assign 1 card to the collection binder (using language preference to select on) and assign the rest
 * check if the collection binder assigned amount of cards is <= 4 (or whather custom deck limit the card has) , if so skip the card
 * otherwise determine the missing amount of cards to fill the collection to the desired possession limit (considering custom deck limits) and select a diverse (maybe different languages this time)
 *
 * maybe we can use the same process of selecting each card variation once of the remaining list
 * in the first run, we do it without a limit
 * from the second run onwards we do it up to the point, until we have the desired amount of cards in the collection or the remaining is empty
 * after we have stopped, the remaining list is the excess to be sold
 *
 * print the excess to the
 */
class PrintExcessPossessions : SuspendingTransactionCliktCommand(name = "excess") {
  val DEFAULT_FOR_DECK_BUILDING = 4 // TODO make configurable through cli arguments
  val printBinderCards: Boolean = false

  override suspend fun runTransaction() = coroutineScope {

    fun CardPossession.desirability(): Double {
      return 1.0 * when (finish) {
        Finish.Normal -> 1.0
        Finish.Foil -> 0.5
        Finish.Etched -> 0.6
      } * when (language) {
        CardLanguage.ENGLISH -> 1.0
        CardLanguage.GERMAN -> 0.7
        else -> 0.1
      } * when (condition) {
        CardCondition.NEAR_MINT -> 1.0
        CardCondition.EXCELLENT -> 0.8
        CardCondition.GOOD -> 0.6
        CardCondition.PLAYED -> 0.4
        CardCondition.POOR -> 0.2
        CardCondition.UNKNOWN -> 0.7
      }
    }

    val result = suspendTransaction {
      Card.all()
    }
        .filter { card -> !card.token && card.type?.contains("Basic") != true }
        .filter { card -> card.prints.any { it.possessions.count() > 0 } }
        .map { card ->
          val neededForDeckBuilding = card.specialDeckRestrictions ?: DEFAULT_FOR_DECK_BUILDING

          val (collectionBinder, remaining) = card.prints.flatMap(CardPrint::possessions)
            .groupBy { possession -> CollectionId(possession.cardPrint.scryfallId, possession.finish) }
            .mapValues { it.value.sortedByDescending(CardPossession::desirability) }.takeFirstOfEachKey()

           generateSequence(CollectionSeparation(
            neededForDeckBuilding,
            collectionBinder.values,
            emptyList(),
            remaining,
          )) { x ->
            val (nextToBeAddedToCollectionDuplicates, newRemaining) = x.remaining.takeFirstOfEachKey(x.stillNeeded) // TODO prefer secondary language if primary language already in collection binder

            CollectionSeparation(
              x.neededForDeckBuilding,
              x.collectionBinder,
              x.collectionDuplicates + nextToBeAddedToCollectionDuplicates.values,
              newRemaining,
            )
          }.first { !(it.isMoreNeeded && it.isMoreAvailable) }
             .let { CollectionExcessReportItem(
               it.collectionBinder,
               it.collectionDuplicates,
               it.remaining.flatMap { (_, value) -> value }
             ) }
//            .also { println("${card.name} => $it") }
        }

    Path("./caco-excess-report.yml").bufferedWriter().use { bw ->
      result.flatMap {
        val sets = sequenceOf(
         it.binder,
          it.duplicates,
          it.excess,
        ).flatMap { it.map { it.cardPrint.set } }.toSet()

        sets.associateWith { currentSet ->
          CollectionExcessReportItem(
            it.binder.filter { it.cardPrint.set == currentSet },
            it.duplicates.filter { it.cardPrint.set == currentSet },
            it.excess.filter { it.cardPrint.set == currentSet },
          )
        }.entries
      }
        .groupingBy { (set, _) -> set }
        .fold(CollectionExcessReportItem.EMPTY, CollectionExcessReportItem::merge)
        .entries
        .filter { (_, collectionExcessReport) -> collectionExcessReport.excess.isNotEmpty()}
        .sortedByDescending { (set, _) -> set.releaseDate }
        .forEach { (set, collectionExcessReport) ->
          bw.write("${set.code}:\n")
          bw.write("  name: \"${set.name}\"\n")
          bw.write("  collection:\n")
          if (printBinderCards && collectionExcessReport.binder.isNotEmpty()) {
            bw.write("    binder:\n")
            collectionExcessReport.binder.sortedAndMerged().forEach {
              bw.write("    - \"$it\"\n")
            }
          }
          if(collectionExcessReport.duplicates.isNotEmpty()) {
            bw.write("    duplicates:\n")
            collectionExcessReport.duplicates.sortedAndMerged().forEach {
              bw.write("    - \"$it\"\n")
            }
          }
          bw.write("    excess:\n")
          collectionExcessReport.excess.sortedAndMerged().forEach {
            bw.write("    - \"$it\"\n")
          }
        }
    }
  }
}

/** TODO should we consider language and/or condition too?! */
data class ReportItemId(
  val cardPrint: CardPrint,
  val finish: Finish,
): Comparable<ReportItemId> {
  override fun compareTo(other: ReportItemId): Int =
    cardPrint.compareToNullable(other.cardPrint) ?: finish.compareTo(other.finish)

  override fun toString(): String = String.format("%4s%s '%s'", "#${cardPrint.collectorNumber}", when(finish) {
    Finish.Normal -> " "
    Finish.Foil -> "★"
    Finish.Etched -> "☆"
  },cardPrint.mergedName)
}

fun Collection<CardPossession>.sortedAndMerged() = groupBy { ReportItemId(it.cardPrint, it.finish) }.entries
  .sortedBy { it.key }
  .map { (reportId, possessions) -> String.format("%2dx %s", possessions.size, reportId) }

data class CollectionId(
  val scryfallId: Uuid,
  val finish: Finish,
)

data class CollectionExcessReportItem(
  val binder: Collection<CardPossession>,
  val duplicates: Collection<CardPossession>,
  val excess: Collection<CardPossession>,
) {
  operator fun plus(other: CollectionExcessReportItem): CollectionExcessReportItem =
    CollectionExcessReportItem(
      binder.plus(other.binder),
      duplicates.plus(other.duplicates),
      excess.plus(other.excess),
    )

  companion object {
    val EMPTY = CollectionExcessReportItem(
      binder = emptyList(),
      duplicates = emptyList(),
      excess = emptyList(),
    )

    fun merge(it: CollectionExcessReportItem, other: Map.Entry<*, CollectionExcessReportItem>): CollectionExcessReportItem =
      it + other.value
  }
}

data class CollectionSeparation(
  val neededForDeckBuilding: Int,
  val collectionBinder: Collection<CardPossession>,
  val collectionDuplicates: Collection<CardPossession>,
  val remaining: Map<CollectionId, Collection<CardPossession>>,
) {
  val stillNeeded: Int = max(0, neededForDeckBuilding - collectionBinder.size - collectionDuplicates.size)
  val isMoreNeeded: Boolean = stillNeeded > 0
  val isMoreAvailable: Boolean = remaining.isNotEmpty() && remaining.any { it.value.isNotEmpty() }
}

fun <K, I> Map<K, Collection<I>>.takeFirstOfEachKey(atMost: Int = Int.MAX_VALUE): Pair<Map<K, I>, Map<K,Collection<I>>> {
  val firsts = mutableMapOf<K, I>()
  val remaining = mutableMapOf<K,Collection<I>>()

  forEach { (k, list) ->
    if (firsts.size < atMost) {
      list.split()?.let { (remains, firstElement) ->
        firsts[k] = firstElement
        if (remains.isNotEmpty())
          remaining[k] = remains
      }
    } else {
      remaining[k] = list
    }
  }

  return firsts.toMap() to remaining.toMap()
}
