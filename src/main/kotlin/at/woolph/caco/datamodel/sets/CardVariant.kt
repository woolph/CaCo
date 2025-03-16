package at.woolph.caco.datamodel.sets

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import java.util.*

object CardVariants : IdTable<UUID>() {
    override val id = uuid("scryfallId").entityId()
    override val primaryKey = PrimaryKey(id)

    val original = reference("original", Cards).index()
    val type = enumeration<CardVariant.Type>("type").index()

    init {
        uniqueIndex(original, type)
    }
}

class CardVariant(id: EntityID<UUID>) : UUIDEntity(id), CardRepresentation {
    companion object : UUIDEntityClass<CardVariant>(CardVariants)

    val scryfallId: UUID get() = id.value
    override var baseVariantCard by Card referencedOn CardVariants.original
    override var variantType by CardVariants.type

    override fun toString(): String = "[${baseVariantCard.set.code}-${baseVariantCard.collectorNumber}] ${baseVariantCard.name} $variantType"

    enum class Type {
        /** a version of the card with the planeswalker symbol in the bottom right corner usually being from "The List" */
        TheList,
        /** a version of the card with a date stamp usually contained in prerelease packs */
        PrereleaseStamped,
        /** a version of the card with a planeswalker stamp usually contained in promo packs */
        PromopackStamped,
    }
}
