/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

open class GenericBoxLabel(override val title: String) : OneSymbolBoxLabel {
  override val icon by lazySetIcon("default", mythicBinderLabelIconRenderer)
}
