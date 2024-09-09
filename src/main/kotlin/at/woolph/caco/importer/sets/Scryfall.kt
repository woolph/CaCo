package at.woolph.caco.importer.sets

import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.httpclient.useHttpClient
import at.woolph.caco.newOrUpdate
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

private val LOG = LoggerFactory.getLogger("at.woolph.caco.importer.sets.Scryfall")

val patternPromoCollectorNumber = Regex("([s★c])$")

/**
 * pads the given collectors number while retaining non-number prefix and non-number suffix
 * for better results when alphabetic sorting of the collector numbers
 * e.g. "T3p" returns "T003p"
 * @param collectorNumber as gained by scryfall API
 * @return padded collector number
 */
fun paddingCollectorNumber(collectorNumber: String): String {
    val pattern = Regex("^([^\\d]*)(\\d*)([^\\d]*)$")
    val (prefix, number, suffix) = pattern.find(collectorNumber)?.let {
        Triple(it.groupValues[1].let { if(it == "TCH") "CH" else it }, it.groupValues[2].toInt(), it.groupValues[3])
    } ?: throw IllegalArgumentException("collectors number \"$collectorNumber\" does not match the regex $pattern")
	val numberPadding = min(max(3 - prefix.length, 1), 3)
    return String.format("%s%0${numberPadding}d%s", prefix, number, suffix)
}

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
    val sets = loadSetsFromScryfall().filter { it.set_type != "memorabilia" }.toList()

    sets.filter(ScryfallSet::isRootSet).forEach {
        CardSet.newOrUpdate(it.code) { it.update(this) }
    }
    sets.forEach {
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
