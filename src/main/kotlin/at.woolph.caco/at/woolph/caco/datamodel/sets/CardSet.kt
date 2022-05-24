package at.woolph.caco.datamodel.sets

import javafx.scene.image.Image
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

object CardSets : IntIdTable() {
    val shortName = varchar("shortName", length = 6).index()
    val name = varchar("name", length = 256).index()
    val dateOfRelease = date("dateOfRelease").index()
    val officalCardCount = integer("officalCardCount").default(0)
    val digitalOnly = bool("digitalOnly").default(false).index()
    val icon = varchar("iconURI", length = 256).nullable()
    val specialDeckRestriction = integer("specialDeckRestriction").nullable()

    val otherScryfallSetCodes = varchar("otherScryfallSetCodes", length = 256).default("")
}

class CardSet(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardSet>(CardSets)

    var shortName by CardSets.shortName
    var name by CardSets.name
    var dateOfRelease by CardSets.dateOfRelease
    var officalCardCount by CardSets.officalCardCount
    var digitalOnly by CardSets.digitalOnly
    var icon by CardSets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })
    var specialDeckRestriction by CardSets.specialDeckRestriction

    var otherScryfallSetCodes by CardSets.otherScryfallSetCodes.transform({ it.joinToString(",") }, { if (it.isEmpty()) emptyList() else it.split(",") })

    val cards by Card referrersOn Cards.set

    val iconImage by lazy { icon.renderSvg(48f)?.let { Image(ByteArrayInputStream(it)) } }

    // TODO consider representing the collection completion in % by filling the set icon with a progress bar style color gradient
}

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
