package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Cards.nameDE
import at.woolph.caco.importer.sets.*
import at.woolph.caco.view.*
import at.woolph.libs.ktfx.mapBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import kotlin.math.min
import javafx.scene.control.Tooltip

abstract class CollectionView : View() {
    companion object {
        val FOIL_NOT_IN_POSSESION = "\u2606"
        val FOIL_IN_POSSESION = "\u2605"
        val CARD_NOT_IN_POSSESION = "\u2B1C"
        val CARD_IN_POSSESION = "\u2B1B"
        val ICON_REDUNDANT_OWNED_CARD = "+"
    }

    inner class CardInfo(val card: Card) {
        val rarity get() = card.rarity.toProperty()
        val name get() = card.name.toProperty()
        val nameDE get() = card.nameDE.toProperty()
        val numberInSet get() = card.numberInSet.toProperty()

        val possessionNonPremiumProperty = SimpleIntegerProperty(0)
        var possessionNonPremium by possessionNonPremiumProperty

        val possessionPremiumProperty = SimpleIntegerProperty(0)
        var possessionPremium by possessionPremiumProperty

        val possessionTotalProperty = SimpleIntegerProperty(0)
        var possessionTotal by possessionTotalProperty

        val collectionCompletionProperty = SimpleStringProperty("")
        var collectionCompletion by collectionCompletionProperty

        val completedNonPremiumProperty = SimpleBooleanProperty(false)
        var completedNonPremium by completedNonPremiumProperty

        val completedPremiumProperty = SimpleBooleanProperty(false)
        var completedPremium by completedPremiumProperty

        init {
            transaction {
                possessionNonPremium = card.getPossesionsNonPremium()
                possessionPremium = card.getPossesionsPremium()

                possessionTotal = possessionNonPremium + possessionPremium

                val ownedCount = min(cardPossesionTargtNonPremium, possessionNonPremium)
                val ownedCountFoil = min(cardPossesionTargtPremium, possessionPremium)

                val plus = if(possessionNonPremium>cardPossesionTargtNonPremium) ICON_REDUNDANT_OWNED_CARD else " "
                val plusFoil = if(possessionPremium>cardPossesionTargtPremium) ICON_REDUNDANT_OWNED_CARD else " "

                collectionCompletion = "${CARD_IN_POSSESION.repeat(ownedCount)}${CARD_NOT_IN_POSSESION.repeat(cardPossesionTargtNonPremium-ownedCount)}$plus\t${FOIL_IN_POSSESION.repeat(ownedCountFoil)}${FOIL_NOT_IN_POSSESION.repeat(cardPossesionTargtPremium-ownedCountFoil)}$plusFoil"

                completedNonPremium = possessionNonPremium>=cardPossesionTargtNonPremium
                completedPremium = possessionPremium>=cardPossesionTargtPremium
                //collectionCompletion = "${CARD_IN_POSSESION.repeat(ownedCount)}${CARD_NOT_IN_POSSESION.repeat(CARD_POSSESION_TARGET-ownedCount)} ${FOIL_IN_POSSESION.repeat(ownedCountFoil)}${FOIL_NOT_IN_POSSESION.repeat(FOIL_POSSESION_TARGET-ownedCountFoil)}"
            }
        }
    }

    override val root = BorderPane()

    val setProperty = SimpleObjectProperty<CardSet?>()
    var set by setProperty

    val filterTextProperty = SimpleStringProperty("")
    val filterNonFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterRarityCommon = SimpleBooleanProperty(true)
    val filterRarityUncommon = SimpleBooleanProperty(true)
    val filterRarityRare = SimpleBooleanProperty(true)
    val filterRarityMythic = SimpleBooleanProperty(true)

    lateinit var tvCards: TableView<CardInfo>
	lateinit var toggleButtonImageLoading: ToggleButton

    val sets = FXCollections.observableArrayList<CardSet>()

    val cards = FXCollections.observableArrayList<CardInfo>()
    val cardsFiltered = cards.filtered { cardInfo -> cardInfo.card.filterView() }

    abstract val cardPossesionTargtNonPremium: Int
    abstract val cardPossesionTargtPremium: Int

    abstract fun Card.getPossesionsNonPremium(): Int
    abstract fun Card.getPossesionsPremium(): Int

    abstract fun Card.filterView(): Boolean

    abstract fun getRelevantSets(): List<CardSet>
    abstract fun ToolBar.addFeatureButtons()

    fun updateSets() {
        sets.setAll(getRelevantSets())
    }

    fun updateCards() {
        cards.setAll(transaction {
            set?.cards?.toList()?.map { CardInfo(it) }
        } ?: emptyList())
    }

    fun addSet(setCode: String): CardSet { // TODO Progress dialog
        val importedSet = importSet(setCode).reimportSet()

        updateSets()

        return importedSet
    }

    fun CardSet.reimportSet(): CardSet = apply {
        importCardsOfSet()
        importCardsOfSetAdditionalLanguage("german")
        importTokensOfSet()
        importPromosOfSet()

        updateCards()
    }

    fun setFilter(text: String, nonFoilComplete: Boolean, foilComplete: Boolean, filterRarityCommon: Boolean, filterRarityUncommon: Boolean, filterRarityRare: Boolean, filterRarityMythic: Boolean) {
        cardsFiltered.setPredicate { cardInfo -> cardInfo.card.filterView()
                    && (if(!text.isNullOrBlank()) cardInfo.name.get().contains(text, ignoreCase = true) || cardInfo.nameDE.get().contains(text, ignoreCase = true) else true)
                    && (nonFoilComplete || !cardInfo.completedNonPremium)
                    && (foilComplete || !cardInfo.completedPremium)
                    && (filterRarityCommon || cardInfo.card.rarity != Rarity.COMMON)
                    && (filterRarityUncommon || cardInfo.card.rarity != Rarity.UNCOMMON)
                    && (filterRarityRare || cardInfo.card.rarity != Rarity.RARE)
                    && (filterRarityMythic || cardInfo.card.rarity != Rarity.MYTHIC)
        }
    }

    init {
        title = "CaCo"

        updateSets()

        setProperty.addListener { _, _, _ -> updateCards() }

        val filterChangeListener = ChangeListener<Any> { _, _, _ ->
            setFilter(filterTextProperty.get(), filterNonFoilCompleteProperty.get(), filterFoilCompleteProperty.get(),
                    filterRarityCommon.get(), filterRarityUncommon.get(), filterRarityRare.get(), filterRarityMythic.get())
        }
        filterTextProperty.addListener(filterChangeListener)
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
                            text = item?.let { "${it.shortName.toUpperCase()} - ${it.name}" }
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
										addSet(it.trim().toLowerCase())
									}.last()
                                }
                            }
                        }
                    }
					// TODO filter toolbar into fragment
                    label("Filter: ")
                    textfield(filterTextProperty) {

                    }
                    togglebutton(CARD_IN_POSSESION.repeat(cardPossesionTargtNonPremium)) {
                        filterNonFoilCompleteProperty.bind(selectedProperty())
                    }
                    if (cardPossesionTargtPremium > 0) {
                        togglebutton(FOIL_IN_POSSESION.repeat(cardPossesionTargtPremium)) {
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
                tvCards = tableview(cardsFiltered) {
                    hboxConstraints {
                        hGrow = Priority.ALWAYS
                    }
                    vboxConstraints {
                        vGrow = Priority.ALWAYS
                    }

                    column("Number", CardInfo::numberInSet) {
                        contentWidth(5.0, useAsMin = true, useAsMax = true)
                    }
                    column("Rarity", CardInfo::rarity) {
                        contentWidth(5.0, useAsMin = true, useAsMax = true)
                    }

                    column("Name EN", CardInfo::name).remainingWidth()

                    column("Name DE", CardInfo::nameDE).remainingWidth()

                    column("Possessions", CardInfo::possessionTotalProperty) {
                        contentWidth(5.0, useAsMin = true, useAsMax = true)
                    }
                    column("Collection Completion", CardInfo::collectionCompletionProperty) {
                        contentWidth(15.0, useAsMin = true, useAsMax = true)
                    }

					setRowFactory {
						object: TableRow<CardInfo>() {
							override fun updateItem(cardInfo: CardInfo?, empty: Boolean) {
								super.updateItem(cardInfo, empty)
								tooltip = cardInfo?.card?.let { CardImageTooltip(it, toggleButtonImageLoading.selectedProperty()) }
							}
						}
					}

                    selectionModel.selectionMode = SelectionMode.SINGLE
					selectionModel.selectedItemProperty().addListener { _, _, _ ->
						if(toggleButtonImageLoading.isSelected) {
							tornadofx.runAsync {
								// precache the next images
								listOf(tvCards.selectionModel.selectedIndex + 1,
										tvCards.selectionModel.selectedIndex - 1,
										tvCards.selectionModel.selectedIndex + 2,
										tvCards.selectionModel.selectedIndex + 3).forEach {
									if (0 <= it && it < tvCards.items.size) {
										tvCards.items[it].card.getCachedImage()
									}
								}
							}
						}
					}
                }
            }
			left {
				vbox {
					this += find<CardDetailsView>().apply {
						this.cardProperty.bind(tvCards.selectionModel.selectedItemProperty().mapBinding { it?.card })
						this.imageLoadingProperty.bind(toggleButtonImageLoading.selectedProperty())
					}
					this += find<CardPossessionView>().apply {
						this.cardProperty.bind(tvCards.selectionModel.selectedItemProperty().mapBinding { it?.card })
					}
				}
			}
        }
    }
}

