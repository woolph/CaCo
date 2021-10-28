package at.woolph.caco.view.collection

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import tornadofx.*


class AddSetsDialog(val owner: View): Dialog<String?>() {
	val messages
		get() = owner.messages

	val setCodeProperty = SimpleStringProperty("")
	val setCode by setCodeProperty

	init {
		initOwner(owner.primaryStage)

		isResizable = true
		title = "Adding Sets"
		headerText = "Add one or more sets (separated by commas)"

		dialogPane.apply {
			content = BorderPane().apply {
				center {
					form {
						fieldset {
							field("CardSet Code") {
								textfield(setCodeProperty) {
									hgrow = Priority.ALWAYS
									runLater { requestFocus() }
								}
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
				ButtonType.APPLY -> setCode.toLowerCase()
				else -> null
			}
		}
	}
}
