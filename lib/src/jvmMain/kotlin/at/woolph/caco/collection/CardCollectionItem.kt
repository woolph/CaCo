/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.CardPrint
import at.woolph.caco.datamodel.sets.CardPrintVariant
import at.woolph.caco.datamodel.sets.Finish
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.toUInt
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.jdbc.select

data class CardCollectionItemId(
    val cardPrint: CardPrint,
    val finish: Finish,
    val language: CardLanguage,
    val condition: CardCondition,
    val variantType: CardPrintVariant.Type? = null,
) {
  val actualScryfallId: Uuid =
      cardPrint.getActualScryfallId(variantType).getOrNull()
          ?: throw IllegalArgumentException("$cardPrint does not exist in $variantType")
}

data class CardCollectionItem(
    val quantity: UInt,
    val cardCollectionItemId: CardCollectionItemId,
    val dateAdded: Instant = Clock.System.now(),
    val purchasePrice: Double? = null,
) {
  fun addToCollection() {
    repeat(quantity.toInt()) {
      CardPossession.new {
        this.cardPrint = cardCollectionItemId.cardPrint
        this.language = cardCollectionItemId.language
        this.condition = cardCollectionItemId.condition
        this.finish = cardCollectionItemId.finish
        this.variantType = cardCollectionItemId.variantType
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
                CardPossessions.cardPrint,
                CardPossessions.finish,
                CardPossessions.language,
                CardPossessions.condition,
                CardPossessions.dateOfAddition,
                CardPossessions.variantType,
                CardPossessions.purchasePrice,
            )
            .where(whereClause)
            .groupBy(
                CardPossessions.cardPrint,
                CardPossessions.finish,
                CardPossessions.language,
                CardPossessions.condition,
                CardPossessions.dateOfAddition,
                CardPossessions.variantType,
                CardPossessions.purchasePrice,
            )
            .map { record ->
              CardCollectionItem(
                  quantity = record[CardPossessions.id.count()].toUInt(),
                  cardCollectionItemId =
                      CardCollectionItemId(
                          cardPrint = CardPrint[record[CardPossessions.cardPrint]],
                          finish = record[CardPossessions.finish],
                          language = record[CardPossessions.language],
                          condition = record[CardPossessions.condition],
                          variantType = record[CardPossessions.variantType],
                      ),
                  purchasePrice = record[CardPossessions.purchasePrice],
                  dateAdded = record[CardPossessions.dateOfAddition],
              )
            }
            .filter(CardCollectionItem::isNotEmpty)
  }
}

fun Iterable<CardPossession>.asCardCollectionItems(): Iterable<CardCollectionItem> =
    groupBy {
          Triple(
              CardCollectionItemId(
                  cardPrint = it.cardPrint,
                  finish = it.finish,
                  language = it.language,
                  condition = it.condition,
                  variantType = it.variantType,
              ),
              it.purchasePrice,
              it.dateOfAddition,
          )
        }
        .mapValues { (_, cardPossessions) -> cardPossessions.count() }
        .map { (cardCollectionItemIdAndPrice, quantity) ->
          val (cardCollectionItemId, purchasePrice, dateOfAddition) = cardCollectionItemIdAndPrice
          CardCollectionItem(
              quantity.toUInt(),
              cardCollectionItemId,
              dateAdded = dateOfAddition,
              purchasePrice = purchasePrice,
          )
        }
