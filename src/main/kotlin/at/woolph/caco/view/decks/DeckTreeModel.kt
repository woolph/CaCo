package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.Format
import javafx.beans.property.Property

interface DeckTreeModel {
	val name: Property<String>
	val format: Property<Format>
	val comment: Property<String>
	val archived: Property<Boolean>
}