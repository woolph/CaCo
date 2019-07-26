package at.woolph.caco.datamodel.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Foil
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.CurrentDateTime

object CardPossessions : IntIdTable() {
    val card = reference("card", Cards).index()
    //val language = varchar("language", length = 2).index()
    val language = enumeration("language", CardLanguage::class).default(CardLanguage.UNKNOWN).index()
    val condition = enumeration("condition", CardCondition::class).default(CardCondition.UNKNOWN).index()
    val foil = enumeration("foil", Foil::class).default(Foil.NONFOIL).index()
    val datetimeOfAddition = datetime("datetimeOfAddition").index().defaultExpression(CurrentDateTime())
}

class CardPossession(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardPossession>(CardPossessions)

    var card by Card referencedOn CardPossessions.card
    var language by CardPossessions.language
    var condition by CardPossessions.condition
    var foil by CardPossessions.foil
    var datetimeOfAddition by CardPossessions.datetimeOfAddition
    //var prereleasePromo by CardPossessions.prereleasePromo
}
