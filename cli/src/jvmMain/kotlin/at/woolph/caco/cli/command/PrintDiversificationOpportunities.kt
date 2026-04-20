/* Copyright 2025-2026 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardPrint
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import kotlin.collections.fold
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * run over every card (distinct by oracle-id or english name, because i don't care if the playset
 * consists of cards from different sets) check if I possess <=4 (or whather custom deck limit the
 * card has) copies, if so skip the card for this report otherwise for each card variation (set,
 * normal/alternative art/frame, nonfoil/foil) assign 1 card to the collection binder (using
 * language preference to select on) and assign the rest check if the collection binder assigned
 * amount of cards is <= 4 (or whather custom deck limit the card has) , if so skip the card
 * otherwise determine the missing amount of cards to fill the collection to the desired possession
 * limit (considering custom deck limits) and select a diverse (maybe different languages this time)
 *
 * maybe we can use the same process of selecting each card variation once of the remaining list in
 * the first run, we do it without a limit from the second run onwards we do it up to the point,
 * until we have the desired amount of cards in the collection or the remaining is empty after we
 * have stopped, the remaining list is the excess to be sold
 *
 * print the excess to the
 */
class PrintDiversificationOpportunities : SuspendingTransactionCliktCommand(name = "diversify") {
  val defaultDeckBuildingCount by
      option(help = "default amount needed for deck building (usually a playset of 4)")
          .int()
          .default(4)
          .validate { require(it > 0) { "defaultDeckBuildingCount must be greater than 0" } }
  val printBinderCards by option(help = "printBinderCards").boolean().default(false)
  val printDuplicateCards by option(help = "printDuplicateCards").boolean().default(true)
  val tooExpensiveThreshold by option(help = "tooExpensiveThreshold").double().default(100.0)

  override suspend fun runTransaction() = coroutineScope {
    fun CardPossession.desirability(): Double {
      return 1.0 *
          when (finish) {
            Finish.Normal -> 1.0
            Finish.Foil -> 0.5
            Finish.Etched -> 0.6
          } *
          when (language) {
            CardLanguage.ENGLISH -> 1.0
            CardLanguage.GERMAN -> 0.7
            else -> 0.1
          } *
          when (condition) {
            CardCondition.NEAR_MINT -> 1.0
            CardCondition.EXCELLENT -> 0.8
            CardCondition.GOOD -> 0.6
            CardCondition.PLAYED -> 0.4
            CardCondition.POOR -> 0.2
            CardCondition.UNKNOWN -> 0.7
          }
    }

    val result =
        suspendTransaction { Card.all() }
            .filter { card -> !card.token && card.type?.contains("Basic") != true }
            .filter { card -> card.prints.any { it.possessions.count() > 0 } }
            .mapNotNull { card ->
              val neededForDeckBuilding = card.specialDeckRestrictions ?: defaultDeckBuildingCount

              val cardPrintings =
                  card.prints
                      .flatMap { it.finishes.map { finish -> CollectionId(it.scryfallId, finish) } }
                      .toSet()

              val cards =
                  card.prints.flatMap(CardPrint::possessions).groupBy { possession ->
                    CollectionId(possession.cardPrint.scryfallId, possession.finish)
                  }

              val missingPrints = cardPrintings - cards.keys

              if (missingPrints.isEmpty()) {
                return@mapNotNull null
              }

              val (collectionBinder, remaining) =
                  cards
                      .mapValues { it.value.sortedByDescending(CardPossession::desirability) }
                      .takeFirstOfEachKey()

              generateSequence(
                      CollectionSeparation(
                          neededForDeckBuilding,
                          collectionBinder.values,
                          emptyList(),
                          remaining,
                      )
                  ) { x ->
                    val (nextToBeAddedToCollectionDuplicates, newRemaining) =
                        x.remaining.takeFirstOfEachKey(
                            x.stillNeeded
                        ) // TODO prefer secondary language if primary language already in
                    // collection binder

                    CollectionSeparation(
                        x.neededForDeckBuilding,
                        x.collectionBinder,
                        x.collectionDuplicates + nextToBeAddedToCollectionDuplicates.values,
                        newRemaining,
                    )
                  }
                  .first { !(it.isMoreNeeded && it.isMoreAvailable) }
                  .let {
                    CollectionDiversificationReportItem(
                        it.collectionDuplicates,
                        missingPrints,
                    )
                  }
            }

    val result2 =
        result.associateCardBasedReportItemWithSet().sortedByDescending { (set, _) ->
          set.releaseDate
        }

    Path("./caco-diversification-report.yml").bufferedWriter().use { bw ->
      result2.forEach { (set, collectionDiversificationReport) ->
        bw.write("${set.code}:\n")
        bw.write("  name: \"${set.name}\"\n")
        if (printDuplicateCards && collectionDiversificationReport.duplicates.isNotEmpty()) {
          bw.write("  duplicatesWorthyOfDiversifying:\n")
          collectionDiversificationReport.duplicates.sortedAndMerged().forEach {
            bw.write("    - name: \"$it\"\n")
            bw.write("      differentVersion:\n")
            collectionDiversificationReport.missingPrints
                .map { CardPrint.findById(it.scryfallId) to it.finish }
                .forEach { (cardPrint, finish) -> bw.write("      - \"$cardPrint\" $finish\n") }
          }
        }
      }
    }
  }
}

data class CollectionDiversificationReportItem(
    val duplicates: Collection<CardPossession>,
    val missingPrints: Set<CollectionId>,
) {
  operator fun plus(
      other: CollectionDiversificationReportItem
  ): CollectionDiversificationReportItem =
      CollectionDiversificationReportItem(
          duplicates.plus(other.duplicates),
          missingPrints = if (missingPrints.isEmpty()) other.missingPrints else missingPrints,
      )

  companion object {
    val EMPTY =
        CollectionDiversificationReportItem(
            duplicates = emptyList(),
            missingPrints = emptySet(),
        )

    fun merge(
        it: CollectionDiversificationReportItem,
        other: Map.Entry<*, CollectionDiversificationReportItem>,
    ): CollectionDiversificationReportItem = it + other.value
  }
}

fun Collection<CollectionDiversificationReportItem>.associateCardBasedReportItemWithSet():
    Collection<Map.Entry<ScryfallCardSet, CollectionDiversificationReportItem>> =
    this.flatMap {
          val sets =
              sequenceOf(
                      it.duplicates,
                  )
                  .flatMap { it.map { it.cardPrint.set } }
                  .toSet()

          sets
              .associateWith { currentSet ->
                CollectionDiversificationReportItem(
                    it.duplicates.filter { it.cardPrint.set == currentSet },
                    it.missingPrints,
                )
              }
              .entries
        }
        .groupingBy { (set, _) -> set }
        .fold(CollectionDiversificationReportItem.EMPTY, CollectionDiversificationReportItem::merge)
        .entries
