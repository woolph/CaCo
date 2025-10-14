/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVariant
import at.woolph.caco.datamodel.sets.Finish
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.count
import java.time.Instant
import java.util.UUID
import kotlin.toUInt

data class CardCollectionItemId(
    val card: Card,
    val finish: Finish,
    val language: CardLanguage,
    val condition: CardCondition,
    val variantType: CardVariant.Type? = null,
) {
    val actualScryfallId: UUID =
        card.getActualScryfallId(variantType).getOrNull() ?: throw IllegalArgumentException("$card does not exist in $variantType")
}

data class CardCollectionItem(
    val quantity: UInt,
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
            CardPossessions
                .select(
                    CardPossessions.id.count(),
                    CardPossessions.card,
                    CardPossessions.finish,
                    CardPossessions.language,
                    CardPossessions.condition,
                    CardPossessions.dateOfAddition,
                    CardPossessions.variantType,
                    CardPossessions.purchasePrice,
                ).where(whereClause)
                .groupBy(
                    CardPossessions.card,
                    CardPossessions.finish,
                    CardPossessions.language,
                    CardPossessions.condition,
                    CardPossessions.dateOfAddition,
                    CardPossessions.variantType,
                    CardPossessions.purchasePrice,
                ).map { record ->
                    CardCollectionItem(
                        quantity = record[CardPossessions.id.count()].toUInt(),
                        cardCollectionItemId =
                            CardCollectionItemId(
                                Card[record[CardPossessions.card]],
                                finish = record[CardPossessions.finish],
                                language = record[CardPossessions.language],
                                condition = record[CardPossessions.condition],
                                variantType = record[CardPossessions.variantType],
                            ),
                        purchasePrice = record[CardPossessions.purchasePrice],
                        dateAdded = record[CardPossessions.dateOfAddition],
                    )
                }.filter(CardCollectionItem::isNotEmpty)
    }
}

fun Iterable<CardPossession>.asCardCollectionItems(): Iterable<CardCollectionItem> =
    groupBy {
        Triple(
            CardCollectionItemId(
                card = it.card,
                finish = it.finish,
                language = it.language,
                condition = it.condition,
                variantType = it.variantType,
            ),
            it.purchasePrice,
            it.dateOfAddition,
        )
    }.mapValues { (_, cardPossessions) -> cardPossessions.count() }
        .map { (cardCollectionItemIdAndPrice, quantity) ->
            val (cardCollectionItemId, purchasePrice, dateOfAddition) = cardCollectionItemIdAndPrice
            CardCollectionItem(
                quantity.toUInt(),
                cardCollectionItemId,
                dateAdded = dateOfAddition,
                purchasePrice = purchasePrice,
            )
        }
