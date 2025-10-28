package at.woolph.caco.binderlabels

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

open class GenericLabel(
    override val code: String,
    override val title: String,
    override val subTitle: String? = null,
) : MapLabelItem {
  override val mainIcon by lazySetIcon("default", mythicBinderLabelIconRenderer)
}
