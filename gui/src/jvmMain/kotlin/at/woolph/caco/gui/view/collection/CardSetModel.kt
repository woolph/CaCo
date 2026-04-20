/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.icon.cachedImage
import at.woolph.caco.icon.uiIconRenderer
import java.io.ByteArrayInputStream
import javafx.scene.image.Image
import tornadofx.ItemViewModel

class CardSetModel(set: ScryfallCardSet) :
    ItemViewModel<ScryfallCardSet>(set), Comparable<CardSetModel> {
  val code = bind(ScryfallCardSet::code)
  val name = bind(ScryfallCardSet::name)
  val releaseDate = bind(ScryfallCardSet::releaseDate)
  val officalCardCount = bind(ScryfallCardSet::cardCount)
  val digitalOnly = bind(ScryfallCardSet::digitalOnly)

  override fun compareTo(other: CardSetModel): Int = item.compareTo(other.item)

  suspend fun getSetIconAsImage(): Image? =
      item?.let { uiIconRenderer.cachedImage(it) }?.let { Image(ByteArrayInputStream(it)) }
}
