/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.cli.SuspendingTransactionCliktCommand
import at.woolph.caco.currency.Currencies
import at.woolph.caco.currency.CurrencyValue
import at.woolph.caco.currency.Percentage
import at.woolph.caco.datamodel.sets.ScryfallCardSet

class PrintMissingStats : SuspendingTransactionCliktCommand(name = "missing-stats") {
    override suspend fun runTransaction() {
        data class MissingStats(
            val count: Int,
            val total: Int,
            val costs: CurrencyValue,
        ) {
            val completionPercentage: Percentage
                get() = Percentage(1.0 - count.toDouble() / total)
        }

        val missingStatsPerSet =
            ScryfallCardSet
                .all()
                .filter {
                    !it.digitalOnly && it.cardCount > 50
                }.map {
                    val overallCardCount = it.cards.count { !it.token }
                    val missingCardsForCollection = it.cards.filter { card -> !card.token && card.possessions.count() < 1 }
                    val count = missingCardsForCollection.count()
                    val costs = missingCardsForCollection.sumOf { it.price ?: 10.0 }
                    it to MissingStats(count, overallCardCount, CurrencyValue(costs, Currencies.USD))
                }.sortedBy { it.second.costs }

        println("Set code\tSet name\tCompletion\tEstimated cost to complete\tMissing cards")
        missingStatsPerSet.forEach { (set, stats) ->
            println("${set.code}\t${set.name}\t${stats.completionPercentage}\t${stats.costs}\t${stats.count}")
        }
    }
}
