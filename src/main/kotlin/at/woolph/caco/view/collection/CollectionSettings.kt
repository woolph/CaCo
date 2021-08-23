package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet

class CollectionSettings(val cardPossesionTargtNonPremium: Int,
							  val cardPossesionTargtPremium: Int,
							  val cardPossessionTargetNonPremiumNoDeckRestriction: Int,
							  val cardSetFilter: (CardSet) -> Boolean,
							  val possessionFilterNonPremium: (Card) -> Int,
							  val possessionFilterPremium: (Card) -> Int)