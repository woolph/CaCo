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

    val iconImage by lazy {
        (icon?.toURL()?.openConnection() as? HttpURLConnection)?.let { conn ->
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "image/svg+xml")

                if (conn.responseCode != 200) {
                    throw Exception("Failed : HTTP error code : ${conn.responseCode}")
                }

                val size = 48.0f
                val transcoder = PNGTranscoder().apply {
                    addTranscodingHint(ImageTranscoder.KEY_WIDTH, size)
                    addTranscodingHint(ImageTranscoder.KEY_HEIGHT, size)
                }

                val buffered = ByteArrayOutputStream()
                buffered.use {
                    transcoder.transcode(TranscoderInput(conn.inputStream), TranscoderOutput(it))
                }

                return@let Image(ByteArrayInputStream(buffered.toByteArray()))
            } catch(ex:Exception) {
                //throw Exception("unable to load svg $icon", ex)
                return@let null
            } finally {
                conn.disconnect()
            }
        }
    }
}
