/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.datamodel.sets.LayoutType
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

class PrintMissing : SuspendingTransactionCliktCommand(name = "missing") {
  val setCode by argument(help = "set to be imported")

  override suspend fun runTransaction() {
    val set = ScryfallCardSet.findByCode(setCode) ?: throw IllegalArgumentException("Set $setCode not found")
    set.cards.filter { card ->
      !card.token &&
        !card.promo &&
        !card.extendedArt &&
        card.possessions.count() < 1 &&
        card.promoType.isEmpty()
    }.groupBy {
      when (it.layout) {
        LayoutType.SCHEME -> CardTypes.SCHEME
        LayoutType.PLANAR -> CardTypes.PLANAR
        else -> CardTypes.NORMAL
      }
    }
    .forEach { (type, cards) ->
      println("$type:")
      cards.sorted().forEach { card ->
        println("1x ${card.mergedName} (${card.set.name})")
      }
    }
  }

  enum class CardTypes {
    NORMAL,
    SCHEME,
    PLANAR,
  }
}
