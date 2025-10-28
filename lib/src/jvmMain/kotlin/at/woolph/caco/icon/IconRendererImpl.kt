/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.icon

import arrow.core.Either
import arrow.core.raise.either
import at.woolph.utils.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual class IconRendererImpl actual constructor(
    val svgLoader: SvgLoader,
    val iconResolution: Float,
): IconRenderer {
  override suspend fun renderSvg(
    id: String,
    uri: Uri,
  ): Either<Throwable, ByteArray> = either {
    val byteArray = svgLoader.loadSvg(uri).bind().toByteArray()

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
