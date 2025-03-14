package at.woolph.caco.collection

import at.woolph.caco.cli.SuspendingTransactionCliktCommand
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.SetType

class PrintMissingCommander : SuspendingTransactionCliktCommand(name = "missing-cmd") {
  override suspend fun runTransaction() {
    val blacklist = listOf("cmd", "drc", "fic", "c13", "c14", "c15", "c16", "cma", "c17", "tdc")
    ScryfallCardSet.all().filter {
      it.type == SetType.COMMANDER && it.code !in blacklist
    }.flatMap {
      val missingCardsForCollection =
        it.cards.filter { card -> !card.token && !card.promo && !card.extendedArt && card.possessions.count() < 1 }
      missingCardsForCollection
    }.forEach { card ->
      println("${card.name} [${card.set.code}] ${card.type}")
    }
  }
}
