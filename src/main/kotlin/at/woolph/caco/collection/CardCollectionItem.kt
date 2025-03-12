package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVersion
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.count
import java.time.Instant
import java.util.UUID
import kotlin.toUInt

data class CardCollectionItemId(
  val card: Card,
  val foil: Boolean,
  val language: CardLanguage,
  val condition: CardCondition,
  val cardVersion: CardVersion = CardVersion.OG,
) {
  init {
    require(card.getActualScryfallId(cardVersion) != null) {
      "The given card $card has no scryfallId available for the version $cardVersion"
    }
  }

  val actualScryfallId: UUID get() = card.getActualScryfallId(cardVersion)!!
}

data class CardCollectionItem(
  val quantity : UInt,
  val cardCollectionItemId: CardCollectionItemId,
  val dateAdded: Instant = Instant.now(),
  val purchasePrice: Double? = null,
) {
  fun addToCollection() {
    repeat(quantity.toInt()) {
      CardPossession.new {
        this.card = cardCollectionItemId.card
        this.language = cardCollectionItemId.language
        this.condition = cardCollectionItemId.condition
        this.foil = cardCollectionItemId.foil
        this.cardVersion = cardCollectionItemId.cardVersion
        this.purchasePrice = this@CardCollectionItem.purchasePrice
        this.dateOfAddition = this@CardCollectionItem.dateAdded
      }
    }
  }

  fun isNotEmpty(): Boolean = quantity > 0U

  companion object {
    fun getFromDatabase(whereClause: Op<Boolean> = Op.TRUE): List<CardCollectionItem> =
    CardPossessions.select(
      CardPossessions.id.count(),
      CardPossessions.card,
      CardPossessions.foil,
      CardPossessions.language,
      CardPossessions.condition,
      CardPossessions.dateOfAddition,
      CardPossessions.cardVersion,
      CardPossessions.purchasePrice,
      )
      .where(whereClause)
    .groupBy(CardPossessions.card, CardPossessions.foil, CardPossessions.language, CardPossessions.condition, CardPossessions.dateOfAddition, CardPossessions.cardVersion,
      CardPossessions.purchasePrice)
    .map { record ->
      CardCollectionItem(
        quantity = record[CardPossessions.id.count()].toUInt(),
        cardCollectionItemId = CardCollectionItemId(
          Card[record[CardPossessions.card]],
          foil = record[CardPossessions.foil],
          language = record[CardPossessions.language],
          condition = record[CardPossessions.condition],
          cardVersion = record[CardPossessions.cardVersion],
        ),
        purchasePrice = record[CardPossessions.purchasePrice],
        dateAdded = record[CardPossessions.dateOfAddition],
      )
    }.filter(CardCollectionItem::isNotEmpty)
  }
}

fun Iterable<CardPossession>.asCardCollectionItems(): Iterable<CardCollectionItem> =
  groupBy { Triple(CardCollectionItemId(
    card = it.card,
    foil = it.foil,
    language = it.language,
    condition = it.condition,
    cardVersion = it.cardVersion,
  ), it.purchasePrice, it.dateOfAddition) }.mapValues { (_, cardPossessions) -> cardPossessions.count() }
    .map { (cardCollectionItemIdAndPrice, quantity) ->
      val (cardCollectionItemId, purchasePrice, dateOfAddition) = cardCollectionItemIdAndPrice
      CardCollectionItem(
        quantity.toUInt(),
        cardCollectionItemId,
        dateAdded = dateOfAddition,
        purchasePrice = purchasePrice,
      )
    }
