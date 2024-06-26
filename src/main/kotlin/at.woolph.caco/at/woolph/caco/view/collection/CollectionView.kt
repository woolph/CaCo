package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.importer.sets.*
import at.woolph.caco.view.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.util.*

abstract class CollectionView(val collectionSettings: CollectionSettings) : View() {
    companion object {
        val FOIL_NOT_IN_POSSESION = "\u2606"
        val FOIL_IN_POSSESION = "\u2605"
        val NONFOIL_NOT_IN_POSSESION = "\u2B1C"
        val NONFOIL_IN_POSSESION = "\u2B1B"
        val CARD_IN_POSSESION = "\u2B24"
        val ICON_REDUNDANT_OWNED_CARD = "+"
    }

    override val root = BorderPane()

    val setProperty = SimpleObjectProperty<CardSet?>()
    var set by setProperty

    val filterTextProperty = SimpleStringProperty("")
    val filterCompleteProperty = SimpleBooleanProperty(true)
    val filterNonFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterRarityCommon = SimpleBooleanProperty(true)
    val filterRarityUncommon = SimpleBooleanProperty(true)
    val filterRarityRare = SimpleBooleanProperty(true)
    val filterRarityMythic = SimpleBooleanProperty(true)

    lateinit var tvCards: TableView<CardPossessionModel>
	lateinit var toggleButtonImageLoading: ToggleButton

    val sets = FXCollections.observableArrayList<CardSet>()

    val cards = FXCollections.observableArrayList<CardPossessionModel>()
    val cardsSorted = cards.sorted()
    val cardsFiltered = cardsSorted.filtered { cardInfo -> cardInfo.filterView() }

    abstract fun CardPossessionModel.filterView(): Boolean

    abstract fun ToolBar.addFeatureButtons()

    fun updateSets() {
        sets.setAll(transaction { CardSet.all().toList().filter { collectionSettings.cardSetFilter(it) }.asObservable()
            .sorted { t1: CardSet, t2: CardSet ->
			-t1.dateOfRelease.compareTo(t2.dateOfRelease)
		}})
    }

    fun updateCards() {
        cards.setAll(transaction {
            set?.cards?.toList()?.map { CardPossessionModel(it, collectionSettings) }
        } ?: emptyList())
    }

    fun addSet(setCode: String): CardSet { // TODO Progress dialog
        val importedSet = importSet(setCode).reimportSet()

        updateSets()

        return importedSet
    }

    fun CardSet.reimportSet(): CardSet = apply {
        update()
//        LOG.info("reimport current set $this")
        importCardsOfSet(listOf("german"))

        updateSets()
        updateCards()
    }

    fun setFilter(text: String, complete: Boolean, nonFoilComplete: Boolean, foilComplete: Boolean, filterRarityCommon: Boolean, filterRarityUncommon: Boolean, filterRarityRare: Boolean, filterRarityMythic: Boolean) {
        cardsFiltered.setPredicate { cardInfo -> cardInfo.filterView()
                    && (if(!text.isBlank()) cardInfo.name.value.contains(text, ignoreCase = true) || cardInfo.nameDE.value?.contains(text, ignoreCase = true) ?: false else true)
                    && (complete || !cardInfo.completed.get())
                    && (nonFoilComplete || !cardInfo.completedNonPremium.get())
                    && (foilComplete || !cardInfo.completedPremium.get())
                    && (filterRarityCommon || cardInfo.rarity.value != Rarity.COMMON)
                    && (filterRarityUncommon || cardInfo.rarity.value != Rarity.UNCOMMON)
                    && (filterRarityRare || cardInfo.rarity.value != Rarity.RARE)
                    && (filterRarityMythic || cardInfo.rarity.value != Rarity.MYTHIC)
        }
    }

    init {
        title = "CaCo"

        updateSets()

        setProperty.addListener { _, _, _ -> updateCards() }

        val filterChangeListener = ChangeListener<Any> { _, _, _ ->
            setFilter(filterTextProperty.get(), filterCompleteProperty.get(), filterNonFoilCompleteProperty.get(), filterFoilCompleteProperty.get(),
                    filterRarityCommon.get(), filterRarityUncommon.get(), filterRarityRare.get(), filterRarityMythic.get())
        }
        filterTextProperty.addListener(filterChangeListener)
        filterCompleteProperty.addListener(filterChangeListener)
        filterNonFoilCompleteProperty.addListener(filterChangeListener)
        filterFoilCompleteProperty.addListener(filterChangeListener)
        filterRarityCommon.addListener(filterChangeListener)
        filterRarityUncommon.addListener(filterChangeListener)
        filterRarityRare.addListener(filterChangeListener)
        filterRarityMythic.addListener(filterChangeListener)

        set = sets.firstOrNull()

        with(root) {
            top {
                toolbar {
					toggleButtonImageLoading = togglebutton("\uD83D\uDDBC")

                    combobox(setProperty, sets) {
                        cellFormat {
                            graphic = item?.iconImage?.let {
                                ImageView(it).apply {
                                    fitHeight = 24.0
                                    fitWidth = 24.0
                                }
                            }
                            text = item?.let { "${it.id.value.uppercase(Locale.getDefault())} - ${it.name}" }
                        }
                    }
                    button("\u21BB") {
                        action {
                            transaction {
                                set?.reimportSet()
                            }
                        }
                    }
                    button("+") {
                        action {
                            transaction {
                                AddSetsDialog(this@CollectionView).showAndWait().ifPresent { setCodes ->
									// TODO progress dialog
									set = setCodes.split(',').map {
										addSet(it.trim().lowercase(Locale.getDefault()))
									}.last()
                                }
                            }
                        }
                    }
					// TODO filter toolbar into fragment
                    label("Filter: ")
                    textfield(filterTextProperty) {

                    }
                    togglebutton(CARD_IN_POSSESION) {
                        filterCompleteProperty.bind(selectedProperty())
                    }
					if (collectionSettings.cardPossesionTargtNonPremium > 0) {
						togglebutton(NONFOIL_IN_POSSESION.repeat(collectionSettings.cardPossesionTargtNonPremium)) {
							filterNonFoilCompleteProperty.bind(selectedProperty())
						}
					}
                    if (collectionSettings.cardPossesionTargtPremium > 0) {
                        togglebutton(FOIL_IN_POSSESION.repeat(collectionSettings.cardPossesionTargtPremium)) {
                            filterFoilCompleteProperty.bind(selectedProperty())
                        }
                    }
                    // TODO segmented button
                    //segmentedbutton {
                        togglebutton("C") {
                            filterRarityCommon.bind(selectedProperty())
                        }
                        togglebutton("U") {
                            filterRarityUncommon.bind(selectedProperty())
                        }
                        togglebutton("R") {
                            filterRarityRare.bind(selectedProperty())
                        }
                        togglebutton("M") {
                            filterRarityMythic.bind(selectedProperty())
                        }
                    //}
                    region {
                        prefWidth = 40.0

                        hboxConstraints {
                            hGrow = Priority.ALWAYS
                        }
                    }
                    addFeatureButtons()
                }
            }
            center {
				splitpane {
//                    vbox {
//                        alignment = Pos.TOP_CENTER
//                        this += find<SetDetailsView> {
//                            runLater {
//                                this.setProperty.bind(this@CollectionView.setProperty.mapBinding { it?.let { CardSetModel(it) } })
//                            }
//                        }
//                    }

					tvCards = tableview(cardsFiltered) {
						hboxConstraints {
							hGrow = Priority.ALWAYS
						}
						vboxConstraints {
							vGrow = Priority.ALWAYS
						}

						column("Number", CardPossessionModel::numberInSet) {
							contentWidth(5.0, useAsMin = true, useAsMax = true)
						}
						column("Rarity", CardPossessionModel::rarity) {
							contentWidth(5.0, useAsMin = true, useAsMax = true)
						}

						column("Name EN", CardPossessionModel::name).remainingWidth()

						column("Name DE", CardPossessionModel::nameDE).remainingWidth()

						column("Possessions", CardPossessionModel::possessionTotal) {
							contentWidth(5.0, useAsMin = true, useAsMax = true)
						}
						column("Collection Completion", CardPossessionModel::collectionCompletion) {
							contentWidth(15.0, useAsMin = true, useAsMax = true)
						}

						setRowFactory {
							object : TableRow<CardPossessionModel>() {
								override fun updateItem(cardInfo: CardPossessionModel?, empty: Boolean) {
									super.updateItem(cardInfo, empty)
//									tooltip = cardInfo?.let { CardImageTooltip(it, toggleButtonImageLoading.selectedProperty()) }
								}
							}
						}

						selectionModel.selectionMode = SelectionMode.SINGLE
//						selectionModel.selectedItemProperty().addListener { _, _, _ ->
//							if (toggleButtonImageLoading.isSelected) {
//								tornadofx.runAsync {
//									// precache the next images
//									listOf(tvCards.selectionModel.selectedIndex + 1,
//											tvCards.selectionModel.selectedIndex - 1,
//											tvCards.selectionModel.selectedIndex + 2,
//											tvCards.selectionModel.selectedIndex + 3).forEach {
//										if (0 <= it && it < tvCards.items.size) {
//											tvCards.items[it].getCachedImage()
//										}
//									}
//								}
//							}
//						}
					}

                    vbox {
                        alignment = Pos.TOP_CENTER
                        this += find<CardDetailsView> {
                            runLater {
                                this.cardProperty.bind(tvCards.selectionModel.selectedItemProperty())
                                this.imageLoadingProperty.bind(toggleButtonImageLoading.selectedProperty())
                            }
                        }
                        this += find<CardPossessionView> {
                            runLater {
                                this.cardProperty.bind(tvCards.selectionModel.selectedItemProperty())
                            }
                        }
                    }
				}
			}
        }
    }
}

