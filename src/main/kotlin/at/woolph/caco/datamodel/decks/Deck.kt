package at.woolph.caco.datamodel.decks

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Decks : IntIdTable() {
    val name = varchar("name", length = 256).index()
    val format = enumeration("format", Format::class).index()
    val comment = text("comment").nullable()

    val archived = bool("archived").default(false)
}

class Deck(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Deck>(Decks)

    var name by Decks.name
    var format by Decks.format
    var comment by Decks.comment

    var archived by Decks.archived

    val variants by Variant referrersOn Variants.deck
}
