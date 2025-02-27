package at.woolph.caco.command

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.importer.sets.ScryfallCard
import at.woolph.caco.importer.sets.ScryfallCard.SetNotInDatabaseException
import at.woolph.caco.importer.sets.ScryfallSet
import at.woolph.caco.importer.sets.downloadBulkData
import at.woolph.caco.importer.sets.importSets
import at.woolph.caco.importer.sets.jsonSerializer
import at.woolph.caco.utils.newOrUpdate
import at.woolph.caco.utils.Either
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
import kotlinx.serialization.json.decodeToSequence
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.io.InputStream
import kotlin.io.path.inputStream

class ImportScryfall: CliktCommand(name = "scryfall") {
    val source by mutuallyExclusiveOptions(
        option("--bulk-data", help="which bulk data to import").convert { Either.Left(it) },
        option("--file", help="file to import").path(mustExist = true).convert { Either.Right(it) },
    ).single().default(Either.Left("default_cards"))

    override fun run() = runBlocking {
      newSuspendedTransaction {
        importSets().collect {}

        suspend fun processJson(it: InputStream) {
          jsonSerializer.decodeToSequence<ScryfallCard>(it).asFlow()
            .filter(ScryfallCard::isImportWorthy)
            .collect {
              try {
                Card.newOrUpdate(it.id) {
                  it.update(this)
                }
              } catch (t: SetNotInDatabaseException) {
                if (t.setType != "memorabilia" || it.set in ScryfallSet.memorabiliaWhiteList)
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
