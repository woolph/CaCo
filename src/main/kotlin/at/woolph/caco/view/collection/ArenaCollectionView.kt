package at.woolph.caco.view.collection

import at.woolph.caco.importer.collection.importArenaCollection
import javafx.scene.control.ToolBar
import tornadofx.action
import tornadofx.button

class ArenaCollectionView: CollectionView(COLLECTION_SETTINGS) {
	companion object {
		val COLLECTION_SETTINGS = CollectionSettings(4, 0, 4,
				{ it.cards.any { it.arenaId != null } },
				{ it.arenaPossessions.count() },
				{ it.arenaPossessions.count() })
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
