package at.woolph.caco.datamodel.sets

import at.woolph.caco.masterdata.imagecache.ImageCache
import at.woolph.caco.utils.compareToNullable
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.emptySized
import org.jetbrains.exposed.sql.javatime.date
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

object ScryfallCardSets : IdTable<UUID>() {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val code = varchar("setCode", length = 10).uniqueIndex()
    val type = enumeration<SetType>("type").index()
    val name = varchar("name", length = 256).index()
    val digitalOnly = bool("digitalOnly").index()

    val parentSetCode = varchar("parentSetCode", length = 10).index().nullable()

    val blockName = varchar("blockName", length = 256).nullable()
    val blockCode = varchar("blockCode", length = 10).index().nullable()

    val cardCount = integer("cardCount")
    val printedSize = integer("printedSize").nullable()
    val releaseDate = date("releaseDate").index()
    val icon = varchar("iconUri", length = 256).nullable()
}

class ScryfallCardSet(id: EntityID<UUID>) : UUIDEntity(id), Comparable<ScryfallCardSet> {
    companion object : UUIDEntityClass<ScryfallCardSet>(ScryfallCardSets) {
        fun findByCode(code: String?) = code?.let { find { ScryfallCardSets.code eq it }.firstOrNull() }
        fun findByParentSetCode(code: String?) = code?.let { find { ScryfallCardSets.parentSetCode eq it } } ?: emptySized()

        fun allRootSets() = all().filter(ScryfallCardSet::isRootSet)

        private fun compareSetCodeNullable(setCode: String?, otherSetcode: String?): Int? =
            setCode?.length?.compareToNullable(otherSetcode?.length) ?: setCode.compareToNullable(otherSetcode)
    }

    var code by ScryfallCardSets.code
    var parentSetCode by ScryfallCardSets.parentSetCode
    var name by ScryfallCardSets.name

    var type by ScryfallCardSets.type
    var digitalOnly  by ScryfallCardSets.digitalOnly
    var cardCount  by ScryfallCardSets.cardCount
    var printedSize  by ScryfallCardSets.printedSize
    var blockCode  by ScryfallCardSets.blockCode
    var blockName  by ScryfallCardSets.blockName

    var releaseDate by ScryfallCardSets.releaseDate
    var icon by ScryfallCardSets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })

    val cards by Card referrersOn Cards.set

    val childSets: SizedIterable<ScryfallCardSet> get() = findByParentSetCode(code)
    val selfAndNonRootChildSets: Sequence<ScryfallCardSet> get()  = sequence {
        yield(this@ScryfallCardSet)
        yieldAll(childSets.filterNot(ScryfallCardSet::isRootSet).flatMap(ScryfallCardSet::selfAndNonRootChildSets))
    }

    val cardsOfSelfAndNonRootChildSets = selfAndNonRootChildSets.flatMap { it.cards.asSequence() }

    val parentSet: ScryfallCardSet? get() = findByCode(parentSetCode)

    val isRootSet: Boolean get() = parentSetCode == null || (parentSet?.type != SetType.COMMANDER && type == SetType.COMMANDER)

    override fun compareTo(other: ScryfallCardSet): Int {
        if (id == other.id) return 0
        return releaseDate.compareToNullable(other.releaseDate)?.let { -it }
            ?: compareSetCodeNullable(code, other.code)
            ?: 0
    }

    override fun toString() = "[$code] $name"
}

suspend fun URI?.renderSvg2(size: Float): ByteArray? = this@renderSvg2?.toURL()?.let { url ->
    val byteArray = withContext(Dispatchers.IO) {
        HttpClient(CIO).use {
            it.request(url).readBytes()
        }
    }

    withContext(Dispatchers.Default) {
        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(ImageTranscoder.KEY_WIDTH, size)
            addTranscodingHint(ImageTranscoder.KEY_HEIGHT, size)
        }
        val buffered = ByteArrayOutputStream()
        buffered.use {
            transcoder.transcode(TranscoderInput(ByteArrayInputStream(byteArray)), TranscoderOutput(it))
        }
        return@withContext buffered.toByteArray()
    }
}

suspend fun URI.loadSetLogo(size: Float) = ImageCache.getImage(toString()) { renderSvg(size) }

fun URI?.renderSvg(size: Float): ByteArray? = (this?.toURL()?.openConnection() as? HttpURLConnection)?.let { conn ->
    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "image/svg+xml")

        if (conn.responseCode != 200) {
            throw Exception("Failed : HTTP error code : ${conn.responseCode}")
        }

        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(ImageTranscoder.KEY_WIDTH, size)
            addTranscodingHint(ImageTranscoder.KEY_HEIGHT, size)
        }

        val buffered = ByteArrayOutputStream()
        buffered.use {
            transcoder.transcode(TranscoderInput(conn.inputStream), TranscoderOutput(it))
        }

        return@let buffered.toByteArray()
    } catch(ex:Exception) {
        return null
//        throw Exception("unable to load svg $this", ex)
    } finally {
        conn.disconnect()
    }
}

fun URI?.renderSvg(size: Float, strokeColor: String, color1: String, color2: String): ByteArray? = (this?.toURL()?.openConnection() as? HttpURLConnection)?.let { conn ->
    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "image/svg+xml")

        if (conn.responseCode != 200) {
            throw Exception("Failed : HTTP error code : ${conn.responseCode}")
        }

        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(ImageTranscoder.KEY_WIDTH, size)
            addTranscodingHint(ImageTranscoder.KEY_HEIGHT, size)
        }

        val svgContent = String(conn.inputStream.readBytes())
        val styleToBeApplied = "<defs>\n" +
            "    <linearGradient id=\"uncommon-gradient\" x2=\"0.35\" y2=\"1\">\n" +
            "        <stop offset=\"0%\" stop-color=\"$color1\" />\n" +
            "        <stop offset=\"50%\" stop-color=\"$color2\" />\n" +
            "        <stop offset=\"100%\" stop-color=\"$color1\" />\n" +
            "      </linearGradient>\n" +
            "  </defs>\n" +
            "<path stroke=\"$strokeColor\" stroke-width=\"2%\" style=\"fill:url(#uncommon-gradient)\" ";
        val inputStream = ByteArrayInputStream(styleToBeApplied.let {
            svgContent.replace("<path ", it)
        }.toByteArray())
        val buffered = ByteArrayOutputStream()
        buffered.use {
            transcoder.transcode(TranscoderInput(inputStream), TranscoderOutput(it))
        }

        return@let buffered.toByteArray()
    } catch(ex:Exception) {
        throw Exception("unable to load svg $this", ex)
    } finally {
        conn.disconnect()
    }
}
