package at.charlemagne.libs.script.cmd

import java.util.*
import java.util.stream.Collectors
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory


/**
 * private constructor for utility class
 */
class CmdScriptEngineFactory : ScriptEngineFactory {

	override fun getEngineName(): String {
		return ENGINE
	}

	override fun getEngineVersion(): String {
		return ENGINE_VERSION
	}

	override fun getExtensions(): List<String> {
		return EXTENSIONS
	}

	override fun getMimeTypes(): List<String> {
		return MIME_TYPES
	}

	override fun getNames(): List<String> {
		return NAMES
	}

	override fun getLanguageName(): String {
		return LANGUAGE
	}

	override fun getLanguageVersion(): String {
		return LANGUAGE_VERSION
	}

	override fun getParameter(key: String): Any? {
		when (key) {
			"ScriptEngine.ENGINE" -> return ENGINE
			"ScriptEngine.ENGINE_VERSION" -> return ENGINE_VERSION
			"ScriptEngine.LANGUAGE" -> return LANGUAGE
			"ScriptEngine.LANGUAGE_VERSION" -> return LANGUAGE_VERSION
			"ScriptEngine.NAME" -> return NAMES[0]
			"ScriptEngine.THREADING" -> return "STATELESS"
			else -> return null
		}
	}

	override fun getMethodCallSyntax(obj: String, m: String, vararg args: String): String?
			= null // TODO method call not supported?!

	override fun getOutputStatement(toDisplay: String): String
			= "@echo \"$toDisplay\""

	override fun getProgram(vararg statements: String): String
			= statements.joinToString("") { "@$it\n" }

	override fun getScriptEngine(): ScriptEngine {
		return CmdScriptEngine(this)
	}

	companion object {
		private val ENGINE = "Command Line Batch Script"
		private val ENGINE_VERSION = "0.1.0"
		private val LANGUAGE = "Command Line Batch Script"
		private val LANGUAGE_VERSION = "0.1.0"
		private val MIME_TYPES = Arrays.asList("application/bat", "application/x-bat", "application/x-msdos-program", "application/textedit", "application/octet-stream")
		private val NAMES = Arrays.asList("Batch Script", "batch")
		private val EXTENSIONS = Arrays.asList("cmd", "bat")
	}
}
