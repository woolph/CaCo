package at.woolph.caco.labels.binder

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

object PromosLabel : MapLabelItem {
  override val code: String = "PRM"
  override val title: String = "Promos & Specials"
  override val mainIcon by lazySetIcon("star", mythicBinderLabelIconRenderer)
}
