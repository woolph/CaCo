package at.woolph.caco.datamodel.sets

import at.woolph.caco.cli.manabase.ColorIdentity
import at.woolph.caco.cli.manabase.ManaColor
import at.woolph.caco.datamodel.collection.ArenaCardPossession
import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.masterdata.import.toEnumSet
import at.woolph.caco.utils.compareToNullable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import java.net.URI
import java.util.*

object Cards : IdTable<UUID>() {
    override val id = uuid("scryfallId").entityId()
    override val primaryKey = PrimaryKey(id)

    val set = reference("set", ScryfallCardSets).index()
    val collectorNumber = varchar("number", length = 10).index()
    val name = varchar("name", length = 256).index()
    val nameDE = varchar("nameDE", length = 256).index().nullable()
    val arenaId = integer("arenaId").nullable().index()
    val rarity = enumeration("rarity", Rarity::class).index()
    val promo = bool("promo").default(false).index()
    val token = bool("token").default(false).index()
    val image = varchar("imageURI", length = 512).nullable()
    val cardmarketUri = varchar("cardmarketUri", length = 512).nullable()

    val extra = bool("extra").default(false)
    val finishes = integer("finishes").default(1)
    val fullArt = bool("fullArt").default(false)
    val extendedArt = bool("extendedArt").default(false)
    val specialDeckRestrictions = integer("specialDeckRestrictions").nullable()

    val manaCost = varchar("manaCost", length = 256).nullable()
    val manaValue = float("manaValue").index()
    val oracleText = varchar("oracleText", length = 4096)
    val type = varchar("type", length = 256).nullable()
    val colorIdentity = integer("colorIdentity")

    val price = double("price").nullable()
    val priceFoil = double("priceFoil").nullable()
    val gameChanger = bool("gameChanger").index()
    val edhrecRank = integer("edhrecRank").nullable()
}

@Serializable
enum class Finish {
    @SerialName("nonfoil") Normal,
    @SerialName("foil") Foil,
    @SerialName("etched") Etched,
    ;

    companion object {
        fun parse(finish: String) = when(finish.lowercase()) {
            "normal", "nonfoil" -> Normal
            "foil" -> Foil
            "etched" -> Etched
            else -> throw IllegalArgumentException("Unknown finish $finish")
        }
    }
}

@Serializable
enum class Legality {
    Legal,
    @SerialName("not_legal") NotLegal,
    Restricted,
    Banned,
}

class Card(id: EntityID<UUID>) : UUIDEntity(id), Comparable<Card>, CardRepresentation {
    companion object : UUIDEntityClass<Card>(Cards) {
        val CARD_DRAW_PATTERN = Regex("draws? (|a |two |three )cards?", RegexOption.IGNORE_CASE)

        private fun compareCollectorNumberNullable(collectorNumber: String, otherCollectorNumber: String) : Int? {
            val (prefix, number, suffix) = splitCollectorNumber(collectorNumber)
            val (otherPrefix, otherNumber, otherSuffix) = splitCollectorNumber(otherCollectorNumber)
            return prefix.compareToNullable(otherPrefix)
                ?: number.compareToNullable(otherNumber)
                ?: suffix.compareToNullable(otherSuffix)
        }

        private fun splitCollectorNumber(collectorNumber: String): Triple<String?, Int, String?> {
            val match = COLLECTION_NUMBER_PATTERN.find(collectorNumber) ?: return Triple(null, 0, null)
            val prefix = match.groups["prefix"]?.value
            val number = match.groups["number"]!!.value.toInt()
            val suffix = match.groups["suffix"]?.value
            return Triple(prefix, number, suffix)
        }
        internal val COLLECTION_NUMBER_PATTERN = Regex("^(?<prefix>\\w+-)?(?<number>\\d+)(?<suffix>.+)?$")
    }

    val scryfallId: UUID get() = id.value
    val variants by CardVariant referrersOn CardVariants

    var set by ScryfallCardSet referencedOn Cards.set
    var collectorNumber by Cards.collectorNumber
    var name by Cards.name
    var nameDE by Cards.nameDE
    var arenaId by Cards.arenaId
    var rarity by Cards.rarity
    var promo by Cards.promo
    var token by Cards.token
    var image by Cards.image.transform({ it?.toString() }, { it?.let { URI(it) } })
    var thumbnail by Cards.image.transform(
        {
            it?.toString()?.replace(".jpg",".png")
            ?.replace("/small/front", "/png/front")
        },
        {
            it?.replace(".png",".jpg")
                ?.replace("/png/front", "/small/front")
                ?.replace( "c1.scryfall.com/file/scryfall-cards/", "cards.scryfall.io/") // old url
                ?.let { URI(it) } },
        )
    var cardmarketUri by Cards.cardmarketUri.transform({ it?.toString() }, { it?.let { URI(it) } })

    var extra by Cards.extra
    var finishes: EnumSet<Finish> by Cards.finishes.transform({
        it.fold(0) { acc, finish -> acc or (1 shl finish.ordinal) } }, { Finish.entries.asSequence()
        .filter { finish -> (it and (1 shl finish.ordinal)) != 0 }.toEnumSet() })
    var fullArt by Cards.fullArt
    var extendedArt by Cards.extendedArt
    var specialDeckRestrictions by Cards.specialDeckRestrictions

    var manaCost by Cards.manaCost
    var manaValue by Cards.manaValue
    var type by Cards.type
    var oracleText by Cards.oracleText

    var price by Cards.price
    var priceFoil by Cards.priceFoil
    var gameChanger by Cards.gameChanger
    var edhrecRank by Cards.edhrecRank

    var colorIdentity by Cards.colorIdentity.transform(
        { it.colorIdentity.fold(0) { acc: Int, manaColor: ManaColor -> (acc or (1 shl manaColor.ordinal)) } },
        { ColorIdentity(ManaColor.entries.asSequence()
            .filter { manaColor -> (it and (1 shl manaColor.ordinal)) != 0 }.toEnumSet())
        })

    val possessions by CardPossession referrersOn CardPossessions
    val arenaPossessions by ArenaCardPossession referrersOn ArenaCardPossessions

    val isCreature: Boolean get() = type?.contains("Creature") == true
    val isLand: Boolean get() = type?.contains("Land") == true
    val isCheapNonland: Boolean
        get() = manaValue <= 2.0 && !isLand

    val isCheapCardDraw: Boolean by lazy {
        (isCheapNonland && (oracleText.contains(CARD_DRAW_PATTERN) &&
                oracleTextNone("{4}", "blood token", "investigate") &&
                ((!isCreature || oracleTextAll("when", "enters")))) ||
                (!isCreature && !oracleText.contains(Regex("pays?", RegexOption.IGNORE_CASE)) &&
                oracleTextAll("look", "library", "put", "your hand"))) ||
                !isLand && oracleText.contains(Regex("cycling( \\{([012])\\}|—pay \\d+ life)", RegexOption.IGNORE_CASE))
    }

    val blacklistCheapRamp = listOf(
        "Dreamscape Artist",
        "Crop Rotation",
        "Ordeal of Nylea",
        "Khalni Heart Expedition",
        "Oashra Cultivator",
        "Elvish Reclaimer",
    )
    val isCheapRamp: Boolean by lazy {
        isCheapNonland && !isCheapCardDraw && (
                oracleText.contains(Regex("\\badd\\b", RegexOption.IGNORE_CASE)) && oracleTextNone("add its ability", "add a lore counter", "dies") ||
                        oracleTextAll("search", "your library", "put", "onto the battlefield") && oracleTextAny("land", "basic") && name !in blacklistCheapRamp ||
                        oracleTextAll("enchanted land is tapped", "adds an additional") ||
                        oracleTextAll("put a creature card with", "from your hand onto the battlefield")
            )
    }

    val isMDFCLand : Boolean get() = (type?.contains("// Land") == true) xor (type?.contains("Land // ") == true)

    val isMDFCLandTapped : Boolean by lazy {
        isMDFCLand && oracleTextAll("enters", "tapped") && oracleTextNone("you may pay", "unless")
    }
    val isMDFCLandUntapped : Boolean by lazy {
        isMDFCLand && (!oracleTextAll("enters", "tapped") || oracleTextAny("you may pay", "unless"))
    }

    private fun oracleTextAll(vararg keywords: String) = keywords.all { oracleText.contains(it, ignoreCase = true) }
    private fun oracleTextAny(vararg keywords: String) = keywords.any { oracleText.contains(it, ignoreCase = true) }
    private fun oracleTextNone(vararg keywords: String) = keywords.none { oracleText.contains(it, ignoreCase = true) }

    override fun compareTo(other: Card): Int =
        set.compareToNullable(other.set)
            ?: compareCollectorNumberNullable(collectorNumber, other.collectorNumber)
            ?: 0

    override fun toString(): String = "[${set.code}-$collectorNumber] $name"

    override val card: Card get() = this
    override val variantType: CardVariant.Type? get() = null
}
