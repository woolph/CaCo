/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.time.ZonedDateTime
import java.util.*

@Serializable
data class ScryfallBulkData(
    @SerialName("object") val objectType: String,
    @Contextual val id: UUID,
    val type: String,
    @SerialName("updated_at") @Contextual val updatedAt: ZonedDateTime,
    @Contextual val uri: URI,
    val name: String,
    val description: String,
    val size: Long,
    @SerialName("download_uri") @Contextual val downloadUri: URI,
    @SerialName("content_type") val contentType: String,
    @SerialName("content_encoding") val contentEncoding: String,
): ScryfallBase {
    override fun isValid() = objectType == "bulk_data"
}