/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import arrow.core.Either
import arrow.core.raise.either
import java.util.UUID

interface CardRepresentation {
    val baseVariantCard: Card
    val variantType: CardVariant.Type?

    operator fun component1(): Card = baseVariantCard

    operator fun component2(): CardVariant.Type? = variantType

    companion object {
        fun findByScryfallId(id: UUID): CardRepresentation? = Card.findById(id) ?: CardVariant.findById(id)
    }

    fun getActualScryfallId(variantType: CardVariant.Type?): Either<Throwable, UUID> =
        either {
            if (variantType == null) return@either baseVariantCard.scryfallId
            return@either baseVariantCard.variants.singleOrNull { it.variantType == variantType }?.scryfallId
                ?: raise(Exception("variant $variantType not found for card ${baseVariantCard.scryfallId}"))
        }
}
