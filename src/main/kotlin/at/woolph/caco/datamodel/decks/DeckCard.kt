package at.woolph.caco.datamodel.decks

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object DeckCards : IntIdTable() {
    val build = reference("build", Builds).index()
    val name = varchar("name", length = 256).index()
    val place = enumeration("place", Place::class).index()
    val count = integer("count").index()
    val comment = text("comment").nullable()
}

class DeckCard(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DeckCard>(DeckCards)

    var build by Build referencedOn DeckCards.build
    var name by DeckCards.name
    var place by DeckCards.place
    var count by DeckCards.count
    var comment by DeckCards.comment
}
