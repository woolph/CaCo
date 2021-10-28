package at.woolph.libs.script.cmd

import at.woolph.libs.log.logger
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import javax.script.*

class CmdScriptEngine : AbstractScriptEngine {
	private var factory: CmdScriptEngineFactory

	/**
	 * package constructor for utility class
	 */
	internal constructor(factory: CmdScriptEngineFactory, n: Bindings) : super(n) {
		this.factory = factory
	}

	/**
	 * package constructor for utility class
	 */
	internal constructor(factory: CmdScriptEngineFactory) : super() {
		this.factory = factory
	}

	@Throws(ScriptException::class)
	override fun eval(script: String, context: ScriptContext): Any? {
		LOG.debug("executing script: {}", script)
		try {


			val bindingsReplacement = VARIABLE_PATTERN.findAll(script)
					.mapNotNull { it.groups[1]?.value }
					.distinct()
					.mapNotNull { variable -> context.getAttribute(variable)?.let {
							resolvedBinding -> Pair(variable, resolvedBinding.toString())
						}
					}
					.toMap(mutableMapOf())

			bindingsReplacement.putAll(System.getenv())

			val replacedScript = replaceVariables(script, bindingsReplacement)

			LOG.debug("executing script with bindings integrated: $replacedScript")

			val pb = ProcessBuilder("cmd", "/c", "\"$replacedScript\"")
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
			pb.redirectError(ProcessBuilder.Redirect.INHERIT) // TODO let it be rerouted
			val process = pb.start()
			try {
				return Integer.valueOf(process.waitFor())
			} catch (e: InterruptedException) {
				LOG.warn("script execution interrupted, process will be destroyed forcibly")
				process.destroyForcibly()
				throw ScriptException(e)
			}

		} catch (e: IOException) {
			LOG.throwing(e)
			throw ScriptException(e)
		}
	}

	@Throws(ScriptException::class)
	override fun eval(reader: Reader, context: ScriptContext): Any? {
		try {
			BufferedReader(reader).useLines {
				it.forEach { eval(it, context) }
			}
		} catch (e: IOException) {
			throw ScriptException(e)
		}

		return null
	}

	override fun createBindings(): Bindings {
		return SimpleBindings()
	}

	override fun getFactory(): ScriptEngineFactory {
		return factory
	}

	companion object {
		private val LOG by logger()

		private fun replaceVariables(input: String, map: Map<String, String>): String {
			var output = input
			for ((key, value) in map) {
				output = output.replace("%$key%", value)
			}
			return output
		}

		private val VARIABLE_PATTERN = Regex("%([a-zA-Z0-9_]{1,})%")
	}
}
