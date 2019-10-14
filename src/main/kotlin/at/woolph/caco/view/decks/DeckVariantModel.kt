package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.DeckVariant
import at.woolph.caco.datamodel.decks.Format
import tornadofx.ItemViewModel
import tornadofx.objectBinding
import tornadofx.select
import tornadofx.toProperty

class DeckVariantModel(variant: DeckVariant): ItemViewModel<DeckVariant>(variant), DeckTreeModel {
	val archetype = bind(DeckVariant::archetype).objectBinding { it?.let { DeckArchetypeModel(it) } }
	val originator = bind(DeckVariant::originator)
	val priority = bind(DeckVariant::priority)
	val link = bind(DeckVariant::link)
	val archetypeName = archetype.select { it?.name ?: "".toProperty() }

	override val name = bind(DeckVariant::name)
	override val format = archetype.select { it?.format ?: Format.Unknown.toProperty() }
	override val comment = bind(DeckVariant::comment)
	override val archived = bind(DeckVariant::archived)

	val parentVariant = bind(DeckVariant::parentVariant)

	override fun toString() = "[${format.value}] ${archetypeName.value} ${name.value}"
}
