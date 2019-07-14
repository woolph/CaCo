package at.woolph.libs.files.config

import at.woolph.libs.files.inputStream
import at.charlemagne.libs.log.logger
import kotlin.properties.ReadOnlyProperty
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


open class ConfigurationFile {
	private val properties = Properties()

	constructor(inputStream: InputStream?) {
		try {
			LOG.trace("loading configuration")
			inputStream?.use(properties::load)
		} catch (e: Exception) {
			LOG.warn(e) { "Loading properties failed due to an exception" }
		}
	}

	constructor(path: Path): this(
			try {
				path.inputStream()
			} catch (e: NoSuchFileException) {
				LOG.warn { "Loading properties from file '$path' failed, because file does not existing" }
				null
			} catch (e: Exception) {
				LOG.warn(e) { "Loading properties from file '$path' failed due to an exception" }
				null
			})

	constructor(file: File): this(
			try {
				file.inputStream()
			} catch (e: NoSuchFileException) {
				LOG.warn { "Loading properties from file '$file' failed, because file does not existing" }
				null
			} catch (e: Exception) {
				LOG.warn(e) { "Loading properties from file '$file' failed due to an exception" }
				null
			})

	fun <Type> get(propertyName: String, convert: (String) -> Type): Type? {
		return if (properties.containsKey(propertyName)) {
			convert.invoke(properties.getProperty(propertyName))
		} else null
	}

	companion object {
		// TODO add mechanism to force the application to ignore the configuration (load app with default settings)
		private val LOG by logger()
	}

	class ConfigurationDelegateVar<Type>(val configurationFile : ConfigurationFile, val default: Type?,
                                         val transform : (String) -> Type) : ReadWriteProperty<Any, Type>  {
		private var actValue: Type? = null

		override operator fun getValue(thisRef: Any, property: KProperty<*>): Type {
			if(actValue==null) {
				actValue = configurationFile.get(property.name, transform) ?: (default ?: throw Exception("missing mandatory configuration ${property.name}"))
			}
			return actValue!!
		}

		override fun setValue(thisRef: Any, property: KProperty<*>, value: Type) {
			actValue = value
		}
	}

	class ConfigurationDelegateVal<Type>(val configurationFile : ConfigurationFile, val default: Type?,
                                         val transform : (String) -> Type) : ReadOnlyProperty<Any, Type>  {
		private var actValue: Type? = null

		override operator fun getValue(thisRef: Any, property: KProperty<*>): Type {
			if(actValue==null) {
				actValue = configurationFile.get(property.name, transform) ?: (default ?: throw Exception("missing mandatory configuration ${property.name}"))
			}
			return actValue!!
		}
	}

	fun <R> config(default: R? = null, op: (String) -> R) = ConfigurationDelegateVar<R>(this, default, op)
	fun config(default: Boolean? = null) = ConfigurationDelegateVar(this, default, String::toBoolean)
	fun config(default: Double? = null) = ConfigurationDelegateVar(this, default, String::toDouble)
	fun config(default: Int? = null) = ConfigurationDelegateVar(this, default, String::toInt)
	fun config(default: String? = null) = ConfigurationDelegateVar(this, default, String::toString)
	fun config(default: Path? = null) = ConfigurationDelegateVar(this, default) { Paths.get(it)!! }
}
