package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import javafx.beans.property.Property
import tornadofx.ItemViewModel

open class CardModel(card: Card): ItemViewModel<Card>(card), Comparable<CardModel>{
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

	val nonfoilAvailable = bind(Card::nonfoilAvailable)
	val foilAvailable = bind(Card::foilAvailable)
	val fullArt = bind(Card::fullArt)
	val extendedArt = bind(Card::extendedArt)
	val specialDeckRestrictions: Property<Int?> = bind(Card::specialDeckRestrictions, defaultValue = null)


	override fun compareTo(other: CardModel): Int {
		if (set.value != other.set.value) {
			return set.value.dateOfRelease.compareTo(other.set.value.dateOfRelease)
		}
		return numberInSet.value.compareTo(other.numberInSet.value)
	}
}
