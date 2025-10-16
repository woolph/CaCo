/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.imagecache

// import javafx.scene.image.Image

import java.util.UUID
import kotlin.io.path.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageCache {
  private val folder = Path(System.getProperty("user.home"), ".caco", "image-cache")

  init {
    folder.createDirectories()
  }

  suspend fun getImageByteArray(id: String, imageLoader: suspend () -> ByteArray?): ByteArray? =
      withContext(Dispatchers.IO) {
        val cachedFile = folder.resolve(UUID.nameUUIDFromBytes(id.toByteArray()).toString())
        if (cachedFile.exists()) {
          cachedFile.readBytes()
        } else {
          val image = imageLoader()
          if (image != null) {
            cachedFile.writeBytes(image)
          }
          image
        }
      }

  //    suspend fun getImage(id: String, imageLoader: suspend () -> ByteArray?): Image? =
  //        getImageByteArray(id, imageLoader)?.let { Image(ByteArrayInputStream(it)) }
  //
  //    suspend fun cacheImage(id: String, imageLoader: suspend () -> ByteArray?) {
  //        getImageByteArray(id, imageLoader)
  //    }
}
