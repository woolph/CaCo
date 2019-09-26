package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.Deck
import tornadofx.ItemViewModel

class DeckModel(deck: Deck): ItemViewModel<Deck>(deck) {
	val name = bind(Deck::name)
	val format = bind(Deck::format)
	val comment = bind(Deck::comment)
	val archived = bind(Deck::archived)

	override fun toString() = "[${format.value}] ${name.value}"
}
