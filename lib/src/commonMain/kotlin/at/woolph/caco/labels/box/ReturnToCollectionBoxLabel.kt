/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer

class ReturnToCollectionBoxLabel(override val subtitle: String? = "Returning Destructed Decks") :
    OneSymbolBoxLabel {
  override val title = "To Be Filed"
  override val icon by lazySetIcon("ath", mythicBinderLabelIconRenderer)
}
