/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.collection

import at.woolph.caco.datamodel.sets.CardPrint
import at.woolph.caco.datamodel.sets.CardPrintVariant
import at.woolph.caco.datamodel.sets.CardPrints
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.utils.currency.CurrencyValue
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object CardPossessions : IntIdTable() {
  val cardPrint = reference("cardPrint", CardPrints).index()
  val dateOfAddition = timestamp("dateOfAddition").index().defaultExpression(CurrentTimestamp)
  val language = enumeration<CardLanguage>("language").default(CardLanguage.UNKNOWN).index()
  val condition = enumeration<CardCondition>("condition").default(CardCondition.UNKNOWN).index()
  val finish = enumeration<Finish>("finish").default(Finish.Normal).index()
  val variantType = enumeration<CardPrintVariant.Type>("variantType").nullable().index()

  val purchasePrice = double("purchasePrice").nullable()
  /**  */
  val tradeLock = bool("tradeLock").default(false).index()
  val location =
      varchar("location", length = 128)
          .index()
          .nullable() // here you can mark where the card is to be found (collection binder, trade
  // binder, deck, lent to someone, storage box, ...)
}

class CardPossession(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<CardPossession>(CardPossessions) {
    fun find(cardPrint: CardPrint) = find { CardPossessions.cardPrint eq cardPrint.id }
  }

  var cardPrint by CardPrint referencedOn CardPossessions.cardPrint
  var dateOfAddition by CardPossessions.dateOfAddition
  var language by CardPossessions.language
  var condition by CardPossessions.condition
  var finish by CardPossessions.finish
  var variantType by CardPossessions.variantType
  var tradeLock by CardPossessions.tradeLock
  var location by CardPossessions.location
  var purchasePrice by CardPossessions.purchasePrice

  val price: CurrencyValue?
    get() =
        cardPrint
            .prices(finish)
            ?.times(
                when (condition) {
                  CardCondition.UNKNOWN -> 0.0
                  CardCondition.NEAR_MINT -> 1.0
                  CardCondition.EXCELLENT -> 0.9
                  CardCondition.GOOD -> 0.7
                  CardCondition.PLAYED -> 0.5
                  CardCondition.POOR -> 0.3
                }
            )

  override fun toString(): String = "$cardPrint $finish $language $condition $variantType"
}
