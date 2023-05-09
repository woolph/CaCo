package at.woolph.caco.importer.sets

import at.woolph.libs.json.useJsonReader
import at.woolph.libs.json.getJsonObjectArray
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.newOrUpdate
import at.woolph.libs.log.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.*
import javax.json.*
import kotlin.IllegalStateException
import kotlin.math.max
import kotlin.math.min

internal val LOG by logger("at.woolph.caco.importer.sets.Scryfall")

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
suspend fun importSet(setCode: String): CardSet = withContext(Dispatchers.IO) {
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonSerializer)
        }
    }.use {
        val response: HttpResponse = it.get("https://api.scryfall.com/sets/$setCode")
        LOG.info("importing set $setCode")

        if (!response.status.isSuccess())
            throw Exception("request failed with status code ${response.status.description}")

        val scryfallSet = response.body<ScryfallSet>()

        if (scryfallSet.isNonDigitalSetWithCards()) {
            CardSet.newOrUpdate(scryfallSet.code) { scryfallSet.update(this@newOrUpdate) }
        } else {
            throw Exception("result is not a set or it's digital or does not have any cards")
        }
    }
}

suspend fun CardSet.update() = withContext(Dispatchers.IO) {
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonSerializer)
        }
    }.use {
        LOG.debug("update set ${this@update}")
        val response: HttpResponse = it.get("https://api.scryfall.com/sets/${this@update.shortName}")

        if (!response.status.isSuccess())
            throw Exception("request failed with status code ${response.status.description}")

        val scryfallSet: ScryfallSet = response.body()

        scryfallSet.update(this@update)
    }
}

internal fun loadSetsFromScryfall(): Flow<ScryfallSet> =
    paginatedDataRequest<ScryfallSet>("https://api.scryfall.com/sets")
    .filter(ScryfallSet::isNonDigitalSetWithCards)

fun importSets(): Flow<CardSet> = flow {
    val sets = loadSetsFromScryfall().toList()

    sets.filter(ScryfallSet::isRootSet).forEach {
        CardSet.newOrUpdate(it.code) { it.update(this) }
    }
    sets.forEach {
        val setId = it.id
        val setFound = CardSet.findById(it.code)
            ?: it.parent_set_code?.let { CardSet.findById(it) }
            ?: throw NoSuchElementException("no card set found for code ${it.code} or ${it.parent_set_code}")

        ScryfallCardSet.newOrUpdate(setId) {
            setCode = it.code
            name = it.name
            set = setFound
        }
    }
    emitAll(CardSet.all().asFlow())
}
