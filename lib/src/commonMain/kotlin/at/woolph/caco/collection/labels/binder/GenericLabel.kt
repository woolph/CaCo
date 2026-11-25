/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.binder

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

open class GenericLabel(
    override val code: String,
    override val title: String,
    override val subTitle: String? = null,
) : at.woolph.caco.collection.labels.binder.MapLabelItem {
  override val mainIcon by lazySetIcon("default", mythicBinderLabelIconRenderer)
}
