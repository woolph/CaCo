package at.woolph.libs.ktfx

import javafx.beans.binding.Bindings
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableObjectValue
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import tornadofx.ChangeListener
import tornadofx.stringBinding
import java.util.concurrent.Callable


fun <S, T> ObservableObjectValue<S>.mapBinding(converter: (S)->T)
		= Bindings.createObjectBinding(Callable{ converter(this.value) }, this)

fun <S> ObservableObjectValue<S>.mapStringBinding(converter: (S)->String)
		= Bindings.createStringBinding(Callable{ converter(this.value) }, this)

fun <S> ObservableValue<S>.toStringBinding()
		= this.stringBinding { value?.toString() ?: "" }

fun <T, N> ObservableValue<T>.selectNullable(nested: (T) -> ObservableValue<N>?): Property<N> {
	fun extractNested(): ObservableValue<N>? = value?.let(nested)

	var currentNested: ObservableValue<N>? = extractNested()

	return object : SimpleObjectProperty<N>() {
		val changeListener = ChangeListener<Any?> { _, _, _ ->
			invalidated()
			fireValueChangedEvent()
		}

		init {
			currentNested?.addListener(changeListener)
			this@selectNullable.addListener(changeListener)
		}

		override fun invalidated() {
			currentNested?.removeListener(changeListener)
			currentNested = extractNested()
			currentNested?.addListener(changeListener)
		}

		override fun get() = currentNested?.value

		override fun set(v: N?) {
			(currentNested as? WritableValue<N>)?.value = v
			super.set(v)
		}
	}
}
