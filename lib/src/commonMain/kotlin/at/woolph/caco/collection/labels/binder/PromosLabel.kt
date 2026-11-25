/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.binder

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

object PromosLabel : at.woolph.caco.collection.labels.binder.MapLabelItem {
  override val code: String = "PRM"
  override val title: String = "Promos & Specials"
  override val mainIcon by lazySetIcon("star", mythicBinderLabelIconRenderer)
}
