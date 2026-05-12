/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

class ArtSeriesLabel(index: Int? = null, override val subtitle: String? = null) :
    OneSymbolBoxLabel {
  override val title = "Art Series ${index ?: ""}"
  override val icon by lazySetIcon("pbook", mythicBinderLabelIconRenderer)
}
