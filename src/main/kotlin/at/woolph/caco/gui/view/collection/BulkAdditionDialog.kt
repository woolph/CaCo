package at.woolph.caco.gui.view.collection

import at.woolph.caco.collection.CardCollectionItem
import at.woolph.caco.collection.CardCollectionItemId
import at.woolph.caco.collection.exportArchidekt
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVariant
import at.woolph.caco.datamodel.sets.Foil
import at.woolph.caco.datamodel.sets.Rarity
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.gui.view.CardDetailsView
import at.woolph.caco.gui.view.filteredBy
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

class BulkAdditionDialog(val collectionSettings: CollectionSettings, val set: ScryfallCardSet, val owner: View, imageLoading: Boolean, selection: Card? = null): Dialog<Boolean>() {

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

    fun updateCards() {
        cards.setAll(transaction {
            set.cards.toList().map { CardModel(it) }
        })
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
						fieldset("Addition Setup") {
							field("Language") {
								combobox(languageProperty, CardLanguage.entries)
							}
							field("CardCondition") {
								combobox(conditionProperty, CardCondition.entries)
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

                        column("#", CardModel::collectorNumber) {
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
                            bulkAddNumberTextField.clear()
                            bulkAddNumberTextField.requestFocus()
                        }
                       if(selection != null) {
                         val item = items.first { it.item.id == selection.id }
                         selectionModel.select(item)
                         scrollTo(item)
                       } else
                           selectionModel.selectFirst()
                    }
                }
            }
        }


        buttons {
            defaultAction { false }
            button(ButtonType.APPLY) {
                action {
                  val cardCollectionItems = cards.flatMap { cardInfo -> listOf(
                    CardCollectionItem(
                      cardInfo.bulkAdditionNonPremium.value.toUInt(),
                      CardCollectionItemId(
                        card = cardInfo.item,
                        foil = false,
                        language = languageProperty.value,
                        condition = conditionProperty.value,
                      )
                    ),
                    CardCollectionItem(
                      cardInfo.bulkAdditionPremium.value.toUInt(),
                      CardCollectionItemId(
                        card = cardInfo.item,
                        foil = true,
                        language = languageProperty.value,
                        condition = conditionProperty.value,
                      )
                    ),
                    CardCollectionItem(
                      cardInfo.bulkAdditionPrereleasePromo.value.toUInt(),
                      CardCollectionItemId(
                        card = cardInfo.item,
                        foil = true,
                        language = languageProperty.value,
                        condition = conditionProperty.value,
                        variantType = CardVariant.Type.PrereleaseStamped,
                      )
                    ),
                  ) }.filter(CardCollectionItem::isNotEmpty)

                  transaction {
                    chooseFile("Choose File to Export to", arrayOf(FileChooser.ExtensionFilter("CSV", "*.csv")),  mode = FileChooserMode.Save, initialDirectory = File(System.getProperty("user.home"))).single().let {
                      cardCollectionItems.exportArchidekt(it.toPath())
                    }
                    cardCollectionItems.forEach(CardCollectionItem::addToCollection)
                  }
                  true
                }
            }
            button(ButtonType.CANCEL)
        }
    }
}
