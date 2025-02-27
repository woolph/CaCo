package at.woolph.caco.importer.sets

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.parseRarity
import at.woolph.caco.utils.newOrUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

private val LOG = LoggerFactory.getLogger("at.woolph.caco.importer.sets.ScryfallCard")

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }
}

object URISerializer : KSerializer<URI> {
    override val descriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URI {
        try {
            return URI.create(decoder.decodeString())
        } catch (t: Throwable) {
            return URI.create("about://version")
        }
    }

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }
}
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }
}
val jsonSerializer = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        contextual(LocalDate::class, LocalDateSerializer)
        contextual(URI::class, URISerializer)
        contextual(UUID::class, UUIDSerializer)
        contextual(ZonedDateTime::class, ZonedDateTimeSerializer)
    }
}

@Serializable
data class ScryfallRelatedCard(
    @SerialName("object") val objectType: String,
    @Contextual val id: UUID,
    val component: String,
    val name: String,
    val type_line: String,
    @Contextual val uri: URI,
): ScryfallBase {
    override fun isValid() = objectType == "related_card"
}

@Serializable
data class ScryfallCardFace(
    @SerialName("object") val objectType: String,
    val name: String,
    val cmc: Double? = null,
    val mana_cost: String,
    val type_line: String? = null,
    @Contextual val oracle_id: UUID? = null,
    val oracle_text: String,
    val layout: String? = null,
    val printed_name: String? = null,
    val printed_text: String? = null,
    val printed_type_line: String? = null,
    val colors: Set<MtgColor> ? = null,
    val color_indicator: Set<MtgColor> ? = null,
    val power: String? = null,
    val toughness: String? = null,
    val loyalty: String? = null,
    val defense: String? = null,
    val flavor_text: String? = null,
    val flavor_name: String? = null,
    val watermark: String? = null,
    val artist: String? = null,
    @Contextual val artist_id: UUID? = null,
    @Contextual val illustration_id: UUID? = null,
    val image_uris: Map<String, @Contextual URI>? = null,
): ScryfallBase {
    override fun isValid() = objectType == "card_face"
}

@Serializable
data class ScryfallPreviewInfo(
    val source: String,
    @Contextual val source_uri: URI,
    @Contextual val previewed_at: LocalDate,
)

@Serializable
data class ScryfallCard(
    @SerialName("object") val objectType: String,
    @Contextual val id: UUID,
    @Contextual val oracle_id: UUID? = null,
    val multiverse_ids: Set<Int>,
    val mtgo_id: Int? = null,
    val mtgo_foil_id: Int? = null,
    val arena_id: Int? = null,
    val tcgplayer_id: Int? = null,
    val tcgplayer_etched_id: Int? = null,
    val cardmarket_id: Int? = null,
    val name: String,
    val printed_name: String? = null,
    val lang: String,
    @Contextual val released_at: LocalDate,
    @Contextual val uri: URI,
    @Contextual val scryfall_uri: URI,
    val layout: String,
    val highres_image: Boolean,
    val image_status: String,
    val image_uris: Map<String, @Contextual URI>? = null,
    val card_faces: List<ScryfallCardFace>? = null,
    val mana_cost: String? = null,
    val cmc: Double? = null,
    val type_line: String? = null,
    val printed_type_line: String? = null,
    val oracle_text: String? = null,
    val life_modifier: String? = null,
    val hand_modifier: String? = null,
    val printed_text: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val loyalty: String? = null,
    val colors: Set<MtgColor>? = null,
    val color_identity: Set<MtgColor>,
    val color_indicator: Set<MtgColor> ? = null,
    val keywords: Set<String>,
    val produced_mana: Set<MtgColor>? = null,
    val all_parts: List<ScryfallRelatedCard> = emptyList(),
    val legalities: Map<String, String>,
    val games: Set<String>,
    val reserved: Boolean,
    val foil: Boolean,
    val nonfoil: Boolean,
    val finishes: Set<String>,
    val oversized: Boolean,
    val promo: Boolean,
    val promo_types: Set<String> = emptySet(),
    val reprint: Boolean,
    val variation: Boolean,
    @Contextual val variation_of: UUID? = null,
    @Contextual val set_id: UUID,
    val set: String,
    val set_name: String,
    val set_type: String,
    @Contextual val set_uri: URI,
    @Contextual val set_search_uri: URI,
    @Contextual val scryfall_set_uri: URI,
    @Contextual val rulings_uri: URI,
    @Contextual val prints_search_uri: URI,
    val collector_number: String,
    val digital: Boolean,
    val rarity: String,
    val watermark: String? = null,
    val flavor_text: String? = null,
    val flavor_name: String? = null,
    @Contextual val card_back_id: UUID? = null,
    val artist: String,
    val artist_ids: Set<@Contextual UUID> = emptySet(),
    @Contextual val illustration_id: UUID? = null,
    val border_color: String,
    val frame: String,
    val frame_effects: Set<String> = emptySet(),
    val security_stamp: String? = null,
    val full_art: Boolean,
    val textless: Boolean,
    val booster: Boolean,
    val story_spotlight: Boolean,
    val edhrec_rank: Int? = null,
    val penny_rank: Int? = null,
    val preview: ScryfallPreviewInfo? = null,
    val prices: Map<String, String?>,
    val related_uris: Map<String, @Contextual URI>,
    val purchase_uris: Map<String, @Contextual URI> = emptyMap(),
    val content_warning: Boolean = false,
    val attraction_lights: Set<Int>? = null,
): ScryfallBase {
    override fun isValid() = objectType == "card"

    fun isNoPromoPackStampedAndNoPrereleasePackStampedVersion() =
        !(promo_types.contains("promopack") && promo_types.contains("stamped") && set !in listOf("ppp1"))
                && !(promo_types.contains("prerelease") && promo_types.contains("datestamped") && set !in listOf("pmh1", "pbng"))

    fun isImportWorthy() = isNoPromoPackStampedAndNoPrereleasePackStampedVersion() &&
        layout != "art_series" && !digital && set != "ced"

    class SetNotInDatabaseException(val set: String, val setName: String, val setType: String): Exception("set not found $set $setName")

    fun update(card: Card) = card.also {
        val isPromo = promo
        val isToken = set_type == "token"

        it.set = ScryfallCardSet.findById(set_id) ?: throw SetNotInDatabaseException(set, set_name, set_type)
        it.numberInSet = collector_number
        it.name = name
        it.arenaId = arena_id
        it.rarity = rarity.parseRarity()
        it.promo = isPromo
        it.token = isToken
        it.image = image_uris?.get("png") ?: card_faces?.get(0)?.image_uris?.get("png")
        it.cardmarketUri = purchase_uris.get("cardmarket")

        it.extra = !booster
        it.nonfoilAvailable = nonfoil
        it.foilAvailable = foil
        it.fullArt = full_art
        it.extendedArt = frame_effects.contains("extendedart")

        it.colorIdentity = color_identity.toColorIdentity()

        it.manaCost = mana_cost ?: card_faces?.mapNotNull { it.mana_cost }?.joinToString(" // ")
        it.manaValue = cmc?.toFloat() ?: 0.0f
        it.oracleText = sequence {
            oracle_text?.let { yield(it) }
            yieldAll(card_faces?.asSequence()?.map { it.oracle_text } ?: emptySequence())
        }.joinToString("\n")
        it.price = prices["eur"]?.toDouble()
        it.priceFoil = prices["eur_foil"]?.toDouble()
        it.type = type_line ?: card_faces?.mapNotNull { it.type_line }?.joinToString(" // ")

        val patternSpecialDeckRestrictions = Regex("A deck can have (any number of cards|only one card|up to (\\w+) cards) named ${this@ScryfallCard.name}\\.")
        it.specialDeckRestrictions = oracle_text?.let { patternSpecialDeckRestrictions.find(it) }?.let {
            when {
                it.groupValues[1] == "any number of cards" -> Int.MAX_VALUE // TODO
                it.groupValues[1] == "only one card" -> 1 // TODO
                else -> when(it.groupValues[2]) {
                    "one" -> 1
                    "two" -> 2
                    "three" -> 3
                    "four" -> 4
                    "five" -> 5
                    "six" -> 6
                    "seven" -> 7
                    "eight" -> 8
                    "nine" -> 9
                    "ten" -> 10
                    "eleven" -> 11
                    "twelve" -> 12
                    "thirteen" -> 13
                    "fourteen" -> 14
                    "fifteen" -> 15
                    else -> throw IllegalStateException("the following value is not recognized currently: " + it.groupValues[2])
                }
            }
        }
    }

    fun determinePrintedName(): String? = printed_name ?:
        card_faces?.let {
            "${it[0]} // ${it[1]}"
        }
}

internal fun CardSet.loadCardsFromScryfall(language: String? = null, optional: Boolean = false): Flow<ScryfallCard> =
    scryfallCardSets.asFlow().flatMapConcat {
        LOG.trace("loadCardsFromScryfall for $this for language $language")
        paginatedDataRequest("https://api.scryfall.com/cards/search?q=${ language?.let {"lang%3A$it%20"} ?: "" }set%3A${it.setCode}&unique=prints&order=set", optional)
    }

suspend fun CardSet.importCardsOfSet(additionalLanguages: List<String> = emptyList()) {
    // FIXME i want to represent every card as it is in reality (double sided token from commander or master sets etc.)
    LOG.trace("import cards of set ${this.shortName}")
    loadCardsFromScryfall()
        .filter(ScryfallCard::isNoPromoPackStampedAndNoPrereleasePackStampedVersion)
        .collect {
            Card.newOrUpdate(it.id) {
                it.update(this)
            }
        }

    additionalLanguages.forEach { language ->
        loadCardsFromScryfall(language, true)
            .filter(ScryfallCard::isNoPromoPackStampedAndNoPrereleasePackStampedVersion)
            .collect {
                Card.findById(it.id)?.apply {
                    nameDE = it.determinePrintedName()
                }
            }
    }
}
