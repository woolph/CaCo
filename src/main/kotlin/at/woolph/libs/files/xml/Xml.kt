package at.woolph.libs.files.xml

import at.charlemagne.libs.log.logger
import org.w3c.dom.Element
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

fun Path.parseXml() = XmlDocument.parse(this)
fun File.parseXml() = XmlDocument.parse(this)
fun InputStream.parseXml() = XmlDocument.parse(this)
fun String.parseXml() = XmlDocument.parse(ByteArrayInputStream(this.toByteArray()))

object XmlDocument {
	val LOG by logger()

	fun parse(file: File) : List<XmlNode> {
		try {
			return parse(file.inputStream())
		} catch (e : Exception) {
			throw IOException("unable to parse XML file $file", e)
		}
	}

	fun parse(path: Path) = parse(path.toFile())

	fun parse(inputStream: InputStream) : List<XmlNode> {
		inputStream.use {
			try {
				val dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				val doc = dBuilder.parse(it)
				val root = doc.documentElement

				root.normalize()

				return listOf(convertToXmlNode(root, null))
			} catch (e: Exception) {
				throw LOG.throwing(IOException("can't parse XML file", e))
			}
		}
	}

	private fun convertToXmlNode(domElement : Element, parent : XmlNode?) : XmlNode {
		val attributes = mutableMapOf<String, String>()
		val children = mutableListOf<XmlNode>()

		val xmlNode = XmlNode(parent, domElement.nodeName, attributes, children, domElement.textContent)
		for(node in NodeListIterable(domElement.childNodes)) {
			when(node.nodeType) {
				Node.ELEMENT_NODE -> children.add(convertToXmlNode(node as Element, xmlNode))
			}
		}

		for(node in NamedNodeMapIterable(domElement.attributes)) {
			attributes[node.nodeName] = node.nodeValue
		}

		return xmlNode
	}
}


private class NodeListIterable(private val nl : NodeList) : Iterable<Node> {
	override fun iterator() = object : Iterator<Node> {
		private var i = 0
		override fun hasNext() = (i)<nl.length
		override fun next() = nl.item(i++)
	}
}

private class NamedNodeMapIterable(private val nl : NamedNodeMap) : Iterable<Node> {
	override fun iterator() = object : Iterator<Node> {
		private var i = 0
		override fun hasNext() = (i)<nl.length
		override fun next() = nl.item(i++)
	}
}

class XmlNode internal constructor (val parent: XmlNode? = null, val name: String, val attributes: kotlin.collections.Map<String, String> = mutableMapOf(), val children: kotlin.collections.List<XmlNode> = mutableListOf(), val content: String? = null) {
	override fun toString() = attributes.asSequence().map { "${it.key}=\"${it.value}\"" }.joinToString(separator = " ", prefix = "<$name ", postfix = ">")
}

val Iterable<XmlNode>.children
	get() = this.flatMap(XmlNode::children)

val Iterable<XmlNode>.attributes
	get() = this.flatMap { it.attributes.entries } // TODO does it work?!

val Iterable<XmlNode>.content
	get() = this.map(XmlNode::content)


operator fun Iterable<XmlNode>.get(name: String) = this.filterByName(name)
operator fun Iterable<XmlNode>.get(p: Pair<String, String>) = this.filterByAttribute(p.first, p.second)
// TODO further filterBy Features as get operator?!

fun byName(nameFilter: (String) -> Boolean)
		= { node: XmlNode -> nameFilter.invoke(node.name) }

fun Iterable<XmlNode>.filterByName(nameFilter: (String) -> Boolean)
		= this.filter(byName(nameFilter))

fun byName(name: String)
		= byName { it == name }

fun Iterable<XmlNode>.filterByName(name: String)
		= this.filter(byName(name))

fun byChildren(childFilter: (XmlNode) -> Boolean)
		= { node: XmlNode -> node.children.any(childFilter) }

fun Iterable<XmlNode>.filterByChildren(childFilter: (XmlNode) -> Boolean)
		= this.filter(byChildren(childFilter))

fun byChildren(name: String)
		= byChildren({ it.name == name })

fun Iterable<XmlNode>.filterByChildren(name: String)
		= this.filter(byChildren(name))

fun byChildren(name: String, valueFilter: (XmlNode) -> Boolean)
		= byChildren({ it.name == name && valueFilter.invoke(it) })

fun Iterable<XmlNode>.filterByChildren(name: String, valueFilter: (XmlNode) -> Boolean)
		= this.filter(byChildren(name, valueFilter))

fun byAttribute(name: String, valueFilter: (String) -> Boolean = { true })
		= { node: XmlNode -> node.attributes[name]?.let(valueFilter) == true }

fun Iterable<XmlNode>.filterByAttribute(name: String, valueFilter: (String) -> Boolean)
		= this.filter(byAttribute(name, valueFilter))

fun byAttribute(name: String, value: String)
		= byAttribute(name, { it == value })

fun Iterable<XmlNode>.filterByAttribute(name: String, value: String)
		= this.filter(byAttribute(name, value))

fun byContent(contentFilter: (String) -> Boolean)
		= { node: XmlNode -> node.content?.let(contentFilter) == true }

fun Iterable<XmlNode>.filterByContent(contentFilter: (String) -> Boolean)
		= this.filter(byContent(contentFilter))

fun byContent(content: String)
		= byContent({ it == content })

fun Iterable<XmlNode>.filterByContent(content: String)
		= this.filter(byContent(content))