package at.woolph.caco.command

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.importer.sets.ScryfallCard
import at.woolph.caco.importer.sets.downloadBulkData
import at.woolph.caco.importer.sets.jsonSerializer
import at.woolph.caco.utils.newOrUpdate
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.decodeToSequence
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

class UpdatesPrices: CliktCommand(name = "prices") {
    override fun run() = runBlocking {
      newSuspendedTransaction {
        downloadBulkData("default_cards") {
          jsonSerializer.decodeToSequence<ScryfallCard>(it).asFlow()
            .filter(ScryfallCard::isNoPromoPackStampedAndNoPrereleasePackStampedVersion)
            .collect {
              try {
                Card.Companion.newOrUpdate(it.id) {
                  it.update(this)
                  cardmarketUri = it.purchase_uris["cardmarket"]

                  price = it.prices["eur"]?.toDouble()
                  priceFoil = it.prices["eur_foil"]?.toDouble()
                }
              } catch (t: Throwable) {
                log.error("error while updating price for card ${it.name}", t)
              }
            }
        }
      }
    }

    companion object {
        val log = LoggerFactory.getLogger(this::class.java.declaringClass)
    }
}
