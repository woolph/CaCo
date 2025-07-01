package at.woolph.caco.masterdata

import arrow.core.Either
import arrow.core.flatMap
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVariant
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.masterdata.import.ScryfallCard
import at.woolph.caco.masterdata.import.ScryfallSet
import at.woolph.caco.masterdata.import.downloadBulkData
import at.woolph.caco.masterdata.import.importSets
import at.woolph.caco.masterdata.import.jsonSerializer
import at.woolph.caco.utils.newOrUpdate
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.decodeToSequence
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

sealed interface BulkDataSource {
  suspend fun processBulkData(block: suspend (InputStream) -> Unit)
}
class BulkDataFile(val file: Path): BulkDataSource {
   override suspend fun processBulkData(block: suspend (InputStream) -> Unit) = block(file.inputStream())
}
class BulkDataApiRequest(val bulkDataName: String): BulkDataSource {
  override suspend fun processBulkData(block: suspend (InputStream) -> Unit) = downloadBulkData(bulkDataName, block)
}

/**
 * updates the masterdata from scryfall into the database
 */
class UpdateMasterdataCommand: SuspendingCliktCommand(name = "update") {
    val source by mutuallyExclusiveOptions(
        option("--bulk-data", help="which bulk data to import").convert { BulkDataApiRequest(it) },
        option("--file", help="file to import").path(mustExist = true).convert { BulkDataFile(it) },
    ).single().default(BulkDataApiRequest("default_cards"))

  override suspend fun run() {
    newSuspendedTransaction {
      importSets().collect {}
    }

    source.processBulkData { bulkDataInputStream ->
      val variant = mutableListOf<Pair<ScryfallCard, CardVariant.Type>>()

      newSuspendedTransaction {
        jsonSerializer.decodeToSequence<ScryfallCard>(bulkDataInputStream).asFlow()
          .filter(ScryfallCard::isImportWorthy)
          .collect {
            try {
              when(it.variant) {
                null -> Card.newOrUpdate(it.id, it::update)
                else -> variant.add(it to it.variant)
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
      }

      newSuspendedTransaction {
        variant.forEach { (scryfallCard, variantType) ->
          determineOriginalCardFor(scryfallCard, variantType).onRight { originalCard ->
            try {
              CardVariant.newOrUpdate(scryfallCard.id) {
                it.baseVariantCard = originalCard
                it.variantType = variantType
              }
            } catch (e: org.jetbrains.exposed.exceptions.ExposedSQLException) {
              log.error("error while importing variant card ${scryfallCard.id} ${scryfallCard.uri} ${scryfallCard.name} (which is considered to be a variant of type $variantType): ${e.message}", if (log.isDebugEnabled) e else null)

            }
          }.onLeft { t ->
            log.error(
              "error while determining the original card for ${scryfallCard.collector_number} ${scryfallCard.name} (which is considered to be a variant of type $variantType): ${t.message} ",
              if (log.isDebugEnabled) t else null
            )
          }
        }
      }
    }
  }

    companion object {
      val log = LoggerFactory.getLogger(this::class.java.declaringClass)

      internal fun determineOriginalCardFor(it: ScryfallCard, variantType: CardVariant.Type) = when(variantType) {
        CardVariant.Type.TheList -> {
          val (setCode, collectorNumber) = it.collector_number.split("-", limit = 2)
          getOriginalCard(setCode.lowercase(), collectorNumber)
        }
        CardVariant.Type.PrereleaseStamped -> {
          val collectorNumber = it.collector_number.replace(PRERELEASE_STAMPED_REPLACEMENT_PATTERN, "")
          val setCode = it.set.assumedSetCode(collectorNumber)
          getOriginalCard(setCode, collectorNumber)
        }
        CardVariant.Type.PromopackStamped -> {
          val collectorNumber = it.collector_number.replace(PROMOPACK_STAMPED_REPLACEMENT_PATTERN, "")
          val setCode = it.set.assumedSetCode(collectorNumber)
          getOriginalCard(setCode, collectorNumber)
        }
      }

      internal fun getOriginalCard(setCode: String, collectorNumber: String) =
        getSetByCode(setCode).flatMap { set ->
          Either.catch {
            Card.find { Cards.set eq set.id and (Cards.collectorNumber eq collectorNumber) }.single()
          }.mapLeft { Exception("no unique card in set $set with collectorNumber $collectorNumber found", it) }
        }

      internal fun getSetByCode(setCode: String) =
        Either.catch {
          ScryfallCardSet.find { ScryfallCardSets.code eq setCode }.single()
        }.mapLeft { Exception("no unique set with code $setCode found", it) }

      val PRERELEASE_STAMPED_REPLACEMENT_PATTERN = Regex("s(?=★?$)")
      val PROMOPACK_STAMPED_REPLACEMENT_PATTERN = Regex("p(?=★?$)")

      fun String.assumedSetCode(assumedCollectorNumber: String) =
        if (assumedCollectorNumber.contains("★")) {
          this
        } else {
          removePrefix("p")
        }
    }
}