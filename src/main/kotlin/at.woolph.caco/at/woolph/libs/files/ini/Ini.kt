package at.woolph.libs.files.ini

import at.woolph.libs.files.inputStream
import at.woolph.libs.log.logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.nio.file.Path

fun Path.parseIni() = IniFile.parse(this)
fun File.parseIni() = IniFile.parse(this)
fun InputStream.parseIni() = IniFile.parse(this)

fun Path.parseIni(commentFilter: (String) -> Boolean) = IniFile.parse(this, commentFilter)
fun File.parseIni(commentFilter: (String) -> Boolean) = IniFile.parse(this, commentFilter)
fun InputStream.parseIni(commentFilter: (String) -> Boolean) = IniFile.parse(this, commentFilter)

object IniFile {
	private val LOG by logger()
	private val DEFAULT_COMMENT_FILTER = { s: String -> s.startsWith(";") }

	fun parse(path: Path, commentFilter: (String) -> Boolean = DEFAULT_COMMENT_FILTER): List<IniSection> {
		try {
			return parse(path.inputStream(), commentFilter)
		} catch (e: IOException) {
			throw LOG.throwing(UncheckedIOException("can't open ini", e))
		}
	}

	fun parse(file: File, commentFilter: (String) -> Boolean = DEFAULT_COMMENT_FILTER): List<IniSection> {
		try {
			return parse(file.inputStream(), commentFilter)
		} catch (e: IOException) {
			throw LOG.throwing(UncheckedIOException("can't open ini", e))
		}
	}

	fun parse(inputStream: InputStream, commentFilter: (String) -> Boolean = DEFAULT_COMMENT_FILTER): List<IniSection> {
		try {
			inputStream.bufferedReader().use { br ->
				val result = mutableListOf<IniSection>()
				var sectionBuilder = IniSection.Builder()

				br.lineSequence().map(String::trim).filterNot(commentFilter).filterNot(String::isEmpty).forEach { line ->
					if (line.startsWith("[")) { // section
						result.add(sectionBuilder.build())
						sectionBuilder = IniSection.Builder(line.substring(1, line.length - 1))
					} else { // key value pair
						val token = line.split("=".toRegex(), 2)
						sectionBuilder += (token[0] to token[1])
					}
				}
				result.add(sectionBuilder.build())
				return result
			}
		} catch (e: IOException) {
			throw LOG.throwing(UncheckedIOException("can't parse ini", e))
		}
	}
}

// TODO immutable with builder or mutable without builder
class IniSection(val name: String?, val attributes: Map<String, String> = mapOf()) {

	val isDefaultSection: Boolean
		get() = name == null

	override fun toString(): String = attributes.map { e -> "${e.key}=${e.value}" }.joinToString(
			separator = System.lineSeparator(),
			prefix = if(name == null) "" else "[$name]${System.lineSeparator()}")

	// TODO still needed?!
	class Builder(val name: String? = null) {
		protected val attributes = mutableMapOf<String, String>()
		protected var built = false

		operator fun plusAssign(attribute: Pair<String, String>) {
			if (built) throw UnsupportedOperationException("IniSection already built")
			attributes[attribute.first] = attribute.second
		}

		fun build(): IniSection {
			if (built) throw UnsupportedOperationException("IniSection already built")
			built = true
			return IniSection(name, attributes)
		}
	}
}

val Collection<IniSection>.name
	inline get() = this.map(IniSection::name) // TODO check if inline property is needed or not (if iterable is mutable, it must be inline)

val Collection<IniSection>.attributes
	inline get() = this.flatMap { it.attributes.entries } // TODO does it work?! // TODO check if inline property is needed or not (if iterable is mutable, it must be inline)

operator fun Collection<IniSection>.get(name: String) = this.filterBySectionName(name)
operator fun Collection<IniSection>.get(p: Pair<String, String>) = this.filterByAttribute(p.first, p.second)

operator fun Collection<Map.Entry<String, String>>.get(name: String) = this.firstOrNull { it.key == name }?.value

fun Collection<IniSection>.filterByAttribute(name: String, valueFilter: (String) -> Boolean)
		= this.filter { section: IniSection -> section.attributes[name]?.let { valueFilter.invoke(it) } ?: false }

fun Collection<IniSection>.filterByAttribute(name: String, value: String)
		= this.filterByAttribute(name) { string -> string == value }

fun Collection<IniSection>.filterBySectionName(nameFilter: (String) -> Boolean)
		= this.filter { section: IniSection -> section.name?.let { nameFilter.invoke(it) } ?: false }

fun Collection<IniSection>.filterBySectionName(name: String)
		= this.filterBySectionName { it == name }

fun Collection<IniSection>.filterDefaultSection()
		= this.filter {iniSection: IniSection -> iniSection.isDefaultSection }