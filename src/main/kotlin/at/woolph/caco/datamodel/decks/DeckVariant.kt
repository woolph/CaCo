package at.woolph.caco.datamodel.decks

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.net.URI

object DeckVariants : IntIdTable() {
    val archetype = reference("archetype", DeckArchetypes).index()

    val parentVariant = reference("parentVariant", DeckVariants).nullable()

    val name = varchar("name", length = 256).index().nullable()
    val originator = varchar("originator", length = 256).index().default("")
	val priority = enumeration("priority", Priority::class).index()
    val link = varchar("link", length = 256).nullable()
    val comment = text("comment").nullable()

    val archived = bool("archived").default(false)
}

class DeckVariant(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DeckVariant>(DeckVariants)

    var archetype by DeckArchetype referencedOn DeckVariants.archetype
    var parentVariant by DeckVariant optionalReferencedOn DeckVariants.parentVariant

    var name by DeckVariants.name
    var originator by DeckVariants.originator
    var priority by DeckVariants.priority
    var link by DeckVariants.link.transform({ it?.toString() }, { it?.let { URI(it) } })
    var comment by DeckVariants.comment

    var archived by DeckVariants.archived

    val builds by Build referrersOn Builds.variant
}
