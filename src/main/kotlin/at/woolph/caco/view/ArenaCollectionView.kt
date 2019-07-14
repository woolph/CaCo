package at.woolph.caco.view

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Set
import at.woolph.caco.importer.collection.importArenaCollection
import at.woolph.pdf.columns
import at.woolph.pdf.drawText
import at.woolph.pdf.frame
import at.woolph.pdf.page
import javafx.scene.control.ToolBar
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
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
        Set.all().toList().filter { it.cards.any { it.arenaId != null } }.observable().sorted { t1: Set, t2: Set ->
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
