/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.icon

import arrow.core.Either
import at.woolph.caco.image.ImageCacheImpl
import at.woolph.caco.lib.Uri
import kotlinx.coroutines.runBlocking

interface IconRenderer {
  suspend fun renderSvg(id: String, uri: Uri): Either<Throwable, ByteArray>
}

class CachingIconRenderer(
  val iconRenderer: IconRenderer,
  val cacheIdSuffix: String = "",
) : IconRenderer {
  override suspend fun renderSvg(id: String, uri: Uri): Either<Throwable, ByteArray> =
    ImageCacheImpl.getImageByteArray("$id$cacheIdSuffix") {
      iconRenderer.renderSvg(id, uri)
    }
}

class ModifyingIconRenderer(
  val iconRenderer: IconRenderer,
  val modificator: (ByteArray) -> ByteArray,
  ): IconRenderer {
  override suspend fun renderSvg(id: String, uri: Uri): Either<Throwable, ByteArray> =
    iconRenderer.renderSvg(id, uri).map(modificator)

  companion object {
    fun gradientModded(
      iconRenderer: IconRenderer,
      strokeColor: String,
      color1: String,
      color2: String,
    ) =
      ModifyingIconRenderer(
        iconRenderer = iconRenderer,
        modificator = {
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
            )
            .toByteArray()
        },
      )
  }
}

expect val BasicIconRenderer: IconRenderer

val uiIconRenderer = CachingIconRenderer(BasicIconRenderer, "")
val mythicBinderLabelIconRenderer =
  ModifyingIconRenderer.gradientModded(CachingIconRenderer(BasicIconRenderer, "-mythic"), "black", "#c54326", "#f7971c")
val rareBinderLabelIconRenderer =
  ModifyingIconRenderer.gradientModded(CachingIconRenderer(BasicIconRenderer, "-rare"), "black", "#8d7431", "#f6db94")
val uncommonBinderLabelIconRenderer =
  ModifyingIconRenderer.gradientModded(CachingIconRenderer(BasicIconRenderer, "-uncommon"), "black", "#626e77", "#c8e2f2")
val commonBinderLabelIconRenderer = CachingIconRenderer(BasicIconRenderer, "-common")

fun lazyIcon(
    cacheId: String,
    nullableUri: Uri?,
    iconRenderer: IconRenderer,
): Lazy<ByteArray?> =
    nullableUri?.let { uri -> lazy { runBlocking { iconRenderer.renderSvg(cacheId, uri).fold({null as ByteArray?},{it}) } } }
        ?: lazyOf(null)

fun lazySetIcon(
    setCode: String,
    iconRenderer: IconRenderer,
): Lazy<ByteArray?> =
    lazyIcon("set-code-$setCode", Uri("https://svgs.scryfall.io/sets/$setCode.svg"), iconRenderer)
