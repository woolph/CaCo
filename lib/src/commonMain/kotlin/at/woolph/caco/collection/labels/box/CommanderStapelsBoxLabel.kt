/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

object CommanderStapelsBoxLabel : OneSymbolBoxLabel {
  override val title: String = "CMD Staples"
  override val icon by lazySetIcon("cmd", mythicBinderLabelIconRenderer)
}
