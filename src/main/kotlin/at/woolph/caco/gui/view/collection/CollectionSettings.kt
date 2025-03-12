package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.ScryfallCardSet

class CollectionSettings(val cardPossesionTargtNonPremium: Int,
						 val cardPossesionTargtPremium: Int,
						 val cardPossessionTargetNonPremiumNoDeckRestriction: Int,
						 val cardSetFilter: (ScryfallCardSet) -> Boolean,
						 val possessionFilterNonPremium: (Card) -> Int,
						 val possessionFilterPremium: (Card) -> Int)