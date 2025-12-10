package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import at.woolph.caco.labels.fetchCardSetsNullable

open class SubtitledDuplicateBoxLabel(
    override val subtitle: String? = null,
    vararg codes: String?,
) : MultipleSymbolBoxLabel {
  private val sets = fetchCardSetsNullable(*codes)
  override val rows: Int =
      when (codes.size) {
        in 0..4 -> 1
        in 5..14 -> 2
        else -> 3
      }

  override val title: String = "DUP"
  override val icons: List<ByteArray> by lazy {
    sets.mapNotNull { it.lazySetIcon(mythicBinderLabelIconRenderer).value }
  }
}
