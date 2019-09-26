package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.importer.collection.importArenaCollection
import javafx.scene.control.ToolBar
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.action
import tornadofx.button
import tornadofx.observable

class ArenaCollectionView: CollectionView(COLLECTION_SETTINGS) {
	companion object {
		val COLLECTION_SETTINGS = CollectionSettings(4, 0,
				{ it.cards.any { it.arenaId != null } },
				{ it.possessions.filter { it.language == CardLanguage.ENGLISH && !it.foil.isFoil }.count() },
				{ it.possessions.filter { it.language == CardLanguage.ENGLISH && it.foil.isFoil }.count() })
	}

    override fun CardPossessionModel.filterView(): Boolean = arenaId != null

    override fun ToolBar.addFeatureButtons() {
        button("Import Collection") {
            action {
                importArenaCollection()
            }
        }
    }
}
