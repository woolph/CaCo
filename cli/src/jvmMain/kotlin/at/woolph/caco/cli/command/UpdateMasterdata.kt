/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.CacoLogger
import at.woolph.caco.masterdata.import.downloadBulkData
import at.woolph.caco.masterdata.import.importSets
import at.woolph.caco.masterdata.import.updateMasterDataFromBulkData
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream


/** updates the masterdata from scryfall into the database */
class UpdateMasterdata: SuspendingCliktCommand(name = "update") {
  val source by
      mutuallyExclusiveOptions(
              option("--bulk-data", "-b", help = "which bulk data to import").convert {
                BulkDataApiRequest(it)
              },
              option("--file", "-f", help = "file to import").path(mustExist = true).convert {
                BulkDataFile(it)
              },
          )
          .single()
          .default(BulkDataApiRequest("default_cards"))

  sealed interface BulkDataSource {
    suspend fun processBulkData(block: suspend (InputStream) -> Unit)
  }

  class BulkDataFile(
    val file: Path,
  ) : BulkDataSource {
    override suspend fun processBulkData(block: suspend (InputStream) -> Unit) =
      block(file.inputStream())
  }

  class BulkDataApiRequest(
    val bulkDataName: String,
  ) : BulkDataSource {
    override suspend fun processBulkData(block: suspend (InputStream) -> Unit) =
      downloadBulkData(bulkDataName, block)
  }

  override suspend fun run() {
    suspendTransaction {
      importSets()
    }
    source.processBulkData { bulkDataInputStream ->
      context(log) {
        updateMasterDataFromBulkData(bulkDataInputStream)
      }
    }
  }

  companion object {
    val log = CacoLogger
  }
}
