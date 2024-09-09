package at.woolph.caco.view.decks

import at.woolph.caco.datamodel.decks.*
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.time.LocalDate

class DecksView : View() {
    override val root = BorderPane()

    lateinit var tvArchetypes: TableView<DeckTreeModel>
	val archetypes = FXCollections.observableArrayList<DeckTreeModel>()
	val archetypesFiltered = archetypes.filtered { it.filterView() }

	fun DeckTreeModel.filterView(): Boolean = true


	lateinit var tvDecks: TableView<DeckBuildModel>
    val decks = FXCollections.observableArrayList<DeckBuildModel>()
    val decksFiltered = decks.filtered { it.filterView() }

    fun DeckBuildModel.filterView(): Boolean = true

	fun updateArchetypes() {
		archetypes.setAll(transaction {
			DeckArchetype.all().map { DeckArchetypeModel(it) }
		})
	}

    fun updateDecks() {
		decks.setAll(transaction {
			Build.all().map { DeckBuildModel(it) }
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
						column("Archetype", DeckBuildModel::archetypeName) {
							remainingWidth()
						}
						column("Name", DeckBuildModel::name) {
							remainingWidth()
						}
						column("Format", DeckBuildModel::format) {
							contentWidth(35.0, useAsMin = true, useAsMax = true)
						}
						column("Archiv", DeckBuildModel::archived) {
							useCheckbox()
							contentWidth(15.0, useAsMin = true, useAsMax = true)
						}

						setRowFactory {
							object: TableRow<DeckBuildModel>() {
								override fun updateItem(deckModel: DeckBuildModel?, empty: Boolean) {
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
			AddDeckBuildDialog(this@DecksView, initialArchetype?.item).showAndWait().ifPresent { (archetype, subname, comment) ->
				Build.new {
					this.archetype = archetype
					this.version = subname
					this.comment = comment// TODO
					this.dateOfCreation = LocalDate.now()
					this.dateOfLastModification = LocalDate.now()
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

	private fun getContextMenu(deck: DeckBuildModel?) = if(deck != null) ContextMenu().apply {
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
