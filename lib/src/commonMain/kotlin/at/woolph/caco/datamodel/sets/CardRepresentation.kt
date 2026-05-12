/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import arrow.core.Either
import arrow.core.raise.either
import kotlin.uuid.Uuid

interface CardRepresentation {
  val baseVariantCard: CardPrint
  val variantType: CardPrintVariant.Type?

  operator fun component1(): CardPrint = baseVariantCard

  operator fun component2(): CardPrintVariant.Type? = variantType

  companion object {
    fun findByScryfallId(scryfallId: Uuid): CardRepresentation? =
        CardPrint.findById(scryfallId) ?: CardPrintVariant.findById(scryfallId)
  }

  fun getActualScryfallId(variantType: CardPrintVariant.Type?): Either<Throwable, Uuid> = either {
    if (variantType == null) return@either baseVariantCard.scryfallId
    return@either baseVariantCard.variants
        .singleOrNull { it.variantType == variantType }
        ?.scryfallId
        ?: raise(Exception("variant $variantType not found for card ${baseVariantCard.scryfallId}"))
  }
}
