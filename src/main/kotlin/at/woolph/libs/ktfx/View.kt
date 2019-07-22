package at.woolph.libs.ktfx

import javafx.event.EventTarget
import tornadofx.UIComponent
import tornadofx.add
import tornadofx.find
import kotlin.reflect.KClass
import javafx.scene.control.Spinner
import javafx.scene.image.Image
import java.net.URI

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

open class ImageCache {
	private val cache = mutableMapOf<URI, Image>()

	fun getImage(uri: URI, requestedWidth: Double, requestedHeight: Double, preserveRatio: Boolean = true, smooth: Boolean = true): Image {
		return cache[uri] ?:  Image(uri.toString(), requestedWidth, requestedHeight, preserveRatio, smooth).apply {
			cache[uri] = this
		}
	}
}
