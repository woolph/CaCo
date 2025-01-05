package at.woolph.caco.importer.deck

import at.woolph.caco.httpclient.useHttpClient
import at.woolph.caco.importer.sets.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.ZonedDateTime

interface Pageable<T> {
    fun hasNext(): Boolean
    fun next(): String
    fun contents(): Flow<T>
    val totalItems: Int?
}

@Serializable
data class ArchidektPaginatedData<T>(
    @SerialName("count") override val totalItems: Int,
    @Contextual val next: String? = null,
    @Contextual val previous: String? = null,
    val results: List<T>,
): Pageable<T> {
    override fun hasNext() = next != null
    override fun next() = next!!
    override fun contents() = results.asFlow()
}

@Serializable
data class DeckTag(
    val id: Int,
    val tag: Int,
    val deck: Int,
    val name: String,
    val position: String,
)

@Serializable
data class Owner(
    val id: Int,
    val username: String,
    @Contextual val avatar: URI,
    val moderator: Boolean,
    val pledgeLevel: Int?,
    val roles: Set<String>,
)

@Serializable
data class Deck(
    val id: Int,
    val name: String,
    val owner: Owner,
    @Contextual val updatedAt: ZonedDateTime,
    val deckFormat: Int,
    val colors: Map<String, Int>,
    @Contextual val featured: URI,
    val customFeatured: String,
    val viewCount: Int,
    val private: Boolean,
    val cardPackage: String?,
    val tags: Set<DeckTag>,
    val unlisted: Boolean,
    val theorycrafted: Boolean,
    val game: Int?,
)

@Serializable
data class ArchidektDecklistEntryCardOracleCard(
    val name: String,
)

@Serializable
data class ArchidektDecklistEntryCard(
    val oracleCard: ArchidektDecklistEntryCardOracleCard,
)

@Serializable
data class ArchidektDecklistEntry(
    val quantity: Int,
    val categories: Set<String>,
    val card: ArchidektDecklistEntryCard,
)

@Serializable
data class ArchidektDecklist(
    val name: String,
    val cards: List<ArchidektDecklistEntry>,
)

private val LOG = LoggerFactory.getLogger("at.woolph.caco.importer.deck")

internal inline fun <reified P: Pageable<T>, reified T> paginatedDataRequest(initialQuery: String, optional: Boolean = false, progressIndicator: ProgressIndicator? = null): Flow<T> = flow {
    var currentQuery: String? = initialQuery

    useHttpClient { client ->
        while (currentQuery != null && currentCoroutineContext().isActive) {
            LOG.debug("importing paginated data from $currentQuery")
            val response: HttpResponse = client.get(currentQuery!!)

            if (response.status.isSuccess()) {
                val paginatedData = response.body<P>()

                currentQuery = if (paginatedData.hasNext()) paginatedData.next() else null

                emitAll(
                    paginatedData.contents()
                        .updateProgressIndicator(
                            progressIndicator,
                            paginatedData.totalItems
                        )
//                    .onEach { LOG.trace("emitting $it") }
                )
            } else {
                if (!optional)
                    throw Exception("request failed with status code ${response.status.description}")
                else
                    currentQuery = null
            }
        }
        progressIndicator?.finished()
    }
}

internal suspend inline fun <reified T> request(query: String): T = useHttpClient { client ->
    LOG.debug("requesting data from $query")
    val response: HttpResponse = client.get(query)
    if (response.status.isSuccess()) {
        return@useHttpClient response.body<T>()
    } else {
        throw Exception("request failed with status code ${response.status.description}")
    }
}
