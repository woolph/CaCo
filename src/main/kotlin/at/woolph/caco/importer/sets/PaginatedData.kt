package at.woolph.caco.importer.sets

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedData<T: ScryfallBase>(
    @SerialName("object") val objectType: String,
    val total_cards: Int? = null,
    val has_more: Boolean,
    val next_page: String? = null,
    val data: List<T>,
): ScryfallBase {
    override fun isValid() = objectType == "list"
}

internal inline fun <reified T: ScryfallBase> paginatedDataRequest(initialQuery: String, optional: Boolean = false, progressIndicator: ProgressIndicator? = null): Flow<T> = flow {
    var currentQuery: String? = initialQuery

    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonSerializer)
        }
    }.use {
        while (currentQuery != null && currentCoroutineContext().isActive) {
            LOG.debug("importing paginated data from $currentQuery")
            val response: HttpResponse = it.get(currentQuery!!)

            if (response.status.isSuccess()) {
                val paginatedData = response.body<PaginatedData<T>>()

                currentQuery = if (paginatedData.has_more) paginatedData.next_page else null

                emitAll(
                    paginatedData.data.asFlow()
                        .updateProgressIndicator(
                            progressIndicator,
                            paginatedData.total_cards ?: paginatedData.data.size
                        )
                        .filter { it.isValid() }
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
