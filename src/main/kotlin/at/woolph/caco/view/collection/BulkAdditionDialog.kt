package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.Condition
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.importer.sets.importCardsOfSet
import at.woolph.caco.importer.sets.importPromosOfSet
import at.woolph.caco.importer.sets.importTokensOfSet
import at.woolph.caco.view.CardDetailsView
import at.woolph.caco.view.getCachedImage
import at.woolph.libs.ktfx.commitValue
import at.woolph.libs.ktfx.mapBinding
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
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.nio.file.Paths

class BulkAdditionDialog(val set: CardSet, val owner: View, imageLoading: Boolean): Dialog<Boolean>() {

    inner class CardInfo(val card: Card) {
        val rarity get() = card.rarity.toProperty()
        val name get() = card.name.toProperty()
        val numberInSet get() = card.numberInSet.toProperty()
        val bulkAdditionNonPremium = SimpleIntegerProperty(0)
        val bulkAdditionPremium = SimpleIntegerProperty(0)
    }

    val languageProperty = SimpleStringProperty("en")
    val conditionProperty = SimpleObjectProperty(Condition.NEAR_MINT)
    val foilProperty = SimpleObjectProperty(Foil.NONFOIL)

    val bulkAddNumberProperty = SimpleIntegerProperty(0)
    val bulkAddNumber by bulkAddNumberProperty

    val bulkAddNumberFoilProperty = SimpleIntegerProperty(0)
    val bulkAddNumberFoil by bulkAddNumberFoilProperty

    val filterTextProperty = SimpleStringProperty("")
    val filterNonFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterRarityCommon = SimpleBooleanProperty(true)
    val filterRarityUncommon = SimpleBooleanProperty(true)
    val filterRarityRare = SimpleBooleanProperty(true)
    val filterRarityMythic = SimpleBooleanProperty(true)

    lateinit var bulkAddNumberSpinner: Spinner<Number>
    lateinit var tvCards: TableView<CardInfo>
	lateinit var toggleButtonImageLoading: ToggleButton

    val cards = FXCollections.observableArrayList<CardInfo>()
    val cardsFiltered = cards.filtered { true }

    val buttonTypeExport = ButtonType("Export")

    fun updateCards() {
        cards.setAll(transaction {
            set.cards.toList().map { CardInfo(it) }
        })
    }

    fun bulkAdd() { // TODO update card set view
        transaction {
            cards.forEach { cardInfo ->
                cardInfo.bulkAdditionNonPremium.value.let { toBeAdded ->
                    if (toBeAdded > 0) {
                        repeat(toBeAdded) {
                            CardPossession.new {
                                this.card = cardInfo.card
                                this.language = languageProperty.value
                                this.condition = conditionProperty.value
                                this.foil = Foil.NONFOIL
                            }
                        }
                    }
                }
                cardInfo.bulkAdditionPremium.value.let { toBeAdded ->
                    if (toBeAdded > 0) {
                        repeat(toBeAdded) {
                            CardPossession.new {
                                this.card = cardInfo.card

                                this.language = languageProperty.value
                                this.condition = conditionProperty.value
                                this.foil = Foil.FOIL
                            }
                        }
                    }
                }
            }
        }
    }

    fun setFilter(filterRarityCommon: Boolean, filterRarityUncommon: Boolean, filterRarityRare: Boolean, filterRarityMythic: Boolean) {
        cardsFiltered.setPredicate { cardInfo ->
                (filterRarityCommon || cardInfo.card.rarity != Rarity.COMMON)
                && (filterRarityUncommon || cardInfo.card.rarity != Rarity.UNCOMMON)
                && (filterRarityRare || cardInfo.card.rarity != Rarity.RARE)
                && (filterRarityMythic || cardInfo.card.rarity != Rarity.MYTHIC)
        }
    }

    init {
        initOwner(owner.primaryStage)

        updateCards()
        val filterChangeListener = ChangeListener<Any> { _, _, _ ->
            setFilter(filterRarityCommon.get(), filterRarityUncommon.get(), filterRarityRare.get(), filterRarityMythic.get())
        }
        filterRarityCommon.addListener(filterChangeListener)
        filterRarityUncommon.addListener(filterChangeListener)
        filterRarityRare.addListener(filterChangeListener)
        filterRarityMythic.addListener(filterChangeListener)

        isResizable = true
        title = "Bulk Addition: ${set.name}"

        dialogPane.apply {
            content = BorderPane().apply {
				top {
					toolbar {
						toggleButtonImageLoading = togglebutton("\uD83D\uDDBC") {
							isSelected = imageLoading
						}
						label("Filter: ")
						/*textfield(filterTextProperty) {

						}*/
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
					}
				}
                left {
                    form {
						fieldset("Addition Setup") { // TODO move to own dialog
							field("Language") {
								combobox(languageProperty, listOf("en", "de", "jp", "ru", "it", "sp"))
							}
							field("Condition") {
								combobox(conditionProperty, Condition.values().asList())
							}
						}
                        fieldset("Card Info") {
							this += find<CardDetailsView>().apply {
								tornadofx.runLater {
									this.cardProperty.bind(tvCards.selectionModel.selectedItemProperty().mapBinding { it?.card })
									this.imageLoadingProperty.bind(toggleButtonImageLoading.selectedProperty())
								}
							}
                        }
                        fieldset("Current Addition") {
                        field("Foil") {
                                combobox(foilProperty, Foil.values().asList())
                            }
                            field("Number") {
                                bulkAddNumberSpinner = spinner(0, 999, bulkAddNumberProperty.value, 1, true, property = bulkAddNumberProperty) {
                                    this.editor.onKeyPressed = EventHandler {
                                        when (it.code) {
                                            KeyCode.UP -> {
                                                foilProperty.value = Foil.NONFOIL
                                                tvCards.selectionModel.selectPrevious()
                                            }
                                            KeyCode.DOWN -> {
                                                foilProperty.value = Foil.NONFOIL
                                                tvCards.selectionModel.selectNext()
                                            }
                                            KeyCode.ENTER -> {
												commitValue()
                                                when(foilProperty.value) {
                                                    Foil.NONFOIL -> tvCards.selectionModel.selectedItem.bulkAdditionNonPremium.set(bulkAddNumberProperty.get())
                                                    Foil.FOIL -> tvCards.selectionModel.selectedItem.bulkAdditionPremium.set(bulkAddNumberProperty.get())
                                                    // TODO addition premium else ->
                                                }

                                                if (it.isShiftDown) {
                                                    foilProperty.value = Foil.values()[foilProperty.value.ordinal + 1]
                                                    bulkAddNumberProperty.set(0)
                                                    bulkAddNumberSpinner.editor.selectAll()

                                                } else {
                                                    foilProperty.value = Foil.NONFOIL
                                                    tvCards.selectionModel.selectNext()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        buttonbar {
                            button("<") {
                                action {
                                    tvCards.selectionModel.selectPrevious()
                                }
                            }
                            button("Add") {
                                action {
                                    when(foilProperty.value) {
                                        Foil.NONFOIL -> tvCards.selectionModel.selectedItem.bulkAdditionNonPremium.set(bulkAddNumberProperty.get())
                                        Foil.FOIL -> tvCards.selectionModel.selectedItem.bulkAdditionPremium.set(bulkAddNumberProperty.get())
                                        // TODO addition premium else ->
                                    }

                                    tvCards.selectionModel.selectNext()
                                }
                            }
                            button(">") {
                                action {
                                    tvCards.selectionModel.selectNext()
                                }
                            }
                        }
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

                        column("CardSet Number", CardInfo::numberInSet) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }
                        column("Rarity", CardInfo::rarity) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

                        column("Name", CardInfo::name).remainingWidth()

                        column("Add", CardInfo::bulkAdditionNonPremium) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

                        column("Add Premium", CardInfo::bulkAdditionPremium) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

                        selectionModel.selectionMode = SelectionMode.SINGLE
                        selectionModel.selectedItemProperty().addListener { _, _, _ ->
							if(toggleButtonImageLoading.isSelected) {
								runAsync {
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
						runLater { selectionModel.selectFirst() }
                    }
                }
            }

            buttonTypes.setAll(ButtonType.APPLY, buttonTypeExport, ButtonType.CANCEL)
        }

        setResultConverter { button ->
            when (button) {
                ButtonType.APPLY -> {
                    bulkAdd()
                    true
                }
                buttonTypeExport -> {
                    transaction {
                        Paths.get("D:\\export.csv").toFile().printWriter().use { out ->
                            out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")
                            cards.forEach { cardInfo ->
                                val cardName = cardInfo.card.name
                                val cardNumberInSet = cardInfo.card.numberInSet
                                val token = cardInfo.card.token
                                val promo = cardInfo.card.promo
                                val condition = when (conditionProperty.value) {
                                    Condition.NEAR_MINT -> "Near Mint"
                                    Condition.EXCELLENT -> "Good (Lightly Played)"
                                    Condition.GOOD -> "Played"
                                    Condition.PLAYED -> "Heavily Played"
                                    Condition.POOR -> "Poor"
                                    else -> throw Exception("unknown condition")
                                }
                                val prereleasePromo = false
                                val language = when (languageProperty.value) {
                                    "en" -> "English"
                                    "de" -> "German"
                                    "jp" -> "Japanese"
                                    "ru" -> "Russian"
                                    else -> throw Exception("unknown language")
                                }
                                val setName = when {
                                    prereleasePromo -> "Prerelease Events: ${set.name}"
                                    token -> "Extras: ${set.name}"
                                    else -> set.name
                                }

                                cardInfo.bulkAdditionNonPremium.value.let { toBeAdded ->
                                    if (toBeAdded > 0) {
                                        out.println("$toBeAdded,0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,,,,,,,,")
                                    }
                                }
                                cardInfo.bulkAdditionPremium.value.let { toBeAdded ->
                                    if (toBeAdded > 0) {
                                        out.println("$toBeAdded,0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,foil,,,,,,,")
                                    }
                                }
                            }
                        }
                    }
                    bulkAdd()
                    true
                }
                else -> false
            }
        }
    }
}
