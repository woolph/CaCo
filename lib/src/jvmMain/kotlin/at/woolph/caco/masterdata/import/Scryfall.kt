/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.SetType
import at.woolph.utils.exposed.newOrUpdate
import at.woolph.utils.ktor.request
import at.woolph.utils.ktor.useHttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

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

fun importSets(): Flow<ScryfallCardSet> = flow {
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
  val afr = ScryfallCardSet.findByCode("afr") ?: throw Exception("set afr not found")
  val oafrId = Uuid.parse("c954ce81-07b0-4881-b350-af3d7780ec22")
  ScryfallCardSet.newOrUpdate(oafrId) { scryfallCardSet ->
    scryfallCardSet.code = "oafr"
    scryfallCardSet.name = "Adventures in the Forgotten Realms Oversized"
    scryfallCardSet.parentSetCode = afr.code
    scryfallCardSet.cardCount = 3
    scryfallCardSet.digitalOnly = false
    scryfallCardSet.type = SetType.TOKEN
    scryfallCardSet.releaseDate = afr.releaseDate
  }

  mapOf(
          "6f509dbe-6ec7-4438-ab36-e20be46c9922" to
              "20665182-5b20-4bb7-8638-4bea6bcfabb3", // Dungeon of the Mad Mage
          "59b11ff8-f118-4978-87dd-509dc0c8c932" to
              "3377d60a-586d-4e59-8f6c-4c27664c1f40", // Lost Mine of Phandelver
          "70b284bd-7a8f-4b60-8238-f746bdc5b236" to
              "3ccf204e-8431-457c-aa3e-d0e2703f5a32", // Tomb of Annihilation
      )
      .forEach { (nonOversizedVersionId, oversizedVersionId) ->
        val nonOversizedVersion = loadCard(nonOversizedVersionId)
        val id0 = UUID.fromString(oversizedVersionId)
        Card.newOrUpdate(id0) { card ->
          nonOversizedVersion
              .copy(
                  oversized = true,
                  id = id0,
                  set = "oafr",
                  set_id = oafrId,
              )
              .update(card)
        }
      }

  // FIXME possessions of oversized dungeon cards are lost when importing to archidekt and
  // reimporting the archidekt export!!!!!
  // FIXME add oversized undercity dungeon
  // FIXME prerelease-stamped promo-stamped are lost when importing to archidekt and reimporting the
  // archidekt export!!!!! => add

  emitAll(ScryfallCardSet.all().asFlow())
}

suspend fun downloadBulkData(type: String, block: suspend (InputStream) -> Unit) {
  val bulkData = request<ScryfallBulkData>("https://api.scryfall.com/bulk-data/${type}")

  withContext(Dispatchers.IO) {
    bulkData.downloadUri.toURL().openConnection().getInputStream().use { block(it) }
  }
}
