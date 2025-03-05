package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.CardSet
import tornadofx.ItemViewModel

class CardSetModel(set: CardSet): ItemViewModel<CardSet>(set) {
	val shortName = bind(CardSet::shortName)
	val name = bind(CardSet::name)
	val dateOfRelease = bind(CardSet::dateOfRelease)
	val officalCardCount = bind(CardSet::officalCardCount)
	val digitalOnly = bind(CardSet::digitalOnly)
	val icon = bind(CardSet::icon)
}
