/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.decks

import at.woolph.caco.datamodel.decks.DeckCard.Companion.transform
import java.util.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object DeckCards : IntIdTable() {
  val build = reference("build", Builds).index()
  val name = varchar("archetypeName", length = 256).index()
  val place = enumeration("place", Place::class).index()
  val count = integer("count").index()
  val roles =
      long("roles")
          .index()
          .default(0L)
          .transform(
              { roles: EnumSet<CardRole> ->
                roles.fold(0L) { acc, cr2 -> acc or (1L shl cr2.ordinal) }
              },
              { roles: Long ->
                CardRole.values()
                    .filter { ((1L shl it.ordinal) and roles) != 0L }
                    .fold(EnumSet.noneOf(CardRole::class.java)) { acc, cardRole ->
                      acc.add(cardRole)
                      acc
                    }
              },
          )
  val comment = text("comment").nullable()
}

class DeckCard(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<DeckCard>(DeckCards)

  var build by Build referencedOn DeckCards.build
  var name by DeckCards.name
  var place by DeckCards.place
  var count by DeckCards.count
  var roles by DeckCards.roles
  var comment by DeckCards.comment
}
