/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.image

import arrow.core.Either

interface ImageCache {
  suspend fun getImageByteArray(id: String, imageLoader: suspend () -> Either<Throwable, ByteArray>): Either<Throwable, ByteArray>
}

expect val ImageCacheImpl: ImageCache
