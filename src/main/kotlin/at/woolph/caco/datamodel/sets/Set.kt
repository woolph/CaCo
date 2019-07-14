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
import java.nio.file.Paths

object Sets : IntIdTable() {
    val shortName = varchar("shortName", length = 3).index()
    val name = varchar("name", length = 256).index()
    val dateOfRelease = date("dateOfRelease").index()
    val officalCardCount = integer("officalCardCount").default(0)
    val digitalOnly = bool("digitalOnly").default(false).index()
    val icon = varchar("iconURI", length = 256).nullable()
}

class Set(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Set>(Sets)

    var shortName by Sets.shortName
    var name by Sets.name
    var dateOfRelease by Sets.dateOfRelease
    var officalCardCount by Sets.officalCardCount
    var digitalOnly by Sets.digitalOnly
    var icon by Sets.icon.transform({ it?.toString() }, { it?.let { URI(it) } })

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
                throw Exception("unable to load svg $icon", ex)
            } finally {
                conn.disconnect()
            }
        }
    }
}
