/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object CardPrintVariants : IdTable<Uuid>() {
  override val id = uuid("scryfallId").entityId()
  override val primaryKey = PrimaryKey(id)

  val original = reference("original", CardPrints).index()
  val type = enumeration<CardPrintVariant.Type>("type").index()

  init {
    uniqueIndex(original, type)
  }
}

class CardPrintVariant(id: EntityID<Uuid>) : UuidEntity(id), CardRepresentation {
  companion object : UuidEntityClass<CardPrintVariant>(CardPrintVariants)

  val scryfallId: Uuid
    get() = id.value

  override var baseVariantCard by CardPrint referencedOn CardPrintVariants.original
  override var variantType by CardPrintVariants.type

  override fun toString(): String =
      "[${baseVariantCard.set.code}-${baseVariantCard.collectorNumber}] ${baseVariantCard.name} $variantType"

  enum class Type {
    /**
     * a version of the card with the planeswalker symbol in the bottom right corner usually being
     * from "The List"
     */
    TheList,
    /** a version of the card with a date stamp usually contained in prerelease packs */
    PrereleaseStamped,
    /** a version of the card with a planeswalker stamp usually contained in promo packs */
    PromopackStamped,
  }
}
