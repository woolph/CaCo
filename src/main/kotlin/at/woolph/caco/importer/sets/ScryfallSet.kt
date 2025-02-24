package at.woolph.caco.importer.sets

import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.utils.newOrUpdate
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.time.LocalDate
import java.util.*

@Serializable
data class ScryfallSet(
    @SerialName("object") val objectType: String,
    @Contextual val id: UUID,
    val code: String,
    val block_code: String? = null,
    val block: String? = null,
    val tcgplayer_id: Int? = null,
    val mtgo_code: String? = null,
    val arena_code: String? = null,
    @Contextual val uri: URI,
    val digital: Boolean,
    val card_count: Int,
    val printed_size: Int? = null,
    val parent_set_code: String? = null,
    val name: String,
    @Contextual val released_at: LocalDate,
    @Contextual val icon_svg_uri: URI,
    @Contextual val scryfall_uri: URI,
    @Contextual val search_uri: URI,
    val set_type: String,
    val nonfoil_only: Boolean,
    val foil_only: Boolean,
): ScryfallBase {
    override fun isValid() = objectType == "set"
    fun isNonDigitalSetWithCards() = !digital && card_count > 0
    fun isRootSet() = parent_set_code == null || !code.endsWith(parent_set_code)
    fun update(cardSet: CardSet) = cardSet.apply {
        name = this@ScryfallSet.name
        type = this@ScryfallSet.set_type
        dateOfRelease = this@ScryfallSet.released_at
        officalCardCount = this@ScryfallSet.card_count
        icon = this@ScryfallSet.icon_svg_uri
    }.also { updatedCardSet ->
        ScryfallCardSet.newOrUpdate(this@ScryfallSet.id) {
            setCode = this@ScryfallSet.code
            name = this@ScryfallSet.name
            set = updatedCardSet
        }
    }

    fun isImportWorthy() = (set_type != "memorabilia" || code == "30a" || code == "p30a")
}
