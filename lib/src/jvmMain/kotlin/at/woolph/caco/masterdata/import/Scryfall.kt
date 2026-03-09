/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import arrow.core.Either
import arrow.core.flatMap
import at.woolph.utils.currency.CurrencyValue
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardPrint
import at.woolph.caco.datamodel.sets.CardPrints
import at.woolph.caco.datamodel.sets.CardPrintVariant
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.utils.exposed.newOrUpdate
import at.woolph.utils.ktor.jsonSerializer
import at.woolph.utils.ktor.request
import at.woolph.utils.ktor.useHttpClient
import co.touchlab.kermit.Logger
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeToSequence
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("at.woolph.caco.importer.sets.Scryfall")

// TODO import double sided tokens as they are printed (especially those of the commander precons)
suspend fun importSet(setCode: String): ScryfallCardSet =
    useHttpClient(Dispatchers.IO) { client ->
      val response: HttpResponse = client.get("https://api.scryfall.com/sets/$setCode")
      LOG.info("importing set $setCode")

      if (!response.status.isSuccess())
          throw Exception(
              "request GET https://api.scryfall.com/sets/$setCode with status code ${response.status.description}"
          )

      val scryfallSet = response.body<ScryfallSet>()

      if (scryfallSet.isImportWorthy) {
        ScryfallCardSet.newOrUpdate(scryfallSet.id, scryfallSet::update)
      } else {
        throw Exception("result is not considered import")
      }
    }

suspend fun loadCard(cardId: String): ScryfallCard =
    useHttpClient(Dispatchers.IO) { client ->
      val response: HttpResponse = client.get("https://api.scryfall.com/cards/$cardId")

      if (!response.status.isSuccess())
          throw Exception(
              "request GET https://api.scryfall.com/cards/$cardId with status code ${response.status.description}"
          )

      return@useHttpClient response.body<ScryfallCard>()
    }

suspend fun ScryfallCardSet.reimport() =
    useHttpClient(Dispatchers.IO) { client ->
      LOG.debug("update set {}", this@reimport)
      val response: HttpResponse = client.get("https://api.scryfall.com/sets/${this@reimport.code}")

      if (!response.status.isSuccess())
          throw Exception("request failed with status code ${response.status.description}")

      val scryfallSet: ScryfallSet = response.body()

      scryfallSet.update(this@reimport)
    }

internal fun loadSetsFromScryfall(): Flow<ScryfallSet> =
    paginatedDataRequest<ScryfallSet>("https://api.scryfall.com/sets")

suspend fun importSets() {
  val (importWorthySets, _) = loadSetsFromScryfall().toList().partition(ScryfallSet::isImportWorthy)

  importWorthySets.forEach {
    try {
      val setId = it.id

      ScryfallCardSet.newOrUpdate(setId) { scryfallCardSet -> it.update(scryfallCardSet) }
    } catch (t: Throwable) {
      LOG.error("error while importing set ${it.name}", t)
    }
  }

  // FIXME ask scryfall to add these oversized dungeon tokens to their database
//  val afr = ScryfallCardSet.findByCode("afr") ?: throw Exception("set afr not found")
//  val oafrId = Uuid.parse("c954ce81-07b0-4881-b350-af3d7780ec22")
//  ScryfallCardSet.newOrUpdate(oafrId) { scryfallCardSet ->
//    scryfallCardSet.code = "oafr"
//    scryfallCardSet.name = "Adventures in the Forgotten Realms Oversized"
//    scryfallCardSet.parentSetCode = afr.code
//    scryfallCardSet.cardCount = 3
//    scryfallCardSet.digitalOnly = false
//    scryfallCardSet.type = SetType.TOKEN
//    scryfallCardSet.releaseDate = afr.releaseDate
//  }
//
//  mapOf(
//          "6f509dbe-6ec7-4438-ab36-e20be46c9922" to
//              "20665182-5b20-4bb7-8638-4bea6bcfabb3", // Dungeon of the Mad Mage
//          "59b11ff8-f118-4978-87dd-509dc0c8c932" to
//              "3377d60a-586d-4e59-8f6c-4c27664c1f40", // Lost Mine of Phandelver
//          "70b284bd-7a8f-4b60-8238-f746bdc5b236" to
//              "3ccf204e-8431-457c-aa3e-d0e2703f5a32", // Tomb of Annihilation
//      )
//      .forEach { (nonOversizedVersionId, oversizedVersionId) ->
//        val nonOversizedVersion = loadCard(nonOversizedVersionId)
//        val id0 = UUID.fromString(oversizedVersionId)
//        Card.newOrUpdate(id0) { card ->
//          nonOversizedVersion
//              .copy(
//                  oversized = true,
//                  id = id0,
//                  set = "oafr",
//                  set_id = oafrId,
//              )
//              .update(card)
//        }
//      }

  // FIXME possessions of oversized dungeon cards are lost when importing to archidekt and
  // reimporting the archidekt export!!!!!
  // FIXME add oversized undercity dungeon
  // FIXME prerelease-stamped promo-stamped are lost when importing to archidekt and reimporting the
  // archidekt export!!!!! => add
}

suspend fun downloadBulkData(type: String, block: suspend (InputStream) -> Unit) {
  val bulkData = request<ScryfallBulkData>("https://api.scryfall.com/bulk-data/${type}")

  withContext(Dispatchers.IO) {
    bulkData.downloadUri.toURL().openConnection().getInputStream().use { block(it) }
  }
}

@OptIn(ExperimentalSerializationApi::class)
context(log: Logger)
suspend fun updateMasterDataFromBulkData(bulkDataInputStream: InputStream) {
  val variant = mutableListOf<Pair<ScryfallCard, CardPrintVariant.Type>>()

  suspendTransaction {
    jsonSerializer
      .decodeToSequence<ScryfallCard>(bulkDataInputStream)
      .asFlow()
      .filter(ScryfallCard::isImportWorthy)
      .collect { scryfallCard ->
        try {
          when (val cardPrintVariant = scryfallCard.variant) {
            null -> {
              val oracleId = scryfallCard.oracle_id ?: scryfallCard.card_faces?.firstNotNullOf { it.oracle_id }
              ?: throw IllegalStateException("no orcacle_id for scryfall_id ${scryfallCard.id}")

              val card = Card.newOrUpdate(oracleId, scryfallCard::update)
              CardPrint.newOrUpdate(scryfallCard.id) {
                scryfallCard.update(it)
                it.card = card
              }
            }

            else -> variant.add(scryfallCard to cardPrintVariant)
          }
        } catch (t: ScryfallCard.SetNotInDatabaseException) {
          if (t.setType != "memorabilia" || scryfallCard.set in ScryfallSet.memorabiliaWhiteList) {
            log.e(t) { "error while importing card ${scryfallCard.name}" }
          } else {
            log.d("not importing card ${scryfallCard.name} cause set ${scryfallCard.set} is not to be imported")
          }
        } catch (t: Throwable) {
          log.e(t) { "error while importing card ${scryfallCard.name}" }
        }
      }
  }
  suspendTransaction {
    variant.forEach { (scryfallCard, variantType) ->
      determineOriginalCardFor(scryfallCard, variantType)
        .onRight { originalCard ->
          try {
            CardPrintVariant.newOrUpdate(scryfallCard.id) {
              it.baseVariantCard = originalCard
              it.variantType = variantType
            }
          } catch (e: ExposedSQLException) {
            log.e("error while importing variant card ${scryfallCard.id} ${scryfallCard.uri} ${scryfallCard.name} (which is considered to be a variant of type $variantType): ${e.message}")
          }
        }
        .onLeft { t ->
          log.e("error while determining the original card for ${scryfallCard.collector_number} ${scryfallCard.name} (which is considered to be a variant of type $variantType): ${t.message}")
        }
    }
  }
}

internal fun determineOriginalCardFor(
  it: ScryfallCard,
  variantType: CardPrintVariant.Type,
) =
  when (variantType) {
    CardPrintVariant.Type.TheList -> {
      val (setCode, collectorNumber) = it.collector_number.split("-", limit = 2)
      getOriginalCard(setCode.lowercase(), collectorNumber)
    }
    CardPrintVariant.Type.PrereleaseStamped -> {
      val collectorNumber =
        it.collector_number.replace(PRERELEASE_STAMPED_REPLACEMENT_PATTERN, "")
      val setCode = it.set.assumedSetCode(collectorNumber)
      getOriginalCard(setCode, collectorNumber)
    }
    CardPrintVariant.Type.PromopackStamped -> {
      val collectorNumber =
        it.collector_number.replace(PROMOPACK_STAMPED_REPLACEMENT_PATTERN, "")
      val setCode = it.set.assumedSetCode(collectorNumber)
      getOriginalCard(setCode, collectorNumber)
    }
  }

internal fun getOriginalCard(
  setCode: String,
  collectorNumber: String,
) =
  getSetByCode(setCode).flatMap { set ->
    Either.catch {
      CardPrint.find {
        CardPrints.set eq set.id and (CardPrints.collectorNumber eq collectorNumber)
      }
        .single()
    }
      .mapLeft {
        Exception(
          "no unique card in set $set with collectorNumber $collectorNumber found",
          it,
        )
      }
  }

internal fun getSetByCode(setCode: String) =
  Either.catch {
    ScryfallCardSet.find { ScryfallCardSets.code eq setCode }.single()
  }
    .mapLeft { Exception("no unique set with code $setCode found", it) }

val PRERELEASE_STAMPED_REPLACEMENT_PATTERN = Regex("s(?=★?$)")
val PROMOPACK_STAMPED_REPLACEMENT_PATTERN = Regex("p(?=★?$)")

fun String.assumedSetCode(assumedCollectorNumber: String) =
  if (assumedCollectorNumber.contains("★")) {
    this
  } else {
    removePrefix("p")
  }

@OptIn(ExperimentalSerializationApi::class)
context(log: Logger)
suspend fun updateMasterDataPrice(bulkDataInputStream: InputStream) {
  jsonSerializer
    .decodeToSequence<ScryfallCard>(bulkDataInputStream)
    .asFlow()
    .filter(ScryfallCard::isImportWorthy)
    .collect { scryfallCard ->
      try {
        CardPrint.findByIdAndUpdate(scryfallCard.id) { cardPrint ->
          scryfallCard.update(cardPrint)
          cardPrint.cardmarketUri = scryfallCard.purchase_uris["cardmarket"]

          cardPrint.priceNormal = scryfallCard.prices["eur"]?.toDouble()?.let { CurrencyValue.eur(it) }
          cardPrint.priceFoil = scryfallCard.prices["eur_foil"]?.toDouble()?.let { CurrencyValue.eur(it) }
          //        cardPrint.priceEtched = scryfallCard.prices["usd_etched"]?.toDouble()?.let {
          // CurrencyValue.usd(it).exchangeTo(Currencies.EUR) }
        }
      } catch (t: Throwable) {
        log.e("error while updating price for cardPrint ${scryfallCard.name}: ${t.message}")
      }
    }
}
