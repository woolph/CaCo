package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

object CommanderStapelsBoxLabel : OneSymbolBoxLabel {
  override val title: String = "CMD Staples"
  override val icon by lazySetIcon("cmd", mythicBinderLabelIconRenderer)
}
