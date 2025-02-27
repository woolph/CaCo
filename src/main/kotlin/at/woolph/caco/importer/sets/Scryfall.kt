package at.woolph.caco.importer.sets

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.httpclient.useHttpClient
import at.woolph.caco.utils.newOrUpdate
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.UUID

private val LOG = LoggerFactory.getLogger("at.woolph.caco.importer.sets.Scryfall")

// TODO import MDFC replacements (STX, KHM, ...)
// TODO import double sided tokens as they are printed (especially those of the commander precons)
suspend fun importSet(setCode: String): CardSet = useHttpClient(Dispatchers.IO) { client ->
    val response: HttpResponse = client.get("https://api.scryfall.com/sets/$setCode")
    LOG.info("importing set $setCode")

    if (!response.status.isSuccess())
        throw Exception("request GET https://api.scryfall.com/sets/$setCode with status code ${response.status.description}")

    val scryfallSet = response.body<ScryfallSet>()

    if (scryfallSet.isNonDigitalSetWithCards()) {
        CardSet.newOrUpdate(scryfallSet.code) { scryfallSet.update(this@newOrUpdate) }
    } else {
        throw Exception("result is not a set or it's digital or does not have any cards")
    }
}

suspend fun loadCard(cardId: String): ScryfallCard = useHttpClient(Dispatchers.IO) { client ->
    val response: HttpResponse = client.get("https://api.scryfall.com/cards/$cardId")

    if (!response.status.isSuccess())
        throw Exception("request GET https://api.scryfall.com/cards/$cardId with status code ${response.status.description}")

    return@useHttpClient response.body<ScryfallCard>()
}

suspend fun CardSet.update() = useHttpClient(Dispatchers.IO) { client ->
    LOG.debug("update set ${this@update}")
    val response: HttpResponse = client.get("https://api.scryfall.com/sets/${this@update.shortName}")

    if (!response.status.isSuccess())
        throw Exception("request failed with status code ${response.status.description}")

    val scryfallSet: ScryfallSet = response.body()

    scryfallSet.update(this@update)
}

internal fun loadSetsFromScryfall(): Flow<ScryfallSet> =
    paginatedDataRequest<ScryfallSet>("https://api.scryfall.com/sets")
    .filter(ScryfallSet::isNonDigitalSetWithCards)

fun importSets(): Flow<CardSet> = flow {
    val (importWorthySets, _) = loadSetsFromScryfall().toList().partition(ScryfallSet::isImportWorthy::get)

    importWorthySets.filter(ScryfallSet::isRootSet).forEach { scryfallSet ->
        CardSet.newOrUpdate(scryfallSet.code) { scryfallSet.update(this) }
    }
    importWorthySets.forEach {
        try {
            val setId = it.id
            val setFound = CardSet.findById(it.code)
                ?: it.parent_set_code?.let { CardSet.findById(it) }
                ?: throw NoSuchElementException("no card set found for code ${it.code} or ${it.parent_set_code}")

            ScryfallCardSet.newOrUpdate(setId) {
                setCode = it.code
                name = it.name
                set = setFound
            }
        } catch (t: Throwable) {
            LOG.error("error while importing set ${it.name}", t)
        }
    }

    // FIXME ask scryfall to add these oversized dungeon tokens to their database
    val oafrId = UUID.fromString("c954ce81-07b0-4881-b350-af3d7780ec22")
    ScryfallCardSet.newOrUpdate(oafrId) {
        setCode = "oafr"
        name = "Adventures in the Forgotten Realms Oversized"
        set = CardSet["afr"]
    }

    mapOf(
        "6f509dbe-6ec7-4438-ab36-e20be46c9922" to "20665182-5b20-4bb7-8638-4bea6bcfabb3", // Dungeon of the Mad Mage
        "59b11ff8-f118-4978-87dd-509dc0c8c932" to "3377d60a-586d-4e59-8f6c-4c27664c1f40", // Lost Mine of Phandelver
        "70b284bd-7a8f-4b60-8238-f746bdc5b236" to "3ccf204e-8431-457c-aa3e-d0e2703f5a32", // Tomb of Annihilation
    ).forEach { (nonOversizedVersionId, oversizedVersionId) ->
        val nonOversizedVersion = loadCard(nonOversizedVersionId)
        val id0 = UUID.fromString(oversizedVersionId)
        Card.newOrUpdate(id0) {
            nonOversizedVersion.copy(
                oversized = true,
                id = id0,
                set = "oafr",
                set_id = oafrId,
            ).update(this)
        }
    }

    emitAll(CardSet.all().asFlow())
}

suspend fun downloadBulkData(type: String, block: suspend (InputStream) -> Unit) {
    val bulkData = request<ScryfallBulkData>("https://api.scryfall.com/bulk-data/${type}")

    withContext(Dispatchers.IO) {
        bulkData.downloadUri.toURL().openConnection().getInputStream().use {
            block(it)
        }
    }
}
