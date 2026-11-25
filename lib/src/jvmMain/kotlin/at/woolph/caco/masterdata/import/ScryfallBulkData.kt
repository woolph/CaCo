/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScryfallBulkData(
    @SerialName("object") val objectType: String,
    @Contextual val id: UUID,
    val type: String,
    @SerialName("updated_at") @Contextual val updatedAt: ZonedDateTime,
    @Contextual val uri: URI,
    val name: String,
    val description: String,
    @SerialName("compressed_size") val compressedSize: Long,
    @SerialName("jsonl_download_uri") @Contextual val downloadUri: URI,
) : ScryfallBase {
  override fun isValid() = objectType == "bulk_data"
}
