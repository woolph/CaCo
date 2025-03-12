package at.woolph.caco.datamodel.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVersion
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object CardPossessions : IntIdTable() {
    val card = reference("card", Cards).index()
	val dateOfAddition = timestamp("dateOfAddition").index().defaultExpression(CurrentTimestamp)
    val language = enumeration<CardLanguage>("language").default(CardLanguage.UNKNOWN).index()
    val condition = enumeration<CardCondition>("condition").default(CardCondition.UNKNOWN).index()
    val foil = bool("foil").default(false).index()
    val cardVersion = enumeration<CardVersion>("cardVersion").default(CardVersion.OG).index()

    val purchasePrice = double("purchasePrice").nullable()
	/**
	 *
	 */
	val tradeLock = bool("tradeLock").default(false).index()
	val location = varchar("location", length = 128).index().nullable() // here you can mark where the card is to be found (collection binder, trade binder, deck, lent to someone, storage box, ...)
}

class CardPossession(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardPossession>(CardPossessions) {
      fun find(card: Card) = find { CardPossessions.card eq card.id }
    }

    var card by Card referencedOn CardPossessions.card
	var dateOfAddition by CardPossessions.dateOfAddition
    var language by CardPossessions.language
    var condition by CardPossessions.condition
    var foil by CardPossessions.foil
    var cardVersion by CardPossessions.cardVersion
    var tradeLock by CardPossessions.tradeLock
    var location by CardPossessions.location
    var purchasePrice by CardPossessions.purchasePrice
}
