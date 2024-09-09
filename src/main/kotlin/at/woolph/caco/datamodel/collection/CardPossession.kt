package at.woolph.caco.datamodel.collection

import at.woolph.caco.datamodel.decks.Builds.default
import at.woolph.caco.datamodel.decks.Builds.index
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object CardPossessions : IntIdTable() {
    val card = reference("card", Cards).index()
	val datetimeOfAddition = datetime("datetimeOfAddition").index().defaultExpression(CurrentDateTime)
    val language = enumeration("language", CardLanguage::class).default(CardLanguage.UNKNOWN).index()
    val condition = enumeration("condition", CardCondition::class).default(CardCondition.UNKNOWN).index()
    val foil = bool("foil").default(false).index()
    val stampPrereleaseDate = bool("stampPrereleaseDate").default(false).index()

	/**
	 * indicates that the card has the metallic planeswalker stamp on the lower right corner of the artwork
	 * (like the cards from promo packs)
	 */
    val stampPlaneswalkerSymbol = bool("stampPlaneswalkerSymbol").default(false).index()

	/**
	 * indicates that the card has the planeswalker symbol in the lower left corner (like the cards from
	 * mystery booster or cards from "The List")
	 */
	val markPlaneswalkerSymbol = bool("markPlaneswalkerSymbol").default(false).index()

	/**
	 *
	 */
	val tradeLock = bool("tradeLock").default(false).index()
	val location = varchar("location", length = 128).index().nullable() // here you can mark where the card is to be found (collection binder, trade binder, deck, lent to someone, storage box, ...)
}

class CardPossession(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardPossession>(CardPossessions)

    var card by Card referencedOn CardPossessions.card
	var datetimeOfAddition by CardPossessions.datetimeOfAddition
    var language by CardPossessions.language
    var condition by CardPossessions.condition
    var foil by CardPossessions.foil
    var stampPrereleaseDate by CardPossessions.stampPrereleaseDate
    var stampPlaneswalkerSymbol by CardPossessions.stampPlaneswalkerSymbol
    var markPlaneswalkerSymbol by CardPossessions.markPlaneswalkerSymbol
    var tradeLock by CardPossessions.tradeLock
    var location by CardPossessions.location
    //var prereleasePromo by CardPossessions.prereleasePromo
}
