/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.currency.CurrencyValue
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.masterdata.import.ScryfallCard
import at.woolph.caco.masterdata.import.downloadBulkData
import at.woolph.caco.masterdata.import.jsonSerializer
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.decodeToSequence
import org.slf4j.LoggerFactory

class UpdatesPrices : SuspendingTransactionCliktCommand(name = "update-prices") {
  override suspend fun runTransaction() {
    downloadBulkData("default_cards") {
      jsonSerializer
          .decodeToSequence<ScryfallCard>(it)
          .asFlow()
          .filter(ScryfallCard::isImportWorthy)
          .collect {
            try {
              Card.Companion.findByIdAndUpdate(it.id) { card ->
                it.update(card)
                card.cardmarketUri = it.purchase_uris["cardmarket"]

                card.priceNormal = it.prices["eur"]?.toDouble()?.let { CurrencyValue.eur(it) }
                card.priceFoil = it.prices["eur_foil"]?.toDouble()?.let { CurrencyValue.eur(it) }
                //        it.priceEtched = prices["usd_etched"]?.toDouble()?.let {
                // CurrencyValue.usd(it).exchangeTo(Currencies.EUR) }
              }
            } catch (t: Throwable) {
              log.error("error while updating price for card ${it.name}", t)
            }
          }
    }
  }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java.declaringClass)
  }
}
