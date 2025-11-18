/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.masterdata.import.downloadBulkData
import at.woolph.caco.masterdata.import.updateMasterDataPrice
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import org.slf4j.LoggerFactory

class UpdatesPrices: SuspendingTransactionCliktCommand(name = "update-prices") {
  override suspend fun runTransaction() {
    downloadBulkData("default_cards") {
      context(log) {
        updateMasterDataPrice(it)
      }
    }
  }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java.declaringClass)
  }
}
