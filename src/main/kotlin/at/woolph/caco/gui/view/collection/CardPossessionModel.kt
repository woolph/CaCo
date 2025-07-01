package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Finish
import javafx.beans.binding.Bindings
import javafx.beans.binding.IntegerBinding
import javafx.beans.property.SimpleIntegerProperty
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.integerBinding
import tornadofx.stringBinding
import kotlin.math.min

open class CardPossessionModel(card: Card, val collectionsSettings: CollectionSettings): CardModel(card) {

	val possessionPremiumTarget: IntegerBinding = integerBinding(extendedArt, nonfoilAvailable, foilAvailable) {
		if (extendedArt.value || !foilAvailable.value) {
			0
		} else {
			if (!nonfoilAvailable.value && collectionsSettings.cardPossesionTargtPremium < 1) {
				1
			} else {
				collectionsSettings.cardPossesionTargtPremium
			}
		}
	}

	val possessionNonPremiumTarget: IntegerBinding = integerBinding(extendedArt, specialDeckRestrictions, nonfoilAvailable, possessionPremiumTarget) {
		if (extendedArt.value || !nonfoilAvailable.value) {
			0
		} else {
			specialDeckRestrictions.value?.let { if (it > 0) min(it, collectionsSettings.cardPossessionTargetNonPremiumNoDeckRestriction) else null } ?: collectionsSettings.cardPossesionTargtNonPremium
		}
	}

	val possessionNonPremium = SimpleIntegerProperty(0)
	val possessionPremium = SimpleIntegerProperty(0)

	val possessionTotal = possessionNonPremium.add(possessionPremium)!!
	val collectionCompletion = stringBinding(possessionNonPremium, possessionPremium, possessionNonPremiumTarget, possessionPremiumTarget) {
		val ownedCount = min(possessionNonPremiumTarget.value, possessionNonPremium.get())
		val ownedCountFoil = min(possessionPremiumTarget.value, possessionPremium.get())

		val plus = if(possessionNonPremium.get()>possessionNonPremiumTarget.value) CollectionView.ICON_REDUNDANT_OWNED_CARD else " "
		val plusFoil = if(possessionPremium.get()>possessionPremiumTarget.value) CollectionView.ICON_REDUNDANT_OWNED_CARD else " "

		return@stringBinding "${CollectionView.NONFOIL_IN_POSSESION.repeat(ownedCount)}${CollectionView.NONFOIL_NOT_IN_POSSESION.repeat(possessionNonPremiumTarget.value-ownedCount)}$plus\t${CollectionView.FOIL_IN_POSSESION.repeat(ownedCountFoil)}${CollectionView.FOIL_NOT_IN_POSSESION.repeat(possessionPremiumTarget.value-ownedCountFoil)}$plusFoil"
	}
	val completedNonPremium = possessionNonPremium.greaterThanOrEqualTo(possessionNonPremiumTarget)!!
	val completedPremium = possessionPremium.greaterThanOrEqualTo(possessionPremiumTarget)!!
	val completed = Bindings.and(completedNonPremium, completedPremium)


	init {
		updatePossessions()
	}

	fun updatePossessions() {
		transaction {
			possessionNonPremium.set(collectionsSettings.possessionFilterNonPremium(item))
			possessionPremium.set(collectionsSettings.possessionFilterPremium(item))
		}
	}

	data class Possession(val count: Int, val foilCount: Int) {
		fun asString() : String = when {
			foilCount > 0 -> "$count*$foilCount"
			count > 0  -> "$count"
			else -> ""
		}

		companion object {
			fun fold(a: Possession, b: Possession) =
				Possession(a.count+b.count, a.foilCount+b.foilCount)
		}
	}
	fun getPaperPossessionsMap(languages: Set<CardLanguage>, conditions: Set<CardCondition>): Map<CardLanguage, Map<CardCondition, Possession>> {
		val possessions = transaction { item.possessions.toList() }

		return languages.associateWith { language ->
			conditions.associateWith { condition ->
				val count = possessions.count { it.language == language && (condition == null || it.condition == condition) && it.finish == Finish.Normal }
				val foilCount = possessions.count { it.language == language && (condition == null || it.condition == condition) && it.finish != Finish.Normal }
				Possession(count, foilCount)
			}
		}
	}
}
