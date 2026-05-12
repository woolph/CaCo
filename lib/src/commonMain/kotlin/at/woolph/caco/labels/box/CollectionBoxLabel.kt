/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import at.woolph.caco.labels.fetchCardSetsNullable

class CollectionBoxLabel(override val subtitle: String? = null, vararg codes: String?) :
    MultipleSymbolBoxLabel {
  private val sets = fetchCardSetsNullable(*codes)
  override val rows: Int = 3
  override val title: String = "COL"
  override val icons: List<ByteArray?> by lazy {
    sets.map { it.lazySetIcon(mythicBinderLabelIconRenderer).value }
  }
}
