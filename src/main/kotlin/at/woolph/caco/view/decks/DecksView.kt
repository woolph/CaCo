package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.*
import at.woolph.caco.datamodel.decks.Deck.Companion.referrersOn
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.importer.sets.*
import at.woolph.caco.view.*
import at.woolph.libs.ktfx.mapBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import kotlin.math.min
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DecksView : View() {
    override val root = BorderPane()

    lateinit var tvDecks: TableView<DeckModel>

    val decks = FXCollections.observableArrayList<DeckModel>()
    val decksFiltered = decks.filtered { it.filterView() }

    fun DeckModel.filterView(): Boolean = true

    fun updateDecks() {
		decks.setAll(transaction {
			Deck.all().map { DeckModel(it) }
        })
    }

    init {
        title = "CaCo"

		updateDecks()

        with(root) {
			left {
				tvDecks = tableview(decksFiltered) {
					hboxConstraints {
						hGrow = Priority.ALWAYS
					}
					vboxConstraints {
						vGrow = Priority.ALWAYS
					}
					column("Name", DeckModel::name) {
						remainingWidth()
					}
					column("Format", DeckModel::format) {
						contentWidth(35.0, useAsMin = true, useAsMax = true)
					}

					setRowFactory {
						object: TableRow<DeckModel>() {
							override fun updateItem(deckModel: DeckModel?, empty: Boolean) {
								super.updateItem(deckModel, empty)
								tooltip = deckModel?.comment?.value?.let { Tooltip(it) }
							}
						}
					}
				}
			}
			center {
			}
        }
    }
}
