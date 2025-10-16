/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.decks

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import java.net.URI

object DeckArchetypes : IntIdTable() {
    val name = varchar("name", length = 256).index()
    val format = enumeration("format", Format::class).index()
    val priority = enumeration("priority", Priority::class).index().default(Priority.MaybeCool)
    val originator = varchar("originator", length = 256).index().nullable()
    val link = varchar("link", length = 256).nullable()
    val comment = text("comment").nullable()

    val archived = bool("archived").default(false)
}

class DeckArchetype(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<DeckArchetype>(DeckArchetypes)

    var name by DeckArchetypes.name
    var format by DeckArchetypes.format
    var priority by DeckArchetypes.priority
    var originator by DeckArchetypes.originator
    var link by DeckArchetypes.link.transform({ it?.toString() }, { it?.let { URI(it) } })
    var comment by DeckArchetypes.comment

    var archived by DeckArchetypes.archived

    val builds by Build referrersOn Builds.archetype
}
