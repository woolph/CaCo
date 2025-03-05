package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.collection.importDeckbox
import at.woolph.caco.collection.toLanguageDeckbox
import at.woolph.libs.pdf.*
import javafx.scene.control.ToolBar
import javafx.stage.FileChooser
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class PaperCollectionView: CollectionView(COLLECTION_SETTINGS) {
	// TODO collection modification matrix (language, condition, premium)
	companion object {
		val COLLECTION_SETTINGS = CollectionSettings(
			1,
			0,
			33,
			{ !it.digitalOnly },
			{ it.possessions.count { !it.foil } },
			{ it.possessions.count { it.foil } },
		)
	}

    override fun CardPossessionModel.filterView(): Boolean = true

    override fun ToolBar.addFeatureButtons() {
        button("Bulk Add") {
            action(coroutineScope) {
				set?.let {
					newSuspendedTransaction {
						BulkAdditionDialog(collectionSettings, it, this@PaperCollectionView, toggleButtonImageLoading.isSelected, tvCards.selectedItem?.item).showAndAwait()?.let {
							updateCards()
						}
					}
				}
            }
        }
        button("Import Collection") {
            action(coroutineScope) {
				// TODO progress dialog
				chooseFile("Open Image", arrayOf(FileChooser.ExtensionFilter("Deckbox Export", "*.csv"))).singleOrNull()?.let {
					importDeckbox(it.toPath())
					updateCards()
				}
            }
        }
        button("Print Inventory") {

            action(coroutineScope) {
				chooseFile("Choose File to Print Inventory", arrayOf(FileChooser.ExtensionFilter("PDF", "*.pdf")), mode = FileChooserMode.Save).singleOrNull()?.let {
					// TODO progress dialog
					val fontTitle = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)

					transaction {
						createPdfDocument(it.toPath()) {
							CardSet.all().sortedByDescending { it.dateOfRelease }.forEach { set ->
								page(PDRectangle.A4) {
									framePagePosition(50f, 20f, 20f, 20f) {
										drawText("Inventory ${set.name}", fontTitle, HorizontalAlignment.CENTER, 0f, box.upperRightY - 10f, Color.BLACK)

										// TODO calc metrics for all sets (so that formatting is the same for all pages)
										set.cards.sortedBy { it.collectorNumber }.filter { !it.promo }.let {
											frame(marginTop = fontTitle.height + 20f) {
												columns((it.size - 1) / 100 + 1, 100, 5f, 3.5f, Font(PDType1Font(Standard14Fonts.FontName.HELVETICA), 6.0f)) {
													var i = 0
													it.filter { !it.token }.forEach {
                                                        val ownedCountEN = it.possessions.count { it.language == CardLanguage.ENGLISH }
                                                        val ownedCountDE = it.possessions.count { it.language == CardLanguage.GERMAN }
														this@columns.get(i) {
															drawTextWithRects("${it.rarity} ${it.collectorNumber} ${it.name}", ownedCountEN, ownedCountDE)
														}
														i++
													}

													i++
													/*
                                                this@columns.get(i) {
                                                    drawText("Tokens", Color.BLACK)
                                                }
                                                i++
                                                */
													it.filter { it.token }.forEach {
                                                        val ownedCountEN = it.possessions.count { it.language == CardLanguage.ENGLISH }
                                                        val ownedCountDE = it.possessions.count { it.language == CardLanguage.GERMAN }
														this@columns.get(i) {
															drawTextWithRects("T ${it.collectorNumber} ${it.name}", ownedCountEN, ownedCountDE)
														}
														i++
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
            }
        }
        button("Export Collection") {
            action(coroutineScope) {
				// TODO progress dialog
				chooseFile("Choose File to Export to", arrayOf(FileChooser.ExtensionFilter("CSV", "*.csv")), mode = FileChooserMode.Save).singleOrNull()?.let {
					it.printWriter().use { out ->
						transaction {
							out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")

							set?.cards?.forEach {
								((Cards innerJoin CardPossessions)
										.select(CardPossessions.id.count(), Cards.name, Cards.token, Cards.promo, Cards.collectorNumber, CardPossessions.condition, CardPossessions.foil, CardPossessions.language)
										.where { CardPossessions.card.eq(it.id) }
										.groupBy(CardPossessions.card, CardPossessions.condition, CardPossessions.foil, CardPossessions.language))
										.forEach {
											val count = it[CardPossessions.id.count()]
											val cardName = it[Cards.name]
											val cardNumberInSet = it[Cards.collectorNumber]
											val token = it[Cards.token]
											val promo = it[Cards.promo]
											val condition = when (it[CardPossessions.condition]) {
												CardCondition.NEAR_MINT -> "Near Mint"
												CardCondition.EXCELLENT -> "Good (Lightly Played)"
												CardCondition.GOOD -> "Played"
												CardCondition.PLAYED -> "Heavily Played"
												CardCondition.POOR -> "Poor"
												else -> throw Exception("unknown condition")
											}
											val prereleasePromo = it[CardPossessions.stampPrereleaseDate]
											val promostamped = if(it[CardPossessions.stampPlaneswalkerSymbol]) "promo" else ""
											val foil = if (it[CardPossessions.foil]) "foil" else ""

											val language = it[CardPossessions.language].toLanguageDeckbox()
											val setName = when {
												prereleasePromo -> "Prerelease Events: ${set?.name}"
												token -> "Extras: ${set?.name}"
												else -> set?.name
											}

											out.println("$count,0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,$foil,,,,,$promostamped,,")
										}
							}
						}
					}
				}
            }
        }
        button("Export Missing") {
            action {
				Toolkit.getDefaultToolkit()
					.systemClipboard
					.setContents(
						StringSelection(transaction {
							cardsSorted.asSequence().map { card ->
								val neededCount = kotlin.math.max(0, card.possessionNonPremiumTarget.value - card.possessionNonPremium.value)
								val suffixName = if (cardsSorted.asSequence().filter { it2 ->
									it2.name.value == card.name.value  && it2.extra.value == card.extra.value
								}.count() > 1) {
									val numberInSetWithSameName = cardsSorted.asSequence().filter { it2 ->
										it2.name.value == card.name.value && it2.extra.value == card.extra.value && it2.numberInSet.value < card.numberInSet.value
									}.count() + 1
									" (V.$numberInSetWithSameName)"
								} else {
									""
								}
								val suffixSet = when {
									card.promo.value -> ": Promos"
									card.extra.value -> ": Extras"
									else -> ""
								}
								Triple(neededCount, "${card.name.value}$suffixName", card.set.value?.set?.name?.let { "$it$suffixSet" })
							}.filter { it.first > 0 }.joinToString("\n") {
									"${it.first} ${it.second} (${it.third})"
							}
						}),
						null)
//				// TODO progress dialog
//				chooseFile("Choose File to Wants", arrayOf(FileChooser.ExtensionFilter("Text", "*.txt")), mode = FileChooserMode.Save).singleOrNull()?.let {
//					it.printWriter().use { out ->
//						transaction {
//							set?.cards?.sortedBy { it.numberInSet }?.filter { !it.promo }?.forEach {
//								val neededCount = kotlin.math.max(0, collectionSettings.cardPossesionTargtNonPremium - it.possessions.count())
//								//val n = if(set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
//								if (neededCount > 0) {
//									out.println("${neededCount} ${it.name} (${set?.name})")
//								}
//							}
//						}
//					}
//				}
            }
        }
    }
}
