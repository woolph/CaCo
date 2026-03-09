package at.woolph.caco.datamodel.sets

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Format
import at.woolph.utils.compareToNullable
import at.woolph.utils.currency.CurrencyValue
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import java.net.URI
import kotlin.uuid.Uuid

object CardPrints: IdTable<Uuid>() {
  override val id = uuid("scryfallId").entityId()
  override val primaryKey = PrimaryKey(id)

  val card = reference("card", Cards).index()
  val set = reference("set", ScryfallCardSets).index()
  val collectorNumber = varchar("number", length = 10).index()
  val flavorName = varchar("flavorName", length = 256).nullable()
  val arenaId = integer("arenaId").nullable().index()
  val rarity = enumeration("rarity", Rarity::class).index()
  val promo = bool("promo").default(false).index()
  val promoType = array<String>("promoType").default(emptyList())
  val image = varchar("imageURI", length = 512).nullable()
  val cardmarketUri = varchar("cardmarketUri", length = 512).nullable()

  val extra = bool("extra").default(false)
  val finishes = integer("finishes").default(1)
  val fullArt = bool("fullArt").default(false)
  val extendedArt = bool("extendedArt").default(false)

  val price = double("price").nullable()
  val priceFoil = double("priceFoil").nullable()
  val priceEtched = double("priceEtched").nullable()
}


class CardPrint(id: EntityID<Uuid>) : UuidEntity(id), Comparable<CardPrint>, CardRepresentation {
  companion object : UuidEntityClass<CardPrint>(CardPrints) {
    val CARD_DRAW_PATTERN = Regex("draws? (|a |two |three )cards?", RegexOption.IGNORE_CASE)

    private fun compareCollectorNumberNullable(
      collectorNumber: String,
      otherCollectorNumber: String,
    ): Int? {
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

    internal val COLLECTION_NUMBER_PATTERN =
      Regex("^(?<prefix>\\w+-)?(?<number>\\d+)(?<suffix>.+)?$")
  }

  val scryfallId: Uuid
    get() = id.value

  val variants by CardPrintVariant referrersOn CardPrintVariants

  var card by Card referencedOn CardPrints.card
  var set by ScryfallCardSet referencedOn CardPrints.set
  var collectorNumber by CardPrints.collectorNumber

  val mergedName: String
    get() = flavorName?.let { "$it ($name)" } ?: name
  val name: String get() = card.name
  val nameDE: String? get() = card.nameDE
  var flavorName by CardPrints.flavorName
  var arenaId by CardPrints.arenaId
  var rarity by CardPrints.rarity
  var promo by CardPrints.promo
  var image by CardPrints.image.transform({ it?.toString() }, { it?.let { URI(it) } })
  var thumbnail by
  CardPrints.image.transform(
    { it?.toString()?.replace(".jpg", ".png")?.replace("/small/front", "/png/front") },
    {
      it?.replace(".png", ".jpg")
        ?.replace("/png/front", "/small/front")
        ?.replace("c1.scryfall.com/file/scryfall-cards/", "cards.scryfall.io/") // old url
        ?.let { URI(it) }
    },
  )
  var cardmarketUri by CardPrints.cardmarketUri.transform({ it?.toString() }, { it?.let { URI(it) } })

  var extra by CardPrints.extra
  var finishes: Set<Finish> by
  CardPrints.finishes.transform(
    { it.fold(0) { acc, finish -> acc or (1 shl finish.ordinal) } },
    {
      Finish.entries
        .asSequence()
        .filter { finish -> (it and (1 shl finish.ordinal)) != 0 }
        .toSet()
    },
  )
  var fullArt by CardPrints.fullArt
  var extendedArt by CardPrints.extendedArt

  fun isLegalIn(format: Format): Boolean =
    card.isLegalIn(format)

  var priceNormal: CurrencyValue? by
  CardPrints.price.transform({ it?.value }, { it?.let { CurrencyValue.usd(it) } })
  var priceFoil: CurrencyValue? by
  CardPrints.priceFoil.transform({ it?.value }, { it?.let { CurrencyValue.usd(it) } })
  var priceEtched: CurrencyValue? by CardPrints.priceEtched.transform({ it?.value }, { it?.let { CurrencyValue.usd(it) } })

  fun prices(finish: Finish): CurrencyValue? =
    when (finish) {
      Finish.Normal -> priceNormal
      Finish.Foil -> priceFoil
      Finish.Etched -> priceEtched
    }

  val lowestPrice: CurrencyValue?
    get() = Finish.entries.mapNotNull { prices(it) }.minOrNull()

  var promoType: Set<String> by CardPrints.promoType.transform({ it.toList() }, { it.toSet() })

  val possessions by CardPossession referrersOn CardPossessions

  override fun compareTo(other: CardPrint): Int =
    set.compareToNullable(other.set)
      ?: compareCollectorNumberNullable(collectorNumber, other.collectorNumber)
      ?: 0

  override fun toString(): String = "[${set.code}-$collectorNumber] $name"

  override val baseVariantCard: CardPrint
    get() = this

  override val variantType: CardPrintVariant.Type?
    get() = null
}
