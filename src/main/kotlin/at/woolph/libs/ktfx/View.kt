package at.woolph.libs.ktfx

import javafx.event.EventTarget
import javafx.scene.control.Spinner
import tornadofx.UIComponent
import tornadofx.add
import tornadofx.find
import kotlin.reflect.KClass

fun <T: UIComponent> EventTarget.view(type: KClass<T>) {
	val t = find(type)
	this.add(t.root)
}

fun <T> Spinner<T>.commitValue() {
	if(isEditable) {
		valueFactory?.let { valueFactory ->
			valueFactory.value = valueFactory.converter?.fromString(editor.text)
		}
	}
}

