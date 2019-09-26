package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.Format
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import tornadofx.*


class AddDeckDialog(val owner: View): Dialog<Triple<String, Format, String>?>() {
	val messages
		get() = owner.messages

	val name = SimpleStringProperty("")
	val format = SimpleObjectProperty(Format.Standard)
	val comment = SimpleStringProperty("")

	init {
		initOwner(owner.primaryStage)

		isResizable = true
		title = "Adding a Deck"
		headerText = "Add a Deck"

		dialogPane.apply {
			content = BorderPane().apply {
				center {
					form {
						fieldset {
							field("Name") {
								textfield(name) {
									hgrow = Priority.ALWAYS
									runLater { requestFocus() }
								}
							}
							field("Format") {
								combobox(format, Format.values().toList())
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
				ButtonType.APPLY -> Triple(name.value, format.value, comment.value)
				else -> null
			}
		}
	}
}
