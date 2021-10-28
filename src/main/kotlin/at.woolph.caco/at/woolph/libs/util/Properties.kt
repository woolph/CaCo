package at.woolph.libs.util

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
 */
class lazyVar<T: Any>(val lazyGet : () -> T): ReadWriteProperty<Any?, T> {
	lateinit var currentVal : T

	override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
		if(!::currentVal.isInitialized) {
			currentVal = lazyGet()
		}
		return currentVal
	}

	override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		currentVal = value
	}
}

/**
 *
 */
class RefreshableProperty<out T: Any>(val provider: () -> T): ReadOnlyProperty<Any?, T> {
	private var value by lazyVar { provider() }

	override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

	fun refresh() {
		value = provider()
	}
}
