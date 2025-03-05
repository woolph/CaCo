package at.woolph.caco.masterdata.imagecache

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.masterdata.imagecache.CachedImage.Companion.transform
import at.woolph.caco.utils.newOrUpdate
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream


object CachedImages : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("uri", 256).entityId()
    val content = blob("content").transform(::ExposedBlob, ExposedBlob::bytes)
}

class CachedImage(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, CachedImage>(CachedImages)

    var content by CachedImages.content
}

object ImageCache {
    init {
        transaction(Databases.imageCache) {
            SchemaUtils.createMissingTablesAndColumns(CachedImages)
        }
    }

    suspend fun getImageByteArray(id: String, imageLoader: () -> ByteArray?): ByteArray? =
        newSuspendedTransaction(Dispatchers.IO, Databases.imageCache) {
            CachedImage.findById(id)?.content ?: imageLoader()?.also { renderedImage ->
                try {
                    CachedImage.newOrUpdate(id) { cachedImage ->
                        cachedImage.content = renderedImage
                    }
                } catch(e: Throwable) {
                    null
                }
            }
        }

    suspend fun getImage(id: String, imageLoader: () -> ByteArray?): Image? =
        getImageByteArray(id, imageLoader)?.let { Image(ByteArrayInputStream(it)) }

    suspend fun cacheImage(id: String, imageLoader: () -> ByteArray?) {
        getImageByteArray(id, imageLoader)
    }
}
