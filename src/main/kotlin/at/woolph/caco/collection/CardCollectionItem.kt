package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.count
import java.time.LocalDate
import kotlin.toUInt

data class CardCollectionItemId(
  val card: Card,
  val foil: Boolean,
  val language: CardLanguage,
  val condition: CardCondition,
  val stampPrereleaseDate: Boolean = false,
  val stampPlaneswalkerSymbol: Boolean = false,
)
data class CardCollectionItem(
  val quantity : UInt,
  val cardCollectionItemId: CardCollectionItemId,
  val dateAdded: LocalDate = LocalDate.now(),
) {
  fun addToCollection() {
    repeat(quantity.toInt()) {
      CardPossession.new {
        this.card = cardCollectionItemId.card
        this.language = cardCollectionItemId.language
        this.condition = cardCollectionItemId.condition
        this.foil = cardCollectionItemId.foil
        this.stampPrereleaseDate = cardCollectionItemId.stampPrereleaseDate
        this.stampPlaneswalkerSymbol = cardCollectionItemId.stampPlaneswalkerSymbol
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
      CardPossessions.stampPrereleaseDate,
      CardPossessions.stampPlaneswalkerSymbol,
      )
      .where(whereClause)
    .groupBy(CardPossessions.card, CardPossessions.foil, CardPossessions.language, CardPossessions.condition, CardPossessions.dateOfAddition)
    .map { record ->
      CardCollectionItem(
        quantity = record[CardPossessions.id.count()].toUInt(),
        cardCollectionItemId = CardCollectionItemId(
          Card[record[CardPossessions.card]],
          foil = record[CardPossessions.foil],
          language = record[CardPossessions.language],
          condition = record[CardPossessions.condition],
          stampPrereleaseDate = record[CardPossessions.stampPrereleaseDate],
          stampPlaneswalkerSymbol = record[CardPossessions.stampPlaneswalkerSymbol],
        ),
        dateAdded = record[CardPossessions.dateOfAddition],
      )
    }.filter(CardCollectionItem::isNotEmpty)
  }
}

fun Iterable<CardPossession>.asCardCollectionItems(): Iterable<CardCollectionItem> =
  groupBy { CardCollectionItemId(
    card = it.card,
    foil = it.foil,
    language = it.language,
    condition = it.condition,
    stampPrereleaseDate = it.stampPrereleaseDate,
    stampPlaneswalkerSymbol = it.stampPlaneswalkerSymbol
  ) }.mapValues { (_, cardPossessions) -> cardPossessions.count() }
    .map { (cardCollectionItemId, quantity) ->
      CardCollectionItem(
        quantity.toUInt(),
        cardCollectionItemId,
      )
    }