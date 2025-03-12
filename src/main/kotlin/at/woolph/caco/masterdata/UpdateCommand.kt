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
          val prereleaseStampedVersions = mutableListOf<ScryfallCard>()
          val promopackStampedVersions = mutableListOf<ScryfallCard>()
          val theListVersions = mutableListOf<ScryfallCard>()

          jsonSerializer.decodeToSequence<ScryfallCard>(it).asFlow()
            .filter(ScryfallCard::isImportWorthy)
            .collect {
              try {
                when {
                  it.isPrereleaseStampedVersion -> prereleaseStampedVersions.add(it)
                  it.isPromopackStampedVersion -> promopackStampedVersions.add(it)
                  it.isTheListVersion ->  {
                    theListVersions.add(it)
                    Card.newOrUpdate(it.id, it::update)
                  }
                  else -> Card.newOrUpdate(it.id, it::update)
                }
              } catch (t: ScryfallCard.SetNotInDatabaseException) {
                if (t.setType != "memorabilia" || it.set in ScryfallSet.memorabiliaWhiteList)
                  log.error("error while importing card ${it.name}: ${t.message}", if (log.isDebugEnabled) t else null)
                else
                  log.debug("not importing card ${it.name} cause set is not to be imported")
              } catch (t: Throwable) {
                log.error("error while importing card ${it.name}: ${t.message}", if (log.isDebugEnabled) t else null)
              }
            }
          theListVersions.forEach {
            try {
              val (setCode, collectorNumber) = it.collector_number.split("-", limit = 2)
              val set = ScryfallCardSet.find { ScryfallCardSets.code eq setCode.lowercase() }.single()
              Card.findSingleByAndUpdate(
                Op.build { Cards.set eq set.id and (Cards.collectorNumber eq collectorNumber) },
                it::updateTheListVersion,
              )
            } catch (t: ScryfallCard.SetNotInDatabaseException) {
              if (t.setType != "memorabilia" || it.set in ScryfallSet.memorabiliaWhiteList)
                log.error("error while importing card ${it.name}: ${t.message}", if (log.isDebugEnabled) t else null)
              else
                log.debug("not importing card ${it.name} cause set is not to be imported")
            } catch (t: Throwable) {
              log.error("error while importing card ${it.collector_number} ${it.name}: ${t.message} ", if (log.isDebugEnabled) t else null)
            }
          }
          prereleaseStampedVersions.forEach {
            val setCode = it.set.removePrefix("p")
            val collectorNumber = it.collector_number.removeSuffix("s")
            val set = ScryfallCardSet.find { ScryfallCardSets.code eq setCode }.single()
            Card.findSingleByAndUpdate(
              Op.build { Cards.set eq set.id and (Cards.collectorNumber eq collectorNumber) },
              it::updatePrereleaseStampedVersion,
            )
          }
          promopackStampedVersions.forEach {
            val setCode = it.set.removePrefix("p")
            val collectorNumber = it.collector_number.removeSuffix("p")
            val set = ScryfallCardSet.find { ScryfallCardSets.code eq setCode }.single()
            Card.findSingleByAndUpdate(
              Op.build { Cards.set eq set.id and (Cards.collectorNumber eq collectorNumber) },
              it::updatePromopackStampedVersion,
            )
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