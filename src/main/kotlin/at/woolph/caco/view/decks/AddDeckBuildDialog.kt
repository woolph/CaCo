package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.DeckArchetype
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.FX
import tornadofx.View
import tornadofx.cellFormat
import tornadofx.center
import tornadofx.combobox
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hgrow
import tornadofx.observable
import tornadofx.runLater
import tornadofx.textarea
import tornadofx.textfield

class AddDeckBuildDialog(val owner: View, initialArchetype: DeckArchetype? = null): Dialog<Triple<DeckArchetype, String, String>?>() {
	val messages get() = owner.messages
	val resources get() = owner.resources

	val name = SimpleStringProperty("")
	val comment = SimpleStringProperty("")

	val deckArchetype = SimpleObjectProperty<DeckArchetype?>()

	val deckArchetypes = FXCollections.observableArrayList<DeckArchetype>()

	fun updateDeckArchetypess() {
		deckArchetypes.setAll(transaction { DeckArchetype.all().toList().observable().sorted { t1, t2 ->
			t1.format.ordinal.compareTo(t2.format.ordinal)
		}})
	}

	init {
		initOwner(owner.primaryStage)

		updateDeckArchetypess()

		println(initialArchetype.toString())

		deckArchetype.set(deckArchetypes.find { it == initialArchetype })

		isResizable = true
		title = "Adding a Deck"
		headerText = "Add a Deck"

		dialogPane.apply {
			content = BorderPane().apply {
				center {
					form {
						fieldset {
							field("Archetype") {
								combobox(deckArchetype, deckArchetypes) {
									cellFormat(FX.defaultScope) {
										/*graphic = item?.format?.let {
											ImageView(resources.image("${it.name}.png")).apply {
												fitHeight = 24.0
												fitWidth = 24.0
											}
										}*/
										text = item?.let { "[${it.format.shortName}] ${it.name}" }
									}
								}
							}
							field("Name") {
								textfield(name) {
									hgrow = Priority.ALWAYS
									runLater { requestFocus() }
								}
							}
							field("Comment") {
								textarea(comment)
							}
						}
					}
				}
			}

			buttonTypes.setAll(ButtonType.APPLY, ButtonType.CANCEL)
		}

		setResultConverter { button ->
			// TODO data validation check
			when (button) {
				ButtonType.APPLY -> Triple(deckArchetype.value!!, name.value, comment.value)
				else -> null
			}
		}
	}
}
