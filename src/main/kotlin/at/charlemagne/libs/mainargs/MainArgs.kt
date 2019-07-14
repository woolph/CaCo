package at.charlemagne.libs.mainargs

import javafx.stage.Stage
import tornadofx.App
import tornadofx.NoPrimaryViewSpecified
import tornadofx.Stylesheet
import tornadofx.UIComponent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Argument<out Type>(val name: String, val index: Int, val mandatory: Boolean, val description: String, val transform: (String) -> Type) : ReadOnlyProperty<KtFxApp, Type> {
	abstract override operator fun getValue(thisRef: KtFxApp, property: KProperty<*>): Type

	override fun toString() = "parameter #$index \"$name\" ... $description"
}

class MandatoryArgument<out Type>(name: String, index: Int, description: String, transform: (String) -> Type) : Argument<Type>(name, index, true, description, transform) {
	init {
		if(index<0) throw IllegalArgumentException("index must not be negative")
	}
	override operator fun getValue(thisRef: KtFxApp, property: KProperty<*>): Type {
		return if(index<thisRef.argumentValues.size) transform(thisRef.argumentValues[index])
		else throw IllegalStateException("no main argument given for the mandatory parameter ${property.name}")
	}
}

class OptionalArgument<out Type>(name: String, index : Int, description: String, transform : (String) -> Type) : Argument<Type>(name, index, false, description, transform) {
	override operator fun getValue(thisRef: KtFxApp, property: KProperty<*>): Type {
		@Suppress("UNCHECKED_CAST")
		return if (index < thisRef.argumentValues.size) transform(thisRef.argumentValues[index])
		else  null as Type //else throw IllegalStateException("type for optional parameters must be nullable (type of \"${property.name}\" is ${}property::class.name})")
	}
}

open class Option(val name :String, val regex : Regex, val description : String, val handler: (MatchResult) -> Unit = {}) : ReadOnlyProperty<KtFxApp, Boolean> {
	var flag = false

	fun tryApplyingTo(arg: String) : Boolean {
		regex.find(arg)?.let {
			handler.invoke(it)
			flag = true
			return true
		}
		return false
	}

	override fun toString() = "option \"$name\" ... $description"

	override fun getValue(thisRef: KtFxApp, property: KProperty<*>) = flag
}


class OptionDelegator(val regex: Regex, val description: String, val handler: (MatchResult) -> Unit = {}) {
	operator fun provideDelegate(thisRef: KtFxApp, prop: KProperty<*>) =
			Option(prop.name, regex, description, handler).apply {
				thisRef.options.add(this)
			}
}

interface ArgumentDelegator<out T> {
	operator fun provideDelegate(thisRef: KtFxApp, prop: KProperty<*>): Argument<T>
}

class MandatoryArgumentDelegator<out T>(val index : Int?, val description: String, val transform : (String) -> T) : ArgumentDelegator<T> {
	override operator fun provideDelegate(thisRef: KtFxApp, prop: KProperty<*>) =
			MandatoryArgument(prop.name, index
					?: thisRef.arguments.size, description, transform).apply {
				thisRef.arguments.add(this)
			}
}

class OptionalArgumentDelegator<out T>(val index : Int?, val description: String, val transform : (String) -> T) : ArgumentDelegator<T> {
	override operator fun provideDelegate(thisRef: KtFxApp, prop: KProperty<*>) =
			OptionalArgument(prop.name, index
					?: thisRef.arguments.size, description, transform).apply {
				thisRef.arguments.add(this)
			}
}

open class KtFxApp(primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, stylesheet: KClass<out Stylesheet>) : App(primaryView, *arrayOf(stylesheet)) {
	val arguments = mutableListOf<Argument<*>>()
	val options = mutableListOf<Option>()
	val argumentValues = mutableListOf<String>()

	inline fun <reified T> argument(index: Int? = null, description : String = "", noinline transform : (String) -> T) : ArgumentDelegator<T> {
		return if(null is T) OptionalArgumentDelegator(index, description, transform)
		else MandatoryArgumentDelegator(index, description, transform)
	}

	fun option(regex: Regex, description : String, handler: (MatchResult) -> Unit = {}) = OptionDelegator(regex, description, handler)
	fun option(regex: Regex, handler: (MatchResult) -> Unit = {}) = option(regex, "", handler)

	fun printArgument() {
		// TODO enhance printArgument
		arguments.sortedBy { it.index }.forEach(::println)
		options.sortedBy { it.name }.forEach(::println)
	}

	fun process(args : Array<String>) {
		println("process parameters")
		argLoop@ for(arg in args) {
			println("process parameter $arg")
			for(option in options) {
				if(option.tryApplyingTo(arg)) {
					continue@argLoop
				}
			}
			// no option could be applied
			argumentValues.add(arg)
		}

		// fast fail if there are mandatory parameters not being set by main arguments
		arguments.firstOrNull { it.mandatory && it.index>=argumentValues.size }?.let {
			throw IllegalStateException("no main argument given for the mandatory parameter #${it.index} ${it.name}")
		}

		// TODO check if mandatory arguments are placed after an optional (which doesn't make any sense)
	}

	override fun start(stage : Stage) {
		println("starting app")
		process(parameters.raw.toTypedArray())
		super.start(stage)
	}
}