package at.woolph.caco.view.collection

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.layout.Priority
import tornadofx.View
import tornadofx.borderpane
import tornadofx.center
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.getValue
import tornadofx.hgrow
import tornadofx.runLater
import tornadofx.textfield
import java.util.*

fun <R> Dialog<R>.dialogPane(block: DialogPane.() -> Unit) = dialogPane.apply(block)

fun <R> Dialog<R>.buttons(block: DialogButtonsBuilder<R>.() -> Unit) =
	DialogButtonsBuilder(this).apply(block).apply()

class DialogButtonsBuilder<R>(val dialog: Dialog<R>) {
	protected var defaultConverter: () -> R = { throw IllegalStateException("no default action defined") }
	fun defaultAction(block: () -> R) {
		defaultConverter = block
	}
	inner class DialogButtonBuilder(val buttonType: ButtonType) {
		private var converter: ((ButtonType) -> R)? = null

		fun action(block: (ButtonType) -> R) {
			converter = block
		}

		internal fun doAction() =
			converter?.let { it(buttonType) } ?: defaultConverter()
	}
	private val buttonBuilers = mutableListOf<DialogButtonBuilder>()

	fun button(buttonType: ButtonType, builder: DialogButtonBuilder.() -> Unit = {}) {
		buttonBuilers += DialogButtonBuilder(buttonType).apply(builder)
	}

	fun button(buttonText: String, builder: DialogButtonBuilder.() -> Unit = {}) =
		button(ButtonType(buttonText), builder)

	internal fun apply() {
		dialog.dialogPane.buttonTypes.setAll(buttonBuilers.map { it.buttonType })
		dialog.setResultConverter { buttonType ->
			buttonBuilers.firstOrNull { it.buttonType === buttonType }?.doAction()
		}
	}
}

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

		dialogPane {
			content = borderpane {
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
		}

		buttons {
			button(ButtonType.APPLY) {
				action {
					setCode.lowercase(Locale.getDefault())
				}
			}
			button(ButtonType.CANCEL)
			defaultAction { null }
		}
	}
}
