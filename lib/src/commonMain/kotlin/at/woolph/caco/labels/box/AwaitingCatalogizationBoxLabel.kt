/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

class AwaitingCatalogizationBoxLabel(index: Int? = null, override val subtitle: String? = null) :
    OneSymbolBoxLabel {
  override val title = "Catalogize ${index ?: ""}"
  override val icon by lazySetIcon("wth", mythicBinderLabelIconRenderer)
}
