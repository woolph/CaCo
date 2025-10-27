/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.icon

import arrow.core.Either
import arrow.core.raise.either
import at.woolph.caco.datamodel.sets.IScryfallCardSet
import at.woolph.caco.lib.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.statement.readRawBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.slf4j.LoggerFactory

class IconRendererImpl(
    val iconResolution: Float = 256f,
): IconRenderer {
  override suspend fun renderSvg(
    id: String,
    uri: Uri,
  ): Either<Throwable, ByteArray> = either {
    val byteArray =
      withContext(Dispatchers.IO) {
        HttpClient(CIO).use { it.request(uri.toURL()).readRawBytes() }
      }

    withContext(Dispatchers.Default) {
      val transcoder =
        PNGTranscoder().apply {
          addTranscodingHint(ImageTranscoder.KEY_WIDTH, iconResolution)
          addTranscodingHint(ImageTranscoder.KEY_HEIGHT, iconResolution)
        }

      return@withContext ByteArrayOutputStream()
        .also {
          it.use {
            try {
              transcoder.transcode(
                TranscoderInput(ByteArrayInputStream(byteArray)),
                TranscoderOutput(it),
              )
            } catch (e: Exception) {
              raise(Exception("couldn't transcode image $uri", e))
            }
          }
        }
        .toByteArray()
    }
  }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java.declaringClass)
  }
}

suspend fun IconRenderer.cachedImage(set: IScryfallCardSet): ByteArray? =
    set.icon?.let {
      renderSvg("set-icon-${set.code}", it)
        .onLeft { IconRendererImpl.log.error("couldn't get icon for $set", it) }
        .getOrNull()
    }

fun IScryfallCardSet?.lazySetIcon(iconRenderer: IconRenderer): Lazy<ByteArray?> =
    this?.let { lazy { runBlocking { iconRenderer.cachedImage(it) } } } ?: lazyOf(null)

val IScryfallCardSet?.lazyIconMythic: Lazy<ByteArray?>
  get() = lazySetIcon(mythicBinderLabelIconRenderer)

val IScryfallCardSet?.lazyIconUncommon: Lazy<ByteArray?>
  get() = lazySetIcon(uncommonBinderLabelIconRenderer)
