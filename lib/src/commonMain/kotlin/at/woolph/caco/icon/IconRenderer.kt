/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.icon

import arrow.core.Either
import at.woolph.caco.datamodel.sets.IScryfallCardSet
import at.woolph.caco.image.ImageCache
import at.woolph.utils.Uri
import kotlinx.coroutines.runBlocking

interface IconRenderer {
  suspend fun renderSvg(id: String, uri: Uri): Either<Throwable, ByteArray>
}

expect class IconRendererImpl(
  svgLoader: SvgLoader,
  iconResolution: Float = 256f,
) : IconRenderer

class CachingIconRenderer(
  val iconRenderer: IconRenderer,
  val cacheIdSuffix: String = "",
) : IconRenderer {
  override suspend fun renderSvg(id: String, uri: Uri): Either<Throwable, ByteArray> =
    ImageCache.getImageByteArray("$id$cacheIdSuffix") {
      iconRenderer.renderSvg(id, uri)
    }
}

val uiIconRenderer = CachingIconRenderer(IconRendererImpl(BasicSvgLoader), "")
val mythicBinderLabelIconRenderer = CachingIconRenderer(IconRendererImpl(MythicSvgLoader), "-mythic")
val rareBinderLabelIconRenderer = CachingIconRenderer(IconRendererImpl(RareSvgLoader), "-rare")
val uncommonBinderLabelIconRenderer = CachingIconRenderer(IconRendererImpl(UncommonSvgLoader), "-uncommon")
val commonBinderLabelIconRenderer = CachingIconRenderer(IconRendererImpl(CommonSvgLoader), "-common")

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


suspend fun IconRenderer.cachedImage(set: IScryfallCardSet): ByteArray? =
  set.icon?.let {
    renderSvg("set-icon-${set.code}", it)
      .onLeft { println("couldn't get icon for $set due to ${it.message}") } // TODO KMP logging
      .getOrNull()
  }

fun IScryfallCardSet?.lazySetIcon(iconRenderer: IconRenderer): Lazy<ByteArray?> =
  this?.let { lazy { runBlocking { iconRenderer.cachedImage(it) } } } ?: lazyOf(null)

val IScryfallCardSet?.lazyIconMythic: Lazy<ByteArray?>
  get() = lazySetIcon(mythicBinderLabelIconRenderer)

val IScryfallCardSet?.lazyIconUncommon: Lazy<ByteArray?>
  get() = lazySetIcon(uncommonBinderLabelIconRenderer)
