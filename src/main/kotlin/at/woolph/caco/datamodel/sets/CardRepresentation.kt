package at.woolph.caco.datamodel.sets

import arrow.core.Either
import arrow.core.raise.either
import java.util.UUID

interface CardRepresentation {
    val card: Card
    val variantType: CardVariant.Type?

    operator fun component1(): Card = card
    operator fun component2(): CardVariant.Type? = variantType

    companion object {
        fun findByScryfallId(id: UUID): CardRepresentation? =
            Card.findById(id) ?: CardVariant.findById(id)
    }

    fun getActualScryfallId(variantType: CardVariant.Type?): Either<Throwable, UUID> = either {
        if (variantType == null) return@either card.scryfallId
        return@either card.variants.singleOrNull { it.variantType == variantType }?.scryfallId
            ?: raise(Exception("variant $variantType not found for card ${card.scryfallId}"))
    }
}
