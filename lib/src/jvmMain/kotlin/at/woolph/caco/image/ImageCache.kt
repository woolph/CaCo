/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.image

import arrow.core.Either
import arrow.core.raise.either
import java.util.UUID
import kotlin.io.path.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object ImageCache {
  private val folder = Path(System.getProperty("user.home"), ".caco", "image-cache")

  init {
    folder.createDirectories()
  }

  actual suspend fun getImageByteArray(
    id: String,
    imageLoader: suspend () -> Either<Throwable, ByteArray>,
  ): Either<Throwable, ByteArray> = either {
    withContext(Dispatchers.IO) {
      val cachedFile = folder.resolve(UUID.nameUUIDFromBytes(id.toByteArray()).toString())
      if (cachedFile.exists()) {
        cachedFile.readBytes()
      } else {
        imageLoader().onRight { image ->
          cachedFile.writeBytes(image)
        }.bind()
      }
    }
  }


  //    suspend fun getImage(id: String, imageLoader: suspend () -> ByteArray?): Image? =
  //        getImageByteArray(id, imageLoader)?.let { Image(ByteArrayInputStream(it)) }
  //
  //    suspend fun cacheImage(id: String, imageLoader: suspend () -> ByteArray?) {
  //        getImageByteArray(id, imageLoader)
  //    }
}
