package at.woolph.libs.ktfx

import javafx.event.EventTarget
import javafx.scene.control.Spinner
import tornadofx.UIComponent
import tornadofx.add
import tornadofx.find
import kotlin.reflect.KClass

inline fun <reified T: UIComponent> EventTarget.view(block: T.() -> Unit = {}) =
	add(find(T::class).apply(block).root)

fun <T> Spinner<T>.commitValue() {
	if(isEditable) {
		valueFactory?.let { valueFactory ->
			valueFactory.value = valueFactory.converter?.fromString(editor.text)
		}
	}
}

