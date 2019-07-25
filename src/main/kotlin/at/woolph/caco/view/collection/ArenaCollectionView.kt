package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.importer.collection.importArenaCollection
import javafx.scene.control.ToolBar
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.action
import tornadofx.button
import tornadofx.observable

class ArenaCollectionView: CollectionView() {
    override val cardPossesionTargtNonPremium get() = 4
    override val cardPossesionTargtPremium get() = 0

    override fun Card.getPossesionsNonPremium() = this.arenaPossessions?.singleOrNull()?.count ?: 0
    override fun Card.getPossesionsPremium() = 0

    override fun Card.filterView(): Boolean = arenaId != null

    override fun getRelevantSets() = transaction {
        CardSet.all().toList().filter { it.cards.any { it.arenaId != null } }.observable().sorted { t1: CardSet, t2: CardSet ->
            -t1.dateOfRelease.compareTo(t2.dateOfRelease)
        }
    }

    override fun ToolBar.addFeatureButtons() {
        button("Import Collection") {
            action {
                importArenaCollection()
            }
        }
    }
}
