package at.woolph.caco.datamodel.decks

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object DeckArchetypes : IntIdTable() {
    val name = varchar("name", length = 256).index()
    val format = enumeration("format", Format::class).index()
    val comment = text("comment").nullable()

	val archived = bool("archived").default(false)
}

class DeckArchetype(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DeckArchetype>(DeckArchetypes)

    var name by DeckArchetypes.name
    var format by DeckArchetypes.format
    var comment by DeckArchetypes.comment

	var archived by DeckArchetypes.archived

    val variants by DeckVariant referrersOn DeckVariants.archetype
}
