/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import java.net.URI
import java.util.UUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScryfallRelatedCard(
    @SerialName("object") val objectType: String,
    @Contextual val id: UUID,
    val component: String,
    val name: String,
    val type_line: String,
    @Contextual val uri: URI,
) : ScryfallBase {
  override fun isValid() = objectType == "related_card"
}
