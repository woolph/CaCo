/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.cli.SuspendingTransactionCliktCommand
import at.woolph.caco.datamodel.sets.LayoutType
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.datamodel.sets.SetType

enum class CardTypes {
    NORMAL,
    SCHEME,
    PLANAR,
}

class PrintMissingCommander : SuspendingTransactionCliktCommand(name = "missing-cmd") {
    override suspend fun runTransaction() {
        val blacklist = listOf("fic", "cmd", "c13", "c14", "c15", "c16", "c17", "cma", "scd")
        ScryfallCardSet
            .find { ScryfallCardSets.type eq SetType.COMMANDER }
            .filter { it.code !in blacklist }
            .flatMap {
                it.cards.filter { card ->
                    !card.token && !card.promo && !card.extendedArt && card.possessions.count() < 1 &&
                        card.promoType.isEmpty()
                }
            }.groupBy {
                when (it.layout) {
                    LayoutType.SCHEME -> CardTypes.SCHEME
                    LayoutType.PLANAR -> CardTypes.PLANAR
                    else -> CardTypes.NORMAL
                }
            }.forEach { (type, cards) ->
                println("$type:")
                cards.sorted().forEach { card ->
                    println("  ${card.set.code} #${card.collectorNumber} ${card.mergedName} (${card.type})")
                }
            }
    }
}
