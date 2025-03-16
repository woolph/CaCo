package at.woolph.caco.gui.view.collection

import at.woolph.caco.collection.asCardCollectionItems
import at.woolph.caco.collection.exportDeckbox
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.collection.importDeckbox
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.libs.pdf.*
import javafx.scene.control.ToolBar
import javafx.stage.FileChooser
import kotlinx.coroutines.launch
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import java.awt.Color
import kotlin.math.max


class EnglishPaperCollectionView: CollectionView(COLLECTION_SETTINGS) {
	companion object {
		val COLLECTION_SETTINGS = CollectionSettings(4, 1, 33,
				{ !it.digitalOnly },
				{ it.possessions.count { it.language == CardLanguage.ENGLISH && it.finish == Finish.Normal  } },
				{ it.possessions.count { it.language == CardLanguage.ENGLISH && it.finish != Finish.Normal  } })
	}

	override fun CardPossessionModel.filterView(): Boolean = true

    override fun ToolBar.addFeatureButtons() {
        button("Import Collection") {
            action {
				chooseFile("Open Image", arrayOf(FileChooser.ExtensionFilter("Deckbox Export", "*.csv"))).singleOrNull()?.let {
					coroutineScope.launch {
						importDeckbox(it.toPath())
						updateCards()
					}
				}
				// TODO progress dialog with console output
            }
        }
        button("Export Inventory") {
            action {
				chooseFile("Choose File to Print Inventory", arrayOf(FileChooser.ExtensionFilter("PDF", "*.pdf")), mode = FileChooserMode.Save).singleOrNull()?.let {
					val fontTitle = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)

					transaction {
						createPdfDocument(it.toPath()) {
							ScryfallCardSet.allRootSets().sortedByDescending { it.releaseDate }.forEach { set ->
								page(PDRectangle.A4) {
									framePagePosition(50f, 20f, 20f, 20f) {
										drawText("Inventory ${set.name}", fontTitle, HorizontalAlignment.CENTER, 0f, box.upperRightY - 10f, Color.BLACK)

										// TODO calc metrics for all sets (so that formatting is the same for all pages)
										set.cardsOfSelfAndNonRootChildSets.sorted().filter { !it.promo }.toList().let {
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
            action {
				// TODO progress dialog
				set?.item?.cards?.flatMap { CardPossession.find(it) }?.asCardCollectionItems()?.let { items ->
					chooseFile("Choose File to Export to", arrayOf(FileChooser.ExtensionFilter("CSV", "*.csv")), mode = FileChooserMode.Save).singleOrNull()?.let {
						items.exportDeckbox(it.toPath())
					}
				} ?: println("No items to export") // TODO show messagebox
            }
        }
        button("Export Missing") {
            action {
				chooseFile("Choose File to Wants", arrayOf(FileChooser.ExtensionFilter("Text", "*.txt")), mode = FileChooserMode.Save).singleOrNull()?.let {
					it.printWriter().use { out ->
						transaction {
							set?.item?.cards?.sortedBy { it.collectorNumber }?.filter { !it.promo }?.forEach {
								val neededCount = max(0, collectionSettings.cardPossesionTargtNonPremium - it.possessions.count())
								//val n = if(set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
								if (neededCount > 0) {
									out.println("${neededCount} ${it.name} (${set?.name})")
								}
							}
						}
					}
				}
            }
        }
    }
}
