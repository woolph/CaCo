package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

object PromosBoxLabel : OneSymbolBoxLabel {
  override val title: String = "Promos"
  override val icon by lazySetIcon("star", mythicBinderLabelIconRenderer)
}
