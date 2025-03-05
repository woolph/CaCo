package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Rarity
import at.woolph.caco.datamodel.sets.loadSetLogo
import at.woolph.caco.masterdata.import.importCardsOfSet
import at.woolph.caco.masterdata.import.importSet
import at.woolph.caco.masterdata.import.importSets
import at.woolph.caco.masterdata.import.update
import at.woolph.caco.gui.view.CardDetailsView
import at.woolph.caco.gui.view.CardImageTooltip
import at.woolph.caco.gui.view.filteredBy
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ButtonBase
import javafx.scene.control.Dialog
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToolBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.javafx.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import tornadofx.View
import tornadofx.button
import tornadofx.center
import tornadofx.column
import tornadofx.combobox
import tornadofx.contentWidth
import tornadofx.getValue
import tornadofx.hboxConstraints
import tornadofx.label
import tornadofx.plusAssign
import tornadofx.region
import tornadofx.remainingWidth
import tornadofx.runLater
import tornadofx.setValue
import tornadofx.splitpane
import tornadofx.tableview
import tornadofx.textfield
import tornadofx.togglebutton
import tornadofx.toolbar
import tornadofx.top
import tornadofx.vbox
import tornadofx.vboxConstraints
import tornadofx.whenDocked
import tornadofx.whenUndocked
import java.util.*
import java.util.concurrent.CancellationException

suspend fun <T> Dialog<T>.showAndAwait(): T? = withContext(Dispatchers.Main.immediate) {
    this@showAndAwait.showAndWait().orElse(null)
}

fun ButtonBase.action(coroutineScope: CoroutineScope, op: suspend () -> Unit) = setOnAction {
    coroutineScope.launch(Dispatchers.Main.immediate) {
        op()
    }
}
val emptyImage = Image(CollectionView::class.java.getResourceAsStream("/empty.png"))
fun imageViewDelayed(coroutineScope: CoroutineScope, width: Int, height: Int, block: suspend () -> Image?) =
    ImageView(emptyImage).apply {
        fitWidth = width.toDouble()
        fitHeight = height.toDouble()
        coroutineScope.launch(Dispatchers.IO) {
            block()?.let {
                withContext(Dispatchers.Main) {
                    this@apply.image = it
                }
            }
        }
    }

abstract class CoroutineScopedView @JvmOverloads constructor(title: String? = null, icon: Node? = null): View(title, icon) {
    val coroutineScopedViewName = "CoroutineScopedView(\"$title\")"
    val coroutineScope = CoroutineScope(SupervisorJob() + CoroutineName(coroutineScopedViewName))
    abstract val jobs: List<suspend (CoroutineScope) -> Unit>

    init {
        whenUndocked {
            coroutineScope.coroutineContext.cancelChildren(CancellationException("undocking $coroutineScopedViewName"))
        }
        whenDocked {
            jobs.forEach {
                coroutineScope.launch(Dispatchers.Main.immediate) {
                    it(this)
                }
            }
        }
    }
}



abstract class CollectionView(val collectionSettings: CollectionSettings) : CoroutineScopedView() {
    companion object {
        const val FOIL_NOT_IN_POSSESION = "\u2606"
        const val FOIL_IN_POSSESION = "\u2605"
        const val NONFOIL_NOT_IN_POSSESION = "\u2B1C"
        const val NONFOIL_IN_POSSESION = "\u2B1B"
        const val CARD_IN_POSSESION = "\u2B24"
        const val ICON_REDUNDANT_OWNED_CARD = "+"

        val LOG = LoggerFactory.getLogger(CollectionView::class.java)
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
    val setsSorted = sets.sorted { t1: CardSet, t2: CardSet ->
        -t1.dateOfRelease.compareTo(t2.dateOfRelease)
    }

    val cards = FXCollections.observableArrayList<CardPossessionModel>()
    val cardsSorted = cards.sorted()
    val cardsFiltered = cardsSorted.filteredBy(listOf(
        filterTextProperty,
        filterCompleteProperty,
        filterNonFoilCompleteProperty,
        filterFoilCompleteProperty,
        filterRarityCommon,
        filterRarityUncommon,
        filterRarityRare,
        filterRarityMythic,
    )) { cardInfo -> cardInfo.filterView()
        && (filterTextProperty.get().isBlank() || cardInfo.names.any { it.contains(filterTextProperty.get(), ignoreCase = true) })
        && (filterCompleteProperty.get() || !cardInfo.completed.get())
        && (filterNonFoilCompleteProperty.get() || !cardInfo.completedNonPremium.get())
        && (filterFoilCompleteProperty.get() || !cardInfo.completedPremium.get())
        && (filterRarityCommon.get() || cardInfo.rarity.value != Rarity.COMMON)
        && (filterRarityUncommon.get() || cardInfo.rarity.value != Rarity.UNCOMMON)
        && (filterRarityRare.get() || cardInfo.rarity.value != Rarity.RARE)
        && (filterRarityMythic.get() || cardInfo.rarity.value != Rarity.MYTHIC)
    }

    abstract fun CardPossessionModel.filterView(): Boolean

    abstract fun ToolBar.addFeatureButtons()

    suspend fun updateSets() {
        val updatedSets = newSuspendedTransaction(Dispatchers.IO) {
            LOG.trace("updateSets loading from DB")
            CardSet.all().toList()
        }.filter { collectionSettings.cardSetFilter(it) }
        LOG.trace("updateSets loaded from DB")

        updateSetsView(updatedSets)
    }

    suspend fun updateSetsView(updatedSets: List<CardSet>) = withContext(Dispatchers.Main.immediate) {
        LOG.trace("updateSets updating view")
        val oldSetSelected = set?.name
        sets.setAll(updatedSets)
        set = sets.find { it.name == oldSetSelected }
        LOG.trace("updateSets view updated")
    }

    suspend fun updateCards() {
        val updatedCards = newSuspendedTransaction(Dispatchers.IO) {
            LOG.trace("updateCards loading from DB")
            set?.cards?.toList()?.map { CardPossessionModel(it, collectionSettings) } ?: emptyList()
        }
        LOG.trace("updateCards loaded from DB")
        updateCardsView(updatedCards)
    }

    suspend fun updateCardsView(updatedCards: List<CardPossessionModel>) = withContext(Dispatchers.Main.immediate) {
        LOG.trace("updateCards view updating")
        cards.setAll(updatedCards)
        LOG.trace("updateCards view updated")
    }

    suspend fun addSet(setCode: String): CardSet { // TODO Progress dialog
        val importedSet = importSet(setCode).reimportSet()

        updateSets()

        return importedSet
    }

    suspend fun importAllSets() {
        importSets()
        updateSets()
    }

    suspend fun CardSet.reimportSet(): CardSet = apply {
        update()
//        LOG.info("reimport current set $this")
        importCardsOfSet(listOf("german"))

        updateSets()
        updateCards()
    }

    suspend fun initSets() {
        updateSets()
        LOG.trace("setting the initial value")
        withContext(Dispatchers.Main.immediate) {
            set = setsSorted.firstOrNull()
        }
        LOG.trace("setting intial value is done")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun updateCardsOnSetChange() {
        setProperty.asFlow().collectLatest { updateCards() }
    }

    override val jobs: List<suspend (CoroutineScope) -> Unit> = listOf(
        { initSets() },
        { updateCardsOnSetChange() },
    )

    init {
        title = "CaCo"

        with(root) {
            top {
                toolbar {
					toggleButtonImageLoading = togglebutton("\uD83D\uDDBC")

                    combobox(setProperty, setsSorted) {
                        cellFormat {
                            graphic = imageViewDelayed(coroutineScope, 24, 24) {
                                item?.icon?.loadSetLogo(48f)
                            }
                            text = item?.let { "${it.id.value.uppercase(Locale.getDefault())} - ${it.name}" }
                        }
                    }
                    button("\u21BB All") {
                        action(coroutineScope) {
                            newSuspendedTransaction {
                                importAllSets()
                            }
                        }
                    }
                    button("\u21BB") {
                        action(coroutineScope) {
                            newSuspendedTransaction {
                                set?.reimportSet()
                            }
                        }
                    }
                    button("+") {
                        action(coroutineScope) {
                            AddSetsDialog(this@CollectionView).showAndAwait()?.let { setCodes ->
                                newSuspendedTransaction {
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

						column<CardPossessionModel, String>("Set", { it.value.set.map { it.setCode } }).apply {
							contentWidth(5.0, useAsMin = true, useAsMax = true)
						}
						column("Number", CardPossessionModel::collectorNumber) {
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
						column("Price", CardPossessionModel::priceString) {
							contentWidth(5.0, useAsMin = true, useAsMax = true)
						}

						setRowFactory {
							object : TableRow<CardPossessionModel>() {
								override fun updateItem(cardInfo: CardPossessionModel?, empty: Boolean) {
									super.updateItem(cardInfo, empty)
									tooltip = cardInfo?.let { CardImageTooltip(it, toggleButtonImageLoading.selectedProperty()) }
								}
							}
						}

						selectionModel.selectionMode = SelectionMode.SINGLE
						selectionModel.selectedItemProperty().addListener { _, _, _ ->
							if (toggleButtonImageLoading.isSelected) {
                                coroutineScope.launch(Dispatchers.Default) {
                                    flowOf(+1, -1, +2, +3)
                                        .map { tvCards.selectionModel.selectedIndex + it }
                                        .filter { it >= 0 && it < tvCards.items.size }
                                        .map { tvCards.items[it] }
                                        .collect {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                it.cacheImage()
                                            }
                                        }
                                }
							}
						}

//                        coroutineScope.launch(Dispatchers.Default) {
//                            selectionModel.selectedItemProperty().asFlow()
//                                .collectLatest {
//                                    flowOf(+1, -1, +2, +3)
//                                        .map { tvCards.selectionModel.selectedIndex + it }
//                                        .filter { it >= 0 && it < tvCards.items.size }
//                                        .map { tvCards.items[it] }
//                                        .collect {
//                                            coroutineScope.launch(Dispatchers.IO) {
//                                                it.cacheImage()
//                                            }
//                                        }
//                                }
//                        }
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

