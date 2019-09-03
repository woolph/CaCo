package at.woolph.caco.datamodel.sets

import at.woolph.caco.datamodel.collection.ArenaCardPossession
import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.net.URI

object Cards : IntIdTable() {
    val set = reference("set", CardSets).index()
    val numberInSet = varchar("number", length = 10).index()
    val name = varchar("name", length = 256).index()
    val nameDE = varchar("nameDE", length = 256).index().nullable()
    val arenaId = integer("arenaId").nullable().index()
    val rarity = enumeration("rarity", Rarity::class).index()
    val promo = bool("promo").default(false).index()
    val token = bool("token").default(false).index()
    val image = varchar("imageURI", length = 256).nullable()
    val cardmarketUri = varchar("cardmarketUri", length = 256).nullable()
}

class Card(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Card>(Cards)

    var set by CardSet referencedOn Cards.set
    var numberInSet by Cards.numberInSet
    var name by Cards.name
    var nameDE by Cards.nameDE
    var arenaId by Cards.arenaId
    var rarity by Cards.rarity
    var promo by Cards.promo
    var token by Cards.token
    var image by Cards.image.transform({ it?.toString() }, { it?.let { URI(it) } })
    var cardmarketUri by Cards.cardmarketUri.transform({ it?.toString() }, { it?.let { URI(it) } })

    val possessions by CardPossession referrersOn CardPossessions.card
    val arenaPossessions by ArenaCardPossession referrersOn ArenaCardPossessions.card
}
