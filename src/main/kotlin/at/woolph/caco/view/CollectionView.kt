package at.woolph.caco.view

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.Condition
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.Set
import at.woolph.caco.importer.sets.importCardsOfSet
import at.woolph.caco.importer.sets.importPromosOfSet
import at.woolph.caco.importer.sets.importSet
import at.woolph.caco.importer.sets.importTokensOfSet
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.shape.Shape
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import tornadofx.controlsfx.segmentedbutton
import kotlin.math.min

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
        val numberInSet get() = card.name.toProperty()

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

        val cardImage by lazy {
            Image(card.image.toString(), 224.0, 312.0, true, true)
        }

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

    val cardName = SimpleStringProperty("")
    val cardNumberInSet = SimpleStringProperty("")

    val setProperty = SimpleObjectProperty<Set>()
    var set by setProperty

    val filterTextProperty = SimpleStringProperty("")
    val filterNonFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterRarityCommon = SimpleBooleanProperty(true)
    val filterRarityUncommon = SimpleBooleanProperty(true)
    val filterRarityRare = SimpleBooleanProperty(true)
    val filterRarityMythic = SimpleBooleanProperty(true)

    lateinit var imageView: ImageView
    lateinit var imageLoadingProgressIndicatorBackground: Shape
    lateinit var imageLoadingProgressIndicator: ProgressIndicator
    lateinit var tvCards: TableView<CardInfo>

    val sets = FXCollections.observableArrayList<Set>()

    val cards = FXCollections.observableArrayList<CardInfo>()
    val cardsFiltered = cards.filtered { cardInfo -> cardInfo.card.filterView() }

    abstract val cardPossesionTargtNonPremium: Int
    abstract val cardPossesionTargtPremium: Int

    abstract fun Card.getPossesionsNonPremium(): Int
    abstract fun Card.getPossesionsPremium(): Int

    abstract fun Card.filterView(): Boolean

    abstract fun getRelevantSets(): List<Set>
    abstract fun ToolBar.addFeatureButtons()

    fun updateSets() {
        sets.setAll(getRelevantSets())
    }

    fun updateCards() {
        cards.setAll(transaction {
            set.cards.toList().map { CardInfo(it) }
        })
    }

    fun addSet(setCode: String): Set { // TODO Progress dialog
        val importedSet = importSet(setCode).reimportSet()

        updateSets()

        return importedSet
    }

    fun Set.reimportSet(): Set = apply {
        importCardsOfSet()
        importTokensOfSet()
        importPromosOfSet()

        updateCards()
    }

    fun setFilter(text: String, nonFoilComplete: Boolean, foilComplete: Boolean, filterRarityCommon: Boolean, filterRarityUncommon: Boolean, filterRarityRare: Boolean, filterRarityMythic: Boolean) {
        cardsFiltered.setPredicate { cardInfo -> cardInfo.card.filterView()
                    && (if(!text.isNullOrBlank()) cardInfo.name.get().contains(text, ignoreCase = true) else true)
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

        setProperty.addListener { observable, oldSet, newSet ->
            updateCards()
        }
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

        set = sets.first()

        with(root) {
            top {
                toolbar {
                    combobox(setProperty, sets) {
                        cellFormat {
                            graphic = item.iconImage?.let {
                                ImageView(it).apply {
                                    fitHeight = 24.0
                                    fitWidth = 24.0
                                }
                            }
                            text = "${item.shortName.toUpperCase()} - ${item.name}"
                        }
                    }
                    button("\u21BB") {
                        action {
                            transaction {
                                set.reimportSet()
                            }
                        }
                    }
                    button("+") {
                        action {
                            transaction {
                                DialogEnterSetCode(this@CollectionView).showAndWait().ifPresent {
                                    set = addSet(it)
                                }
                            }
                        }
                    }
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
            left {
                form {
                    fieldset("Card Info") {
                        field("Set Number") {
                            textfield(cardNumberInSet) {
                                isEditable = true
                            }
                        }
                        field("Name") {
                            textfield(cardName) {
                                isEditable = false
                            }
                        }
                        field("Image") {
                            stackpane {
                                imageView = imageview {
                                    fitHeight = 312.0
                                    fitWidth = 224.0
                                }
                                imageLoadingProgressIndicatorBackground = rectangle {
                                    fill = javafx.scene.paint.Color.rgb(1, 1, 1, 0.3)
                                    isVisible = false
                                    height = imageView.fitHeight
                                    width = imageView.fitWidth
                                }
                                imageLoadingProgressIndicator = progressindicator {
                                    isVisible = false
                                }
                            }
                        }
                    }
                    /*
                    buttonbar {
                        button("<") {
                            action {
                                tvCards.selectionModel.selectPrevious()
                            }
                        }
                        button(">") {
                            action {
                                tvCards.selectionModel.selectNext()
                            }
                        }
                    }
                    */
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

                    column("Set Number", CardInfo::numberInSet) {
                        contentWidth(5.0, useAsMin = true, useAsMax = true)
                    }
                    column("Rarity", CardInfo::rarity) {
                        contentWidth(5.0, useAsMin = true, useAsMax = true)
                    }

                    column("Name", CardInfo::name).remainingWidth()

                    column("Possessions", CardInfo::possessionTotalProperty) {
                        contentWidth(5.0, useAsMin = true, useAsMax = true)
                    }
                    column("Collection Completion", CardInfo::collectionCompletionProperty) {
                        contentWidth(15.0, useAsMin = true, useAsMax = true)
                    }

                    selectionModel.selectionMode = SelectionMode.SINGLE
                    selectionModel.selectedItemProperty().addListener { _, _, newCard ->
                        newCard?.let {
                            cardNumberInSet.set(it.numberInSet.get())
                            cardName.set(it.name.get())

                            runAsync {
                                imageLoadingProgressIndicatorBackground.isVisible = true
                                imageLoadingProgressIndicator.isVisible = true
                                newCard.cardImage
                            } ui {
                                imageView.image = it
                                imageLoadingProgressIndicator.isVisible = false
                                imageLoadingProgressIndicatorBackground.isVisible = false
                            }

                            runAsync {
                                val loadingIndizes = listOf(
                                        selectionModel.selectedIndex + 1,
                                        selectionModel.selectedIndex - 1,
                                        selectionModel.selectedIndex + 2,
                                        selectionModel.selectedIndex - 2,
                                        selectionModel.selectedIndex + 3)

                                loadingIndizes.forEach {
                                    if (0 <= it && it < items.size) {
                                        items[it].cardImage
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class DialogEnterSetCode(val owner: View): Dialog<String?>() {
    val messages
        get() = owner.messages

    val setCodeProperty = SimpleStringProperty("")
    val setCode by setCodeProperty

    init {
        initOwner(owner.primaryStage)

        isResizable = true
        title = "Add a Set"
        headerText = "Add a Set"

        dialogPane.apply {
            content = BorderPane().apply {
                center {
                    form {
                        fieldset {
                            field("Set Code") {
                                textfield(setCodeProperty) {
                                    hgrow = Priority.ALWAYS
                                    runLater { requestFocus() }
                                }
                            }
                        }
                    }
                }
            }

            buttonTypes.setAll(ButtonType.APPLY, ButtonType.CANCEL)
        }

        setResultConverter { button ->
            // TODO data validation check
            when (button) {
                ButtonType.APPLY -> setCode.toLowerCase()
                else -> null
            }
        }
    }
}
