package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Foil
import at.woolph.caco.datamodel.sets.Rarity
import at.woolph.caco.importer.collection.toLanguageDeckbox
import at.woolph.caco.view.CardDetailsView
import at.woolph.caco.view.getCachedImage
import at.woolph.libs.ktfx.commitValue
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.io.File


fun <S> TableView<S>.getNextEditableColumn(forward: Boolean, currentCol: TableColumn<S, out Any>?): TableColumn<S, *>? {
	val columns = this.columns.filter { it.isEditable && it.isVisible && (it.styleClass.contains("editable-col") || it.styleClass.contains("result-col")) }.toList()

	if (columns.size < 2) {
		return null
	}

	val currentIndex = columns.indexOf(currentCol)

	val nextIndex = if (forward) {
			if (currentIndex >= columns.size - 1) 0 else currentIndex+1
		} else {
			if (currentIndex <= 0) columns.size - 1 else currentIndex-1
		}
	return columns[nextIndex]
}

class BulkAdditionDialog(val set: CardSet, val owner: View, imageLoading: Boolean, selection: Card? = null): Dialog<Boolean>() {

    inner class CardModel(card: Card): at.woolph.caco.view.collection.CardModel(card) {
        val bulkAdditionNonPremium = SimpleIntegerProperty(0)
        val bulkAdditionPremium = SimpleIntegerProperty(0)
		val bulkAdditionPrereleasePromo = SimpleIntegerProperty(0)
    }

    val languageProperty = SimpleObjectProperty(CardLanguage.ENGLISH)
    val conditionProperty = SimpleObjectProperty(CardCondition.NEAR_MINT)
    val foilProperty = SimpleObjectProperty(Foil.NONFOIL)

    val filterTextProperty = SimpleStringProperty("")
    val filterNonFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterFoilCompleteProperty = SimpleBooleanProperty(true)
    val filterRarityCommon = SimpleBooleanProperty(true)
    val filterRarityUncommon = SimpleBooleanProperty(true)
    val filterRarityRare = SimpleBooleanProperty(true)
    val filterRarityMythic = SimpleBooleanProperty(true)

    lateinit var bulkAddNumberTextField: TextField
    lateinit var tvCards: TableView<CardModel>
	lateinit var toggleButtonImageLoading: ToggleButton

    val cards = FXCollections.observableArrayList<CardModel>()
    val cardsFiltered = cards.filtered { true }

    val buttonTypeExport = ButtonType("Export")

    fun updateCards() {
        cards.setAll(transaction {
            set.cards.toList().map { CardModel(it) }
        })
    }

    fun bulkAdd() { // TODO update card set view
        transaction {
            cards.forEach { cardInfo ->
                cardInfo.bulkAdditionNonPremium.value.let { toBeAdded ->
                    if (toBeAdded > 0) {
                        repeat(toBeAdded) {
                            CardPossession.new {
                                this.card = cardInfo.item
                                this.language = languageProperty.value
                                this.condition = conditionProperty.value
                                this.foil = false
                            }
                        }
                    }
                }
                cardInfo.bulkAdditionPremium.value.let { toBeAdded ->
                    if (toBeAdded > 0) {
                        repeat(toBeAdded) {
                            CardPossession.new {
                                this.card = cardInfo.item

                                this.language = languageProperty.value
                                this.condition = conditionProperty.value
                                this.foil = true
                            }
                        }
                    }
                }
				cardInfo.bulkAdditionPrereleasePromo.value.let { toBeAdded ->
					if (toBeAdded > 0) {
						repeat(toBeAdded) {
							CardPossession.new {
								this.card = cardInfo.item

								this.language = languageProperty.value
								this.condition = conditionProperty.value
								this.foil = true
								this.stampPrereleaseDate = true
							}
						}
					}
				}
            }
        }
    }

    fun setFilter(filterRarityCommon: Boolean, filterRarityUncommon: Boolean, filterRarityRare: Boolean, filterRarityMythic: Boolean) {
        cardsFiltered.setPredicate { cardInfo ->
                (filterRarityCommon || cardInfo.rarity.value != Rarity.COMMON)
                && (filterRarityUncommon || cardInfo.rarity.value != Rarity.UNCOMMON)
                && (filterRarityRare || cardInfo.rarity.value != Rarity.RARE)
                && (filterRarityMythic || cardInfo.rarity.value != Rarity.MYTHIC)
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
                        button("+1") {
                            action {
                               tvCards.items.forEach {
                                   it.bulkAdditionNonPremium.add(1)
                               }
                            }
                        }
					}
				}
                left {
                    form {
						fieldset("Addition Setup") { // TODO move to own dialog
							field("Language") {
								combobox(languageProperty, CardLanguage.values().toList())
							}
							field("CardCondition") {
								combobox(conditionProperty, CardCondition.values().asList())
							}
						}
                        fieldset("Card Info") {
							this += find<CardDetailsView>().apply {
								tornadofx.runLater {
									this.cardProperty.bind(tvCards.selectionModel.selectedItemProperty())
									this.imageLoadingProperty.bind(toggleButtonImageLoading.selectedProperty())
								}
							}
                        }
                        fieldset("Current Addition") {
                            field("Number") {
                                bulkAddNumberTextField = textfield {
                                    onKeyPressed = EventHandler {
                                        when (it.code) {
                                            KeyCode.UP -> {
                                                foilProperty.value = Foil.NONFOIL
                                                tvCards.selectionModel.selectPrevious()
                                            }
                                            KeyCode.DOWN -> {
                                                foilProperty.value = Foil.NONFOIL
                                                tvCards.selectionModel.selectNext()
                                            }
                                        }
                                    }
                                    onAction = EventHandler {
                                        val regex = Regex("(\\d+)?(\\*(\\d+))?(/(\\d+))?")
                                        regex.matchEntire(this.text)?.let {
                                            val nonfoils = it.groups[1]?.value?.toInt() ?: 0
                                            val foils = it.groups[3]?.value?.toInt() ?: 0
                                            val prereleaseStampedFoils = it.groups[5]?.value?.toInt() ?: 0

                                            tvCards.selectionModel.selectedItem.bulkAdditionNonPremium.set(nonfoils)
                                            tvCards.selectionModel.selectedItem.bulkAdditionPremium.set(foils)
                                            tvCards.selectionModel.selectedItem.bulkAdditionPrereleasePromo.set(prereleaseStampedFoils)
                                            tvCards.selectionModel.selectNext()
                                        }
                                    }
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

                        column("#", CardModel::numberInSet) {
							tooltip("Collector Number")
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }
                        column("R", CardModel::rarity) {
							tooltip("Rarity")
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

                        column("Name", CardModel::name).remainingWidth()

                        column("Add", CardModel::bulkAdditionNonPremium) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

                        column("Add Premium", CardModel::bulkAdditionPremium) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

						column("Add Prerelease Promo", CardModel::bulkAdditionPrereleasePromo) {
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
											tvCards.items[it].getCachedImage()
										}
									}
								}
							}
                            bulkAddNumberTextField.clear()
                            bulkAddNumberTextField.requestFocus()
                        }
						runLater {
                           if(selection != null)
                               selectionModel.select(items.find { it.item == selection })
                           else
                               selectionModel.selectFirst()

						}
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
					chooseFile("Choose File to Export to", arrayOf(FileChooser.ExtensionFilter("CSV", "*.csv")),  mode = FileChooserMode.Save, initialDirectory = File(System.getProperty("user.home"))).singleOrNull()?.let {
						transaction {
							it.printWriter().use { out ->
								out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")
								cards.forEach { cardInfo ->
									val cardName = cardInfo.item.name
									val cardNumberInSet = cardInfo.item.numberInSet
									val token = cardInfo.item.token
									val promo = cardInfo.item.promo
									val condition = when (conditionProperty.value) {
										CardCondition.NEAR_MINT -> "Near Mint"
										CardCondition.EXCELLENT -> "Good (Lightly Played)"
										CardCondition.GOOD -> "Played"
										CardCondition.PLAYED -> "Heavily Played"
										CardCondition.POOR -> "Poor"
										else -> throw Exception("unknown condition")
									}
									val prereleasePromo = false
									val language = languageProperty.value.toLanguageDeckbox()
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
					}
                    bulkAdd()
                    true
                }
                else -> false
            }
        }
    }
}
