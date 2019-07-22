package at.woolph.libs.ktfx

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableObjectValue
import java.util.concurrent.Callable


fun <S, T> ObservableObjectValue<S>.mapBinding(converter: (S)->T)
		= Bindings.createObjectBinding(Callable{ converter(this.value) }, this)

fun <S> ObservableObjectValue<S>.mapStringBinding(converter: (S)->String)
		= Bindings.createStringBinding(Callable{ converter(this.value) }, this)
