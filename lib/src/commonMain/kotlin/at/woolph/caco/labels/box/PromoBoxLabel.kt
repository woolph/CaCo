package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

object PromoBoxLabel : MultipleSymbolBoxLabel {
  override val rows: Int = 1
  override val title: String = "DUP"
  override val icons: List<ByteArray> by lazy {
    listOfNotNull(lazySetIcon("star", mythicBinderLabelIconRenderer).value)
  }
}
