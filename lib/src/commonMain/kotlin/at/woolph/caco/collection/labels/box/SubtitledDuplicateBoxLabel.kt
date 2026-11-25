/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import at.woolph.caco.collection.labels.fetchCardSetsNullable

open class SubtitledDuplicateBoxLabel(
    override val subtitle: String? = null,
    vararg codes: String?,
) : MultipleSymbolBoxLabel {
  private val sets = fetchCardSetsNullable(*codes)
  override val rows: Int =
      when (codes.size) {
        in 0.. MAX_SETS_FOR_ONE_ROW -> 1
        in (MAX_SETS_FOR_ONE_ROW+1)..MAX_SETS_FOR_TWO_ROWS -> 2
        else -> 3
      }

  override val title: String = "DUP"
  override val icons: List<ByteArray> by lazy {
    sets.mapNotNull { it.lazySetIcon(mythicBinderLabelIconRenderer).value }
  }

  companion object {
    const val MAX_SETS_FOR_ONE_ROW = 4
    const val MAX_SETS_FOR_TWO_ROWS = 15
  }
}
