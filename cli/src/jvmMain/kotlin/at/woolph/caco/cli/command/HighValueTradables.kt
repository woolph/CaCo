/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.collection.DEFAULT_COLLECTION_SETTINGS
import at.woolph.caco.currency.CurrencyValue
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double

class HighValueTradables : SuspendingTransactionCliktCommand() {
  val priceThreshold by
      option(help = "The price threshold above which cards are considered 'high value'")
          .double()
          .default(1.0)

  override suspend fun runTransaction() {
    val collectionSettings = DEFAULT_COLLECTION_SETTINGS
    ScryfallCardSet.all()
        .map { set ->
          val cardsSorted = set.cards.sortedBy { it.collectorNumber }

          set to
              cardsSorted
                  .asSequence()
                  .flatMap { card ->
                    val suffixName =
                        if (
                            cardsSorted
                                .asSequence()
                                .filter { it2 -> it2.name == card.name && it2.extra == card.extra }
                                .count() > 1
                        ) {
                          val numberInSetWithSameName =
                              cardsSorted
                                  .asSequence()
                                  .filter { it2 ->
                                    it2.name == card.name &&
                                        it2.extra == card.extra &&
                                        it2.collectorNumber < card.collectorNumber
                                  }
                                  .count() + 1
                          " (V.$numberInSetWithSameName)"
                        } else {
                          ""
                        }
                    val suffixSet =
                        when {
                          card.promo -> ": Promos"
                          card.extra -> ": Extras"
                          else -> ""
                        }

                    Finish.entries
                        .asSequence()
                        .map { finish ->
                          val excess =
                              card.possessions.count { it.finish == finish } -
                                  collectionSettings.possessionTarget(card, finish)

                          val finishedSuffix =
                              when (finish) {
                                Finish.Normal -> ""
                                Finish.Foil -> "(Foil)"
                                Finish.Etched -> "(Etched)"
                              }

                          ExcessPossionItem(
                              excess,
                              "${card.name}$suffixName$finishedSuffix",
                              card.set.name.let { "$it$suffixSet" },
                              card.prices(finish) ?: CurrencyValue.eur(100000.0),
                          )
                        }
                        .filter { it.excess > 0 && it.cardPrice.value >= priceThreshold }
                  }
                  .joinToString("\n")
        }
        .filter { (_: ScryfallCardSet, cards: String) -> cards.isNotBlank() }
        .forEach { (set: ScryfallCardSet, cards: String) ->
          echo("------------------------\n${set.name}\n$cards")
          echo()
        }
  }

  data class ExcessPossionItem(
    val excess: Int,
    val cardName: String,
    val setName: String,
    val cardPrice: CurrencyValue,
  ) {
    override fun toString() = "$excess $cardName ($setName) $cardPrice"
  }
}
