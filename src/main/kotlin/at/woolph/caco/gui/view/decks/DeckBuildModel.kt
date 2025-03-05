package at.woolph.caco.gui.view.decks

import at.woolph.caco.datamodel.decks.Build
import at.woolph.caco.datamodel.decks.Format
import at.woolph.caco.datamodel.decks.Priority
import tornadofx.ItemViewModel
import tornadofx.objectBinding
import tornadofx.select
import tornadofx.toProperty

class DeckBuildModel(build: Build): ItemViewModel<Build>(build), DeckTreeModel {
	val archetype = bind(Build::archetype).objectBinding { it?.let { DeckArchetypeModel(it) } }
	val originator = archetype.select { it?.originator ?: "".toProperty() }
	val priority = archetype.select { it?.priority ?: Priority.MaybeCool.toProperty() }
	//val link = bind(Build::link)
	val archetypeName = archetype.select { it?.name ?: "".toProperty() }

	override val name = bind(Build::version)
	override val format = archetype.select { it?.format ?: Format.Unknown.toProperty() }
	override val comment = bind(Build::comment)
	override val archived = bind(Build::archived)

	val parentBuild = bind(Build::parentBuild)

	override fun toString() = "[${format.value}] ${archetypeName.value} (${name.value})"
}
