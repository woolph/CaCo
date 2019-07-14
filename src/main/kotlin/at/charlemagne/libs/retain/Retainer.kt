package at.charlemagne.libs.retain

import at.charlemagne.libs.log.logger
import java.util.*
import java.util.prefs.Preferences

inline fun <reified T> retainer(preventRestoring: Boolean = false) = lazy {
	Retainer(Preferences.userNodeForPackage(T::class.java), preventRestoring)
}

class Retainer(val userPrefs: Preferences, var preventRestoring: Boolean = false) {
	companion object {
		private val logger by logger()
	}
	private val retainValueAssignments = HashMap<String, String?>()

	init {
		logger.debug("Adding Retainer's Shutdown hook!")
		Runtime.getRuntime().addShutdownHook(Thread {
			logger.trace("Retainer's Shutdown hook active")
			storeRetain()
		})
	}

	fun storeRetain() {
		logger {
			synchronized(retainValueAssignments) {
				retainValueAssignments.forEach { propertyName, stringValue ->
					if(stringValue!=null) {
						logger.info { "userprefs setting $propertyName = $stringValue" }
						userPrefs.put(propertyName, stringValue)
					} else {
						logger.info { "userprefs deleting $propertyName" }
						userPrefs.remove(propertyName)
					}
				}
				retainValueAssignments.clear()
			}
		}
	}

	fun putValue(propertyName: String, value: String?) {
		synchronized(retainValueAssignments) {
			retainValueAssignments.put(propertyName, value)
		}
	}

	fun getValue(propertyName: String): String? {
		synchronized(retainValueAssignments) {
			return retainValueAssignments.get(propertyName) ?:
				if(!preventRestoring) userPrefs.get(propertyName, null) else null
		}
	}

	data class RetainPropertyKey(val retainer: Retainer, val propertyName: String) {
		fun putValue(value: String?) {
			retainer.putValue(propertyName, value)
		}

		fun getValue() = retainer.getValue(propertyName)

		operator fun get(subPropertyName: String)
				= RetainPropertyKey(retainer, "$propertyName.$subPropertyName")
	}

	operator fun get(propertyName: String) = RetainPropertyKey(this, propertyName)

/*
	class RetainDelegate<T>(val retainProperty: RetainProperty, val propertyName: String?,
							val converter: StringConverter<T>) {
		operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadWriteProperty<Any?, T> {
			val propName = propertyName ?: prop.name
			return Delegates.observable(converter.fromString(retainProperty.getValue(propName))) { _, old, new ->
				logger.debug { "retain property $propName has changed from $old to $new" }

				retainProperty.putValue(propName, new?.let(converter::toString))
			}
		}
	}
	fun <T> retainedProperty(propertyName: String? = null, converter: StringConverter<T>)
			= RetainDelegate<T>(this, propertyName, converter)

	fun retainedString(propertyName: String? = null)
			= retainedProperty(propertyName, getDefaultStringConverter<String?>(null))

	fun retainedBoolean(propertyName: String? = null, defaultValue: Boolean = false)
			= retainedProperty(propertyName, getDefaultStringConverter<Boolean>(defaultValue))

	fun retainedInt(propertyName: String? = null, defaultValue: Int = 0)
			= retainedProperty(propertyName, getDefaultStringConverter<Int>(defaultValue))

	fun retainedDouble(propertyName: String? = null, defaultValue: Double = 0.0)
			= retainedProperty(propertyName, getDefaultStringConverter<Double>(defaultValue))

	fun retainedPath(propertyName: String? = null, defaultValue: Path? = null)
			= retainedProperty(propertyName, getDefaultStringConverter<Path?>(defaultValue))
	*/
}
/*

fun <T> createStringConverter(fromString: (String?) -> T) = object: StringConverter<T>() {
	override fun toString(`object`: T) = `object`.toString()
	override fun fromString(string: String?) = fromString(string)
}

inline fun <reified T> getDefaultStringConverter(nullValue: T): StringConverter<T> = when(T::class) {
	String::class -> createStringConverter {
		(it) as T
	}
	Boolean::class -> createStringConverter {
		(it?.toBoolean() ?: nullValue) as T
	}
	Int::class -> createStringConverter {
		(it?.toInt() ?: nullValue) as T
	}
	Double::class -> createStringConverter {
		(it?.toDouble() ?: nullValue) as T
	}
	Path::class -> createStringConverter {
		(it?.let { Paths.get(it) } ?: nullValue) as T
	}
	else -> createStringConverter {
		if(it==null) nullValue
		throw IllegalArgumentException("no default string converter fromString available for ")
	}
}
*/
