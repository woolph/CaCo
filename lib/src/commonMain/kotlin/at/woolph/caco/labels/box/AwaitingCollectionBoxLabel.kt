package at.woolph.caco.labels.box

import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import at.woolph.utils.toRoman

class AwaitingCollectionBoxLabel(index: Int, override val subtitle: String? = null) :
    OneSymbolBoxLabel {
  override val title = "To Be Filed ${index.toRoman()}"
  override val icon by lazySetIcon("ath", mythicBinderLabelIconRenderer)
}
