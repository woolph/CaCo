/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.ktor

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

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

val jsonSerializer = Json {
  decodeEnumsCaseInsensitive = true
  ignoreUnknownKeys = true
  serializersModule = SerializersModule {
    contextual(LocalDate::class, LocalDateSerializer)
    contextual(URI::class, URISerializer)
    contextual(UUID::class, UUIDSerializer)
    contextual(ZonedDateTime::class, ZonedDateTimeSerializer)
  }
}

object LocalDateSerializer : KSerializer<LocalDate> {
  override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): LocalDate {
    return LocalDate.parse(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: LocalDate) {
    encoder.encodeString(value.toString())
  }
}

object URISerializer : KSerializer<URI> {
  override val descriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): URI =
    try {
      URI.create(decoder.decodeString())
    } catch (_: Throwable) {
      URI.create("about://version")
    }

  override fun serialize(encoder: Encoder, value: URI) {
    encoder.encodeString(value.toString())
  }
}

object UUIDSerializer : KSerializer<UUID> {
  override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): UUID {
    return UUID.fromString(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: UUID) {
    encoder.encodeString(value.toString())
  }
}

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
  override val descriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): ZonedDateTime {
    return ZonedDateTime.parse(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: ZonedDateTime) {
    encoder.encodeString(value.toString())
  }
}

suspend inline fun <reified T> request(query: String): T = useHttpClient { client ->
  val response: HttpResponse = client.get(query)
  if (response.status.isSuccess()) {
    return@useHttpClient response.body<T>()
  } else {
    throw Exception("request failed with status code ${response.status.description}")
  }
}
