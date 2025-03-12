package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.ScryfallCardSet
import tornadofx.ItemViewModel

class CardSetModel(set: ScryfallCardSet): ItemViewModel<ScryfallCardSet>(set) {
	val code = bind(ScryfallCardSet::code)
	val name = bind(ScryfallCardSet::name)
	val releaseDate = bind(ScryfallCardSet::releaseDate)
	val officalCardCount = bind(ScryfallCardSet::cardCount)
	val digitalOnly = bind(ScryfallCardSet::digitalOnly)
	val icon = bind(ScryfallCardSet::icon)
}
