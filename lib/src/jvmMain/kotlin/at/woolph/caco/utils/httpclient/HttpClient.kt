/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.utils.httpclient

import at.woolph.caco.masterdata.import.jsonSerializer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.withContext

suspend fun <R> useHttpClient(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend (HttpClient) -> R,
) =
    withContext(context) {
      HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonSerializer) }
            install(UserAgent) { agent = "CaCoApp/0.1" }
          }
          .use { block(it) }
    }
