package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.DeckArchetype
import tornadofx.ItemViewModel

class DeckArchetypeModel(deck: DeckArchetype): ItemViewModel<DeckArchetype>(deck), DeckTreeModel {
	override val name = bind(DeckArchetype::name)
	override val format = bind(DeckArchetype::format)
	override val comment = bind(DeckArchetype::comment)
	override val archived = bind(DeckArchetype::archived)

	override fun toString() = "[${format.value}] ${name.value}"
}
