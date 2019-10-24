package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.*
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.security.KeyStore

class DecksView : View() {
    override val root = BorderPane()

    lateinit var tvArchetypes: TableView<DeckTreeModel>
	val archetypes = FXCollections.observableArrayList<DeckTreeModel>()
	val archetypesFiltered = archetypes.filtered { it.filterView() }

	fun DeckTreeModel.filterView(): Boolean = true


	lateinit var tvDecks: TableView<DeckVariantModel>
    val decks = FXCollections.observableArrayList<DeckVariantModel>()
    val decksFiltered = decks.filtered { it.filterView() }

    fun DeckVariantModel.filterView(): Boolean = true


	fun updateArchetypes() {
		archetypes.setAll(transaction {
			DeckArchetype.all().map { DeckArchetypeModel(it) }
		})
	}

    fun updateDecks() {
		decks.setAll(transaction {
			DeckVariant.all().map { DeckVariantModel(it) }
        })
    }

    init {
        title = "CaCo"

		updateArchetypes()
		updateDecks()

        with(root) {
			top {
				toolbar {
					button("+") {
						action {
							addNewArchetype()
						}
					}
					button("+") {
						action {
							addNewDeck()
						}
					}
					region {
						prefWidth = 40.0

						hboxConstraints {
							hGrow = Priority.ALWAYS
						}
					}
				}
			}
			left {
				splitpane(orientation = Orientation.VERTICAL) {
					tvArchetypes = tableview(archetypesFiltered) {
						hboxConstraints {
							hGrow = Priority.ALWAYS
						}
						vboxConstraints {
							vGrow = Priority.ALWAYS
						}
						column("Format", DeckTreeModel::format) {
							contentWidth(35.0, useAsMin = true, useAsMax = true)
						}
						column("Name", DeckTreeModel::name) {
							remainingWidth()
						}
						column("Archiv", DeckTreeModel::archived) {
							useCheckbox()
							contentWidth(15.0, useAsMin = true, useAsMax = true)
						}

						setRowFactory {
							object: TableRow<DeckTreeModel>() {
								override fun updateItem(deckModel: DeckTreeModel?, empty: Boolean) {
									super.updateItem(deckModel, empty)
									tooltip = deckModel?.comment?.value?.let { if(it.isNotBlank()) Tooltip(it) else null }

									setOnContextMenuRequested { event ->
										getContextMenu(item).show(this, event.screenX, event.screenY)
									}
								}
							}
						}
					}

					tvDecks = tableview(decksFiltered) {
						hboxConstraints {
							hGrow = Priority.ALWAYS
						}
						vboxConstraints {
							vGrow = Priority.ALWAYS
						}
						column("Archetype", DeckVariantModel::archetypeName) {
							remainingWidth()
						}
						column("Name", DeckVariantModel::name) {
							remainingWidth()
						}
						column("Format", DeckVariantModel::format) {
							contentWidth(35.0, useAsMin = true, useAsMax = true)
						}
						column("Archiv", DeckVariantModel::archived) {
							useCheckbox()
							contentWidth(15.0, useAsMin = true, useAsMax = true)
						}

						setRowFactory {
							object: TableRow<DeckVariantModel>() {
								override fun updateItem(deckModel: DeckVariantModel?, empty: Boolean) {
									super.updateItem(deckModel, empty)
									tooltip = deckModel?.comment?.value?.let { if(it.isNotBlank()) Tooltip(it) else null }

									setOnContextMenuRequested { event ->
										getContextMenu(item).show(this, event.screenX, event.screenY)
									}
								}
							}
						}
					}
				}
			}
			center {
			}
        }
    }

	fun addNewArchetype() {
		transaction {
			AddDeckArchetypeDialog(this@DecksView).showAndWait().ifPresent { (name, format, comment) ->
				transaction {
					DeckArchetype.new {
						this.name = name
						this.format = format
						this.comment = comment
					}
					updateArchetypes()
				}
			}
		}
	}

	fun addNewDeck(initialArchetype: DeckArchetypeModel? = null) {
		transaction {
			AddDeckVariantDialog(this@DecksView, initialArchetype?.item).showAndWait().ifPresent { (archetype, name, comment) ->
				DeckVariant.new {
					this.archetype = archetype
					this.name = name
					this.comment = comment// TODO
				}
				updateDecks()
			}
		}
	}

	private fun getContextMenu(deck: DeckTreeModel?) = if(deck != null) ContextMenu().apply {
		if(deck is DeckArchetypeModel) {
			item("Add Deck") {
				action {
					println(deck.item)
					addNewDeck(deck)
				}
			}
		}
		checkmenuitem("Archive") {
			selectedProperty().bindBidirectional(deck.archived)
		}
	} else ContextMenu().apply {
		item("Add new archetype") {
			action {
				addNewArchetype()
			}
		}
	}

	private fun getContextMenu(deck: DeckArchetypeModel?) = if(deck != null) ContextMenu().apply {
		item("Delete") {
			action {
				confirmation("Are you sure you want to delete archetype $deck?", null, ButtonType.YES, ButtonType.NO) {
					if(it == ButtonType.YES) {
						transaction {
							deck.item.delete()
							updateArchetypes()
						}
					}
				}
			}
		}
		checkmenuitem("Archive") {
			selectedProperty().bindBidirectional(deck.archived)
		}
	} else ContextMenu().apply {
		item("Add new archetype") {
			action {
				addNewArchetype()
			}
		}
	}

	private fun getContextMenu(deck: DeckVariantModel?) = if(deck != null) ContextMenu().apply {
			item("Delete") {
				action {
					confirmation("Are you sure you want to delete $deck?", null, ButtonType.YES, ButtonType.NO) {
						if(it == ButtonType.YES) {
							transaction {
								deck.item.delete()
								updateDecks()
							}
						}
					}
				}
			}
			checkmenuitem("Archive") {
				selectedProperty().bindBidirectional(deck.archived)
			}
		} else ContextMenu().apply {
			item("Add new archetype") {
				action {
					addNewDeck()
				}
			}
		}
}
