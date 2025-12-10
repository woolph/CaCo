/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import at.woolph.utils.exposed.UuidEntity
import at.woolph.utils.exposed.UuidEntityClass
import at.woolph.utils.exposed.ktUuid
import java.util.*
import org.jetbrains.exposed.v1.dao.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import kotlin.uuid.Uuid

object CardVariants : IdTable<Uuid>() {
  override val id = ktUuid("scryfallId").entityId()
  override val primaryKey = PrimaryKey(id)

  val original = reference("original", Cards).index()
  val type = enumeration<CardVariant.Type>("type").index()

  init {
    uniqueIndex(original, type)
  }
}

class CardVariant(id: EntityID<Uuid>) : UuidEntity(id), CardRepresentation {
  companion object : UuidEntityClass<CardVariant>(CardVariants)

  val scryfallId: Uuid
    get() = id.value

  override var baseVariantCard by Card referencedOn CardVariants.original
  override var variantType by CardVariants.type

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
