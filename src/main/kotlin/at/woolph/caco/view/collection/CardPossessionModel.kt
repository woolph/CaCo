package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import javafx.beans.property.SimpleIntegerProperty
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.stringBinding
import kotlin.math.min

class CardPossessionModel(card: Card, val collectionsSettings: CollectionSettings): CardModel(card) {
	val possessionNonPremium = SimpleIntegerProperty(0)
	val possessionPremium = SimpleIntegerProperty(0)

	val possessionTotal = possessionNonPremium.add(possessionPremium)!!
	val collectionCompletion = stringBinding(possessionNonPremium, possessionPremium) {
		val ownedCount = min(collectionsSettings.cardPossesionTargtNonPremium, possessionNonPremium.get())
		val ownedCountFoil = min(collectionsSettings.cardPossesionTargtPremium, possessionPremium.get())

		val plus = if(possessionNonPremium.get()>collectionsSettings.cardPossesionTargtNonPremium) CollectionView.ICON_REDUNDANT_OWNED_CARD else " "
		val plusFoil = if(possessionPremium.get()>collectionsSettings.cardPossesionTargtPremium) CollectionView.ICON_REDUNDANT_OWNED_CARD else " "

		return@stringBinding "${CollectionView.CARD_IN_POSSESION.repeat(ownedCount)}${CollectionView.CARD_NOT_IN_POSSESION.repeat(collectionsSettings.cardPossesionTargtNonPremium-ownedCount)}$plus\t${CollectionView.FOIL_IN_POSSESION.repeat(ownedCountFoil)}${CollectionView.FOIL_NOT_IN_POSSESION.repeat(collectionsSettings.cardPossesionTargtPremium-ownedCountFoil)}$plusFoil"
	}
	val completedNonPremium = possessionNonPremium.greaterThanOrEqualTo(collectionsSettings.cardPossesionTargtNonPremium)!!
	val completedPremium = possessionPremium.greaterThanOrEqualTo(collectionsSettings.cardPossesionTargtPremium)!!

	init {
		updatePossessions()
	}

	fun updatePossessions() {
		transaction {
			possessionNonPremium.set(collectionsSettings.possessionFilterNonPremium(item))
			possessionPremium.set(collectionsSettings.possessionFilterPremium(item))
		}
	}

	fun getPaperPossessions(language: CardLanguage, condition: CardCondition)
			= transaction { item.possessions.filter { it.language == language && it.condition == condition }.count() }
}
