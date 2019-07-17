package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.Condition
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Foil
import at.woolph.caco.datamodel.sets.Rarity
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.importer.sets.importCardsOfSet
import at.woolph.caco.importer.sets.importPromosOfSet
import at.woolph.caco.importer.sets.importTokensOfSet
import at.woolph.libs.ktfx.commitValue
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.shape.Shape
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*

class BulkAdditionDialog(val set: CardSet, val owner: View, imageLoading: Boolean): Dialog<Boolean>() {

    inner class CardInfo(val card: Card) {
        val rarity get() = card.rarity.toProperty()
        val name get() = card.name.toProperty()
        val numberInSet get() = card.numberInSet.toProperty()
        val bulkAdditionNonPremium = SimpleIntegerProperty(0)
        val bulkAdditionPremium = SimpleIntegerProperty(0)

        val cardImage by lazy {
            Image(card.image.toString(), 224.0, 312.0, true, true)
        }
    }

    val cardName = SimpleStringProperty("")
    val cardNumberInSet = SimpleStringProperty("")

    val languageProperty = SimpleStringProperty("en")
    val conditionProperty = SimpleObjectProperty(Condition.NEAR_MINT)
    val foilProperty = SimpleObjectProperty(Foil.NONFOIL)

	val imageLoadingProperty = SimpleBooleanProperty(imageLoading)

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
    lateinit var imageView: ImageView
    lateinit var imageLoadingProgressIndicatorBackground: Shape
    lateinit var imageLoadingProgressIndicator: ProgressIndicator
    lateinit var tvCards: TableView<CardInfo>

    val cards = FXCollections.observableArrayList<CardInfo>()
    val cardsFiltered = cards.filtered { true }

    fun updateCards() {
        cards.setAll(transaction {
            set.cards.toList().map { CardInfo(it) }
        })
    }

    fun bulkAdd() { // TODO update card set view
        cards.forEach { cardInfo ->
            cardInfo.bulkAdditionNonPremium.value.let { toBeAdded ->
                if (toBeAdded > 0) {
                    transaction {
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
            }
            cardInfo.bulkAdditionPremium.value.let { toBeAdded ->
                if (toBeAdded > 0) {
                    transaction {
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

	fun loadImage() {
		if(imageLoadingProperty.get()) {
			runAsync {
				imageLoadingProgressIndicatorBackground.isVisible = true
				imageLoadingProgressIndicator.isVisible = true
				tvCards.selectedItem?.cardImage
			} ui {
				imageView.image = it
				imageLoadingProgressIndicator.isVisible = false
				imageLoadingProgressIndicatorBackground.isVisible = false
			}

			// preload surrounding images
			runAsync {
				listOf(tvCards.selectionModel.selectedIndex + 1,
						tvCards.selectionModel.selectedIndex - 1,
						tvCards.selectionModel.selectedIndex + 2,
						tvCards.selectionModel.selectedIndex + 3).forEach {
					if (0 <= it && it < tvCards.items.size) {
						tvCards.items[it].cardImage
					}
				}
			}
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

		imageLoadingProperty.addListener { _, _, newValue ->
			if(newValue) {
				loadImage()
			} else {
				imageView.image = null
			}
		}

        isResizable = true
        title = "Bulk Addition: ${set.name}"

        dialogPane.apply {
            content = BorderPane().apply {
                /*left {
                    form {
                        fieldset {
                            field("CardSet Code") {
                                textfield(setCodeProperty) {
                                    hgrow = Priority.ALWAYS
                                    runLater { requestFocus() }
                                }
                            }
                        }
                    }
                }*/
				top {
					toolbar {
						togglebutton("\uD83D\uDDBC") {
							isSelected = imageLoadingProperty.get()
							imageLoadingProperty.bind(selectedProperty())
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
                            field("CardSet Number") {
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
                        fieldset("Current Addition") {
                        field("Foil") {
                                combobox(foilProperty, Foil.values().asList())
                            }
                            field("Number") {
                                bulkAddNumberSpinner = spinner(0, 999, bulkAddNumberProperty.value, 1, true, property = bulkAddNumberProperty) {
                                    this.editor.onKeyPressed = javafx.event.EventHandler {
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
                            hGrow = javafx.scene.layout.Priority.ALWAYS
                        }
                        vboxConstraints {
                            vGrow = javafx.scene.layout.Priority.ALWAYS
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
                        selectionModel.selectedItemProperty().addListener { _, _, newCard ->
                            newCard?.let {
                                cardNumberInSet.set(it.numberInSet.get())
                                cardName.set(it.name.get())

								tornadofx.runLater {
									bulkAddNumberProperty.set(0)
									bulkAddNumberSpinner.requestFocus()
									bulkAddNumberSpinner.editor.selectAll()
								}

								loadImage()
                            }
                        }
						runLater { selectionModel.selectFirst() }
                    }
                }
            }

            buttonTypes.setAll(ButtonType.APPLY, ButtonType.CANCEL)
        }

        setResultConverter { button ->
            when (button) {
                ButtonType.APPLY -> {
                    bulkAdd()
                    true
                }
                else -> false
            }
        }
    }
}
