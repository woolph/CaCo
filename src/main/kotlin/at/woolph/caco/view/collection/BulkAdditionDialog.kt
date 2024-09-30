package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Foil
import at.woolph.caco.datamodel.sets.Rarity
import at.woolph.caco.importer.collection.setNameMapping
import at.woolph.caco.importer.collection.toDeckboxCondition
import at.woolph.caco.importer.collection.toLanguageDeckbox
import at.woolph.caco.view.CardDetailsView
import at.woolph.caco.view.filteredBy
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.FileChooserMode
import tornadofx.View
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.center
import tornadofx.chooseFile
import tornadofx.column
import tornadofx.combobox
import tornadofx.contentWidth
import tornadofx.field
import tornadofx.fieldset
import tornadofx.find
import tornadofx.form
import tornadofx.hboxConstraints
import tornadofx.label
import tornadofx.left
import tornadofx.plusAssign
import tornadofx.region
import tornadofx.remainingWidth
import tornadofx.runLater
import tornadofx.tableview
import tornadofx.textfield
import tornadofx.togglebutton
import tornadofx.toolbar
import tornadofx.tooltip
import tornadofx.top
import tornadofx.vboxConstraints
import java.io.File

class BulkAdditionDialog(val collectionSettings: CollectionSettings, val set: CardSet, val owner: View, imageLoading: Boolean, selection: Card? = null): Dialog<Boolean>() {

    inner class CardModel(card: Card): CardPossessionModel(card, collectionSettings) {
        val bulkAdditionNonPremium = SimpleIntegerProperty(0)
        val bulkAdditionPremium = SimpleIntegerProperty(0)
		val bulkAdditionPrereleasePromo = SimpleIntegerProperty(0)
    }

    val languageProperty = SimpleObjectProperty(CardLanguage.ENGLISH)
    val conditionProperty = SimpleObjectProperty(CardCondition.NEAR_MINT)
    val foilProperty = SimpleObjectProperty(Foil.NONFOIL)

    val filterTextProperty = SimpleStringProperty("")
    val filterRarityCommon = SimpleBooleanProperty(true)
    val filterRarityUncommon = SimpleBooleanProperty(true)
    val filterRarityRare = SimpleBooleanProperty(true)
    val filterRarityMythic = SimpleBooleanProperty(true)

    lateinit var bulkAddNumberTextField: TextField
    lateinit var tvCards: TableView<CardModel>
	lateinit var toggleButtonImageLoading: ToggleButton

    val cards = FXCollections.observableArrayList<CardModel>()
    val cardsSorted = cards.sorted()
    val cardsFiltered = cardsSorted.filteredBy(listOf(
        filterTextProperty,
        filterRarityCommon,
        filterRarityUncommon,
        filterRarityRare,
        filterRarityMythic,
    )) { cardInfo -> (filterTextProperty.get().isBlank() || cardInfo.names.any { it.contains(filterTextProperty.get(), ignoreCase = true) })
            && (filterRarityCommon.get() || cardInfo.rarity.value != Rarity.COMMON)
            && (filterRarityUncommon.get() || cardInfo.rarity.value != Rarity.UNCOMMON)
            && (filterRarityRare.get() || cardInfo.rarity.value != Rarity.RARE)
            && (filterRarityMythic.get() || cardInfo.rarity.value != Rarity.MYTHIC)
    }

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

    init {
        initOwner(owner.primaryStage)

        updateCards()

        isResizable = true
        title = "Bulk Addition: ${set.name}"

        dialogPane {
            content = borderpane {
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
                                   it.bulkAdditionNonPremium.set(1)
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
								runLater {
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
                                            else -> {}
                                        }

                                        System.console()
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

                        column("Possession", CardModel::possessionTotal) {
                            contentWidth(5.0, useAsMin = true, useAsMax = true)
                        }

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
                                // TODO make working again
//								runAsync {
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
        }


        buttons {
            defaultAction { false }
            button(ButtonType.APPLY) {
                action {
                    chooseFile("Choose File to Export to", arrayOf(FileChooser.ExtensionFilter("CSV", "*.csv")),  mode = FileChooserMode.Save, initialDirectory = File(System.getProperty("user.home"))).single().let {
                        transaction {
                            it.printWriter().use { out ->
                                out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")
                                cards.forEach { cardInfo ->
                                    val cardName = cardInfo.item.name
                                    val cardNumberInSet = cardInfo.item.numberInSet
                                    val token = cardInfo.item.token
                                    val promo = cardInfo.item.promo
                                    val condition = conditionProperty.value.toDeckboxCondition()
                                    val prereleasePromo = false
                                    val language = languageProperty.value.toLanguageDeckbox()
                                    val setName = setNameMapping.asSequence().firstOrNull { it.value == set.name }?.key ?: set.name?.let {
                                        when {
                                            prereleasePromo -> "Prerelease Events: ${it}"
                                            token -> "Extras: ${it}"
                                            else -> it
                                        }
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
            }
            button(ButtonType.CANCEL)
        }
    }
}
