package at.woolph.caco.datamodel.sets

import at.woolph.caco.datamodel.collection.ArenaCardPossession
import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import org.jetbrains.exposed.dao.*
import java.net.URI
import java.util.*

object Cards : IdTable<UUID>() {
    override val id = uuid("id").entityId()

    val set = reference("set", ScryfallCardSets).index()
    val numberInSet = varchar("number", length = 10).index()
    val name = varchar("name", length = 256).index()
    val nameDE = varchar("nameDE", length = 256).index().nullable()
    val arenaId = integer("arenaId").nullable().index()
    val rarity = enumeration("rarity", Rarity::class).index()
    val promo = bool("promo").default(false).index()
    val token = bool("token").default(false).index()
    val image = varchar("imageURI", length = 256).nullable()
    val cardmarketUri = varchar("cardmarketUri", length = 256).nullable()

    val extra = bool("extra").default(false)
    val nonfoilAvailable = bool("nonfoilAvailable").default(true)
    val foilAvailable = bool("foilAvailable").default(true)
    val fullArt = bool("fullArt").default(false)
    val extendedArt = bool("extendedArt").default(false)
    val specialDeckRestrictions = integer("specialDeckRestrictions").nullable()

    val manaCost = varchar("manaCost", length = 256).nullable()
    val type = varchar("type", length = 256).nullable()

    val price = double("price").nullable()
    val priceFoil = double("priceFoil").nullable()
}

class Card(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Card>(Cards)

    var set by ScryfallCardSet referencedOn Cards.set
    var numberInSet by Cards.numberInSet
    var name by Cards.name
    var nameDE by Cards.nameDE
    var arenaId by Cards.arenaId
    var rarity by Cards.rarity
    var promo by Cards.promo
    var token by Cards.token
    var image by Cards.image.transform({ it?.toString() }, { it?.let { URI(it) } })
    var cardmarketUri by Cards.cardmarketUri.transform({ it?.toString() }, { it?.let { URI(it) } })

    var extra by Cards.extra
    var nonfoilAvailable by Cards.nonfoilAvailable
    var foilAvailable by Cards.foilAvailable
    var fullArt by Cards.fullArt
    var extendedArt by Cards.extendedArt
    var specialDeckRestrictions by Cards.specialDeckRestrictions

    var manaCost by Cards.manaCost
    var type by Cards.type

    var price by Cards.price
    var priceFoil by Cards.priceFoil

    val possessions by CardPossession referrersOn CardPossessions.card
    val arenaPossessions by ArenaCardPossession referrersOn ArenaCardPossessions.card
}
