/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.icon

import arrow.core.Either
import arrow.core.raise.either
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.masterdata.imagecache.ImageCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI

class IconRenderer(
    val iconResolution: Float = 256f,
    val cacheIdSuffix: String = "",
    val svgMod: (ByteArray) -> ByteArray = { it },
) {
    suspend fun renderSvg(uri: URI): Either<Exception, ByteArray> =
        either {
            val byteArray =
                withContext(Dispatchers.IO) {
                    HttpClient(CIO).use {
                        it.request(uri.toURL()).readRawBytes()
                    }
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
                                transcoder.transcode(TranscoderInput(ByteArrayInputStream(svgMod(byteArray))), TranscoderOutput(it))
                            } catch (e: Exception) {
                                raise(Exception("couldn't transcode image $uri", e))
                            }
                        }
                    }.toByteArray()
            }
        }

    companion object {
        val log = LoggerFactory.getLogger(this::class.java.declaringClass)

        fun gradientModded(
            cacheIdSuffix: String,
            strokeColor: String,
            color1: String,
            color2: String,
        ) = IconRenderer(cacheIdSuffix = cacheIdSuffix, svgMod = {
            String(it)
                .replace(
                    "<path ",
                    "<defs>\n" +
                        "    <linearGradient id=\"uncommon-gradient\" x2=\"0.35\" y2=\"1\">\n" +
                        "        <stop offset=\"0%\" stop-color=\"$color1\" />\n" +
                        "        <stop offset=\"50%\" stop-color=\"$color2\" />\n" +
                        "        <stop offset=\"100%\" stop-color=\"$color1\" />\n" +
                        "      </linearGradient>\n" +
                        "  </defs>\n" +
                        "<path stroke=\"$strokeColor\" stroke-width=\"2%\" style=\"fill:url(#uncommon-gradient)\" ",
                ).toByteArray()
        })
    }
}

val uiIconRenderer = IconRenderer(iconResolution = 24f)
val mythicBinderLabelIconRenderer = IconRenderer.gradientModded("-mythic", "black", "#c54326", "#f7971c")
val rareBinderLabelIconRenderer = IconRenderer.gradientModded("-rare", "black", "#8d7431", "#f6db94")
val uncommonBinderLabelIconRenderer = IconRenderer.gradientModded("-uncommon", "black", "#626e77", "#c8e2f2")
val commonBinderLabelIconRenderer = IconRenderer(cacheIdSuffix = "-common")

suspend fun IconRenderer.cachedImage(
    cacheId: String,
    uri: URI,
): ByteArray? =
    ImageCache.getImageByteArray("$cacheId$cacheIdSuffix") {
        renderSvg(uri)
            .onLeft { IconRenderer.log.error("couldn't get icon for $uri", it) }
            .getOrNull()
    }

fun lazyIcon(
    cacheId: String,
    nullableUri: URI?,
    iconRenderer: IconRenderer,
): Lazy<ByteArray?> = nullableUri?.let { uri -> lazy { runBlocking { iconRenderer.cachedImage(cacheId, uri) } } } ?: lazyOf(null)

fun lazySetIcon(
    setCode: String,
    iconRenderer: IconRenderer,
): Lazy<ByteArray?> = lazyIcon("set-code-$setCode", URI("https://svgs.scryfall.io/sets/$setCode.svg"), iconRenderer)

suspend fun IconRenderer.cachedImage(set: ScryfallCardSet): ByteArray? =
    set.icon?.let {
        ImageCache.getImageByteArray("set-icon-${set.code}$cacheIdSuffix") {
            renderSvg(it)
                .onLeft { IconRenderer.log.error("couldn't get icon for $set", it) }
                .getOrNull()
        }
    }

fun ScryfallCardSet?.lazySetIcon(iconRenderer: IconRenderer): Lazy<ByteArray?> =
    this?.let { lazy { runBlocking { iconRenderer.cachedImage(it) } } } ?: lazyOf(null)

val ScryfallCardSet?.lazyIconMythic: Lazy<ByteArray?> get() = lazySetIcon(mythicBinderLabelIconRenderer)
val ScryfallCardSet?.lazyIconUncommon: Lazy<ByteArray?> get() = lazySetIcon(uncommonBinderLabelIconRenderer)
