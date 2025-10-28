/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import at.woolph.caco.decks.Pageable
import at.woolph.utils.ktor.useHttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class PaginatedData<T : ScryfallBase>(
    @SerialName("object") val objectType: String,
    @SerialName("total_cards") override val totalItems: Int? = null,
    val has_more: Boolean,
    @Contextual val next_page: String? = null,
    val data: List<T>,
) : ScryfallBase, Pageable<T> {
  override fun isValid() = objectType == "list"

  override fun hasNext(): Boolean = has_more

  override fun next(): String = next_page!!

  override fun contents(): Flow<T> = data.asFlow()
}

private val LOG = LoggerFactory.getLogger("at.woolph.caco.masterdata.import.PaginatedData")

internal inline fun <reified T : ScryfallBase> paginatedDataRequest(
    initialQuery: String,
    optional: Boolean = false,
    progressIndicator: ProgressIndicator? = null,
): Flow<T> = flow {
  var currentQuery: String? = initialQuery

  useHttpClient { client ->
    while (currentQuery != null && currentCoroutineContext().isActive) {
      LOG.debug("importing paginated data from $currentQuery")
      val response: HttpResponse = client.get(currentQuery!!)

      if (response.status.isSuccess()) {
        val paginatedData = response.body<PaginatedData<T>>()

        currentQuery = if (paginatedData.has_more) paginatedData.next_page else null

        emitAll(
            paginatedData.data.asFlow().filter { it.isValid() }
            //                    .onEach { LOG.trace("emitting $it") }
        )
      } else {
        if (!optional)
            throw Exception("request failed with status code ${response.status.description}")
        else currentQuery = null
      }
    }
    progressIndicator?.finished()
  }
}
