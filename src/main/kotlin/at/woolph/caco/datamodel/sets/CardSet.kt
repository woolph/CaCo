package at.woolph.caco.datamodel.sets

import at.woolph.caco.imagecache.ImageCache
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.date
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.*

object ScryfallCardSets : IdTable<UUID>() {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val setCode = varchar("setCode", length = 10).index()
    val set = reference("set", CardSets).index()
    val name = varchar("name", length = 256).index()
}

class ScryfallCardSet(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ScryfallCardSet>(ScryfallCardSets)

    var setCode by ScryfallCardSets.setCode
    var name by ScryfallCardSets.name

    var set by CardSet referencedOn ScryfallCardSets.set

    val cards by Card referrersOn Cards.set
}

object CardSets : IdTable<String>() {
    override val id = varchar("id", length = 6).entityId()
    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", length = 256).index()
    val dateOfRelease = date("dateOfRelease").index()
    val officalCardCount = integer("officalCardCount").default(0)
    val digitalOnly = bool("digitalOnly").default(false).index()
    val icon = varchar("iconURI", length = 256).nullable()
    val specialDeckRestriction = integer("specialDeckRestriction").nullable()
}

class CardSet(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, CardSet>(CardSets)

    var shortName by CardSets.id
    var name by CardSets.name
    var dateOfRelease by CardSets.dateOfRelease
    var officalCardCount by CardSets.officalCardCount
    var digitalOnly by CardSets.digitalOnly
    var icon by CardSets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })
    var specialDeckRestriction by CardSets.specialDeckRestriction

    val scryfallCardSets by ScryfallCardSet referrersOn ScryfallCardSets.set

    val cards: List<Card>
        get() = scryfallCardSets.flatMap(ScryfallCardSet::cards)
    // TODO consider representing the collection completion in % by filling the set icon with a progress bar style color gradient
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
