package at.woolph.caco.datamodel.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object ArenaCardPossessions : IntIdTable() {
    val card = reference("card", Cards).index()
    val count = integer("count").index()
}

class ArenaCardPossession(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArenaCardPossession>(ArenaCardPossessions)

    var card by Card referencedOn ArenaCardPossessions.card
    var count by ArenaCardPossessions.count
}
