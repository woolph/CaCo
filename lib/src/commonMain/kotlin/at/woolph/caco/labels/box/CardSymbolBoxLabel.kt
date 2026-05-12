/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

import at.woolph.caco.icon.commonBinderLabelIconRenderer
import at.woolph.caco.icon.lazyIcon
import at.woolph.utils.Uri

open class CardSymbolBoxLabel(override val title: String, symbolId: String) : OneSymbolBoxLabel {
  override val icon by
      lazyIcon(
          "card-symbol-$symbolId",
          Uri("https://svgs.scryfall.io/card-symbols/$symbolId.svg"),
          commonBinderLabelIconRenderer,
      )
}
