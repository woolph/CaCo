package at.woolph.libs.ktfx

import javafx.event.EventTarget
import tornadofx.UIComponent
import tornadofx.add
import tornadofx.find
import kotlin.reflect.KClass

fun <T: UIComponent> EventTarget.view(type: KClass<T>) {
	val t = find(type)
	this.add(t.root)
}
