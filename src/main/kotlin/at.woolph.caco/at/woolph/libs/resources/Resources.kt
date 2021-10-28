package at.woolph.libs.resources

/*
class FormattedResourceBundle @JvmOverloads constructor(protected val resourceBundle: ResourceBundle, protected val keyArgumentSplitter: String = ":", protected val argumentSplitter: String = ",", protected val keyExtractor: java.util.function.Function<String?, Optional<String>> = FormattedResourceBundle::extractKey) : ResourceBundle() {

	fun getStringOrKey(stringOrKey: String): String {
		LOG.trace("getStringIfKey for key '{}' with arguments", stringOrKey)
		return LOG.exit(keyExtractor.apply(stringOrKey)
				.map { key -> getString(key) }
				.orElseGet { stringOrKey })
	}

	fun getStringOrKey(stringOrKey: String, vararg args: Any): String {
		LOG.trace("getStringIfKey for key '{}' with arguments", stringOrKey)
		LOG.entry(args)
		return LOG.exit(String.format(getStringOrKey(stringOrKey), *args as Array<Any>))
	}

	fun getStringOrKeyWithArguments(stringOrKeyWithArguments: String): String {
		LOG.trace("enter getFormattedString for key '{}' with arguments", stringOrKeyWithArguments)
		val tokken = stringOrKeyWithArguments.split(keyArgumentSplitter.toRegex(), 2).toTypedArray()
		val stringOrKey = tokken[0]
		val args = if (tokken.size > 1) tokken[1].split(argumentSplitter.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() else arrayOfNulls<String>(0)

		return LOG.exit(getStringOrKey(stringOrKey, *args as Array<Any>))
	}


	fun getOptionalString(key: String): Optional<String> {
		try {
			return Optional.of(getString(key))
		} catch (e: MissingResourceException) {
			LOG.debug("resource missing", e)
			return Optional.empty()
		}

	}

	fun getOptionalStringOrKey(stringOrKey: String): Optional<String> {
		try {
			return Optional.of(getStringOrKey(stringOrKey))
		} catch (e: MissingResourceException) {
			LOG.debug("resource missing", e)
			return Optional.empty()
		}

	}

	fun getOptionalStringOrKey(stringOrKey: String, vararg args: Any): Optional<String> {
		try {
			return Optional.of(getStringOrKey(stringOrKey, *args))
		} catch (e: MissingResourceException) {
			LOG.debug("resource missing", e)
			return Optional.empty()
		}

	}

	fun getOptionalStringOrKeyWithArguments(stringOrKeyWithArguments: String): Optional<String> {
		try {
			return Optional.of(getStringOrKeyWithArguments(stringOrKeyWithArguments))
		} catch (e: MissingResourceException) {
			LOG.debug("resource missing", e)
			return Optional.empty()
		}

	}

	override fun handleGetObject(key: String): Any {
		return resourceBundle.getObject(key)
	}

	override fun getKeys(): Enumeration<String> {
		return resourceBundle.keys
	}

	companion object {
		private val LOG = XLoggerFactory.getXLogger(FormattedResourceBundle::class.java)

		/**
		 * default key extractor
		 * @param stringOrKey
		 * @return
		 */
		public fun extractKey(stringOrKey: String?): Optional<String> {
			return if (stringOrKey != null && stringOrKey.startsWith("%")) {
				Optional.of(stringOrKey.substring(1))
			} else Optional.empty()
		}
	}
}
		*/