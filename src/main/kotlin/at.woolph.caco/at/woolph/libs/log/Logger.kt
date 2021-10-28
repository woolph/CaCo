package at.woolph.libs.log

import org.slf4j.LoggerFactory

class Logger(private val logger: org.slf4j.Logger) : org.slf4j.Logger by logger {
	fun debug(throwable: Throwable) {
		if(this.isDebugEnabled)
			this.debug(null as String?, throwable)
	}

	fun trace(throwable: Throwable) {
		if(this.isTraceEnabled)
			this.trace(null as String?, throwable)
	}

	fun info(throwable: Throwable) {
		if(this.isInfoEnabled)
			this.info(null as String?, throwable)
	}

	fun warn(throwable: Throwable) {
		if(this.isWarnEnabled)
			this.warn(null as String?, throwable)
	}

	fun error(throwable: Throwable) {
		if(this.isErrorEnabled)
			this.error(null as String?, throwable)
	}

	fun debug(throwable: Throwable, message: () -> String) {
		if(this.isDebugEnabled)
			this.debug(message.invoke(), throwable)
	}

	fun trace(throwable: Throwable, message: () -> String) {
		if(this.isTraceEnabled)
			this.trace(message.invoke(), throwable)
	}

	fun info(throwable: Throwable, message: () -> String) {
		if(this.isInfoEnabled)
			this.info(message.invoke(), throwable)
	}

	fun warn(throwable: Throwable, message: () -> String) {
		if(this.isWarnEnabled)
			this.warn(message.invoke(), throwable)
	}

	fun error(throwable: Throwable, message: () -> String) {
		if(this.isErrorEnabled)
			this.error(message.invoke(), throwable)
	}

	fun debug(message: () -> String) {
		if(this.isDebugEnabled)
			this.debug(message.invoke())
	}

	fun trace(message: () -> String) {
		if(this.isTraceEnabled)
			this.trace(message.invoke())
	}

	fun info(message: () -> String) {
		if(this.isInfoEnabled)
			this.info(message.invoke())
	}

	fun warn(message: () -> String) {
		if(this.isWarnEnabled)
			this.warn(message.invoke())
	}

	fun error(message: () -> String) {
		if(this.isErrorEnabled)
			this.error(message.invoke())
	}

	fun <T: Throwable> throwing(throwable: T): T {
		if(isDebugEnabled)
			debug("throwing exception", throwable)
		return throwable
	}

	/**
	 * executes the callable and logs it's entry and exit including it's result or exception with trace level
	 */
	operator fun <R> invoke(vararg args: Any?, name: String = Thread.currentThread().stackTrace[2].toString(), callable: () -> R): R {
		try {
			trace { args.joinToString(separator = ", ", prefix = "enter \"$name\" with arguments (", postfix = ")") { "'$it'" } }

			val result = callable.invoke()

			trace { "exit \"$name\" with result '$result'" }

			return result
		} catch(e: Exception) {
			trace("exit $name with exception:", e)
			throw e
		}
	}

//	fun activateDebugMode() {
//		val ctx = LogManager.getContext(false) as LoggerContext
//		val loggerConfig = ctx.configuration.getLoggerConfig(name)
//		loggerConfig.level = Level.ALL
//		ctx.updateLoggers()
//	}
}

fun <R : Any> R.logger(className: String = this::class.java.name): Lazy<Logger> {
	return lazy { Logger(LoggerFactory.getLogger(className.removeSuffix("\$Companion"))) }
}

fun logger(className: String): Lazy<Logger> {
	return lazy { Logger(LoggerFactory.getLogger(className.removeSuffix("\$Companion"))) }
}
