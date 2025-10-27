/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.SetType
import at.woolph.caco.lib.Uri
import java.net.URI
import java.time.LocalDate
import java.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ScryfallSet(
  @SerialName("object") val objectType: String,
  @Contextual val id: Uuid,
  val code: String,
  val block_code: String? = null,
  val block: String? = null,
  val tcgplayer_id: Int? = null,
  val mtgo_code: String? = null,
  val arena_code: String? = null,
  @Contextual val uri: Uri,
  val digital: Boolean,
  val card_count: Int,
  val printed_size: Int? = null,
  val parent_set_code: String? = null,
  val name: String,
  @Contextual val released_at: LocalDate,
  @Contextual val icon_svg_uri: Uri,
  @Contextual val scryfall_uri: Uri,
  @Contextual val search_uri: Uri,
  val set_type: SetType,
  val nonfoil_only: Boolean,
  val foil_only: Boolean,
) : ScryfallBase {
  override fun isValid() = objectType == "set"

  fun update(scryfallCardSet: ScryfallCardSet) {
    scryfallCardSet.code = this@ScryfallSet.code
    scryfallCardSet.name = this@ScryfallSet.name

    scryfallCardSet.blockCode =
        when { // reassigning blockCode
          code in setOf("gnt", "gn2", "gn3") -> "gnt"
          else -> block_code
        }
    scryfallCardSet.blockName =
        when { // reassigning blockName
          code in setOf("gnt", "gn2", "gn3") -> "Game Night"
          else -> block
        }
    scryfallCardSet.digitalOnly = digital
    scryfallCardSet.cardCount = card_count
    scryfallCardSet.printedSize = printed_size
    scryfallCardSet.type = set_type
    scryfallCardSet.parentSetCode =
        when { // reassigning parent_set_codes
          code == "gk1" -> "grn"
          code == "gk2" -> "rna"
          code == "pltc" -> "ltc"
          code == "h1r" -> "mh1"
          code == "h2r" -> "mh2"
          else -> parent_set_code
        }
    scryfallCardSet.releaseDate = this@ScryfallSet.released_at
    scryfallCardSet.icon = this@ScryfallSet.icon_svg_uri
  }

  companion object {
    val memorabiliaWhiteList =
        listOf(
            "ptg",
            "30a",
            "p30a",
        )
  }

  val isImportWorthy: Boolean
    get() =
        !digital &&
            card_count > 0 &&
            code != "plst" &&
            (set_type != SetType.MEMORABILIA ||
                code.startsWith("f") ||
                code.startsWith("o") ||
                code in memorabiliaWhiteList)
}
