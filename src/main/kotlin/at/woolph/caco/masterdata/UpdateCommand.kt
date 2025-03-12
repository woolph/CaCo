package at.woolph.caco.masterdata

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.masterdata.import.ScryfallCard
import at.woolph.caco.masterdata.import.ScryfallSet
import at.woolph.caco.masterdata.import.downloadBulkData
import at.woolph.caco.masterdata.import.importSets
import at.woolph.caco.masterdata.import.jsonSerializer
import at.woolph.caco.utils.Either
import at.woolph.caco.utils.newOrUpdate
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeToSequence
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.io.InputStream
import kotlin.io.path.inputStream

/**
 * updates the masterdata from scryfall into the database
 */
class UpdateCommand: CliktCommand(name = "update") {
    val source by mutuallyExclusiveOptions(
        option("--bulk-data", help="which bulk data to import").convert { Either.Left(it) },
        option("--file", help="file to import").path(mustExist = true).convert { Either.Right(it) },
    ).single().default(Either.Left("default_cards"))

    override fun run() = runBlocking {
      newSuspendedTransaction {
        importSets().collect {}

        @OptIn(ExperimentalSerializationApi::class)
        suspend fun processJson(it: InputStream) {
          jsonSerializer.decodeToSequence<ScryfallCard>(it).asFlow()
            .filter(ScryfallCard::isImportWorthy)
            .collect {
              try {
                Card.Companion.newOrUpdate(it.id, it::update)

                if (it.set == "plst") {
                  val (setCode, collectorNumber) = it.collector_number.split("-", limit = 2)
                  val set = ScryfallCardSet.Companion.find { ScryfallCardSets.code eq setCode }.single()
                  Card.Companion.findSingleByAndUpdate(
                    Op.Companion.build { Cards.set eq set.id and (Cards.collectorNumber eq collectorNumber) },
                    it::updateTheListPendant,
                  )
                }
              } catch (t: ScryfallCard.SetNotInDatabaseException) {
                if (t.setType != "memorabilia" || it.set in ScryfallSet.Companion.memorabiliaWhiteList)
                  log.error("error while importing card ${it.name}: ${t.message}", if (log.isDebugEnabled) t else null)
                else
                  log.debug("not importing card ${it.name} cause set is not to be imported")
              } catch (t: Throwable) {
                log.error("error while importing card ${it.name}: ${t.message}", if (log.isDebugEnabled) t else null)
              }
            }
        }

        when (val sourceX = source) {
          is Either.Left -> downloadBulkData(sourceX.value) { processJson(it) }
          is Either.Right -> sourceX.value.inputStream().use { processJson(it) }
        }
      }
    }

    companion object {
        val log = LoggerFactory.getLogger(this::class.java.declaringClass)
    }
}