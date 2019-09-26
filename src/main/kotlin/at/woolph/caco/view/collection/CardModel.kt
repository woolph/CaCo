package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.Card
import tornadofx.ItemViewModel

open class CardModel(card: Card): ItemViewModel<Card>(card) {
	val set = bind(Card::set)
	val numberInSet = bind(Card::numberInSet)
	val name = bind(Card::name)
	val nameDE = bind(Card::nameDE)
	val arenaId = bind(Card::arenaId)
	val rarity = bind(Card::rarity)
	val promo = bind(Card::promo)
	val token = bind(Card::token)
	val image = bind(Card::image)
	val cardmarketUri = bind(Card::cardmarketUri)
}
