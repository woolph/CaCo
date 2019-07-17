package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.Condition
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Foil
import at.woolph.caco.importer.collection.getLatestDeckboxCollectionExport
import at.woolph.caco.importer.collection.importDeckbox
import at.woolph.pdf.*
import javafx.scene.control.ToolBar
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.action
import tornadofx.button
import tornadofx.observable
import java.io.File
import java.nio.file.Paths
import kotlin.math.max


class EnglishPaperCollectionView: CollectionView() {
    override val cardPossesionTargtNonPremium get() = 4
    override val cardPossesionTargtPremium get() = 1

    override fun Card.getPossesionsNonPremium() = this.possessions.filter { it.language == "en" && !it.foil.isFoil }.count()
    override fun Card.getPossesionsPremium() = this.possessions.filter { it.language == "en" && it.foil.isFoil }.count()

    override fun Card.filterView(): Boolean = true

    override fun getRelevantSets() = transaction {
        CardSet.all().toList().filter { !it.digitalOnly }.observable().sorted { t1: CardSet, t2: CardSet ->
            -t1.dateOfRelease.compareTo(t2.dateOfRelease)
        }
    }

    override fun ToolBar.addFeatureButtons() {
        button("Import Collection") {
            action {
                getLatestDeckboxCollectionExport(File("D:\\woolph\\Downloads\\"))?. let { importDeckbox(it) }
            }
        }
        button("Print Inventory") {
            action {
                val fontTitle = Font(PDType1Font.HELVETICA_BOLD, 10f)

                transaction {
                    createPdfDocument(Paths.get("D:\\woolph\\Dropbox\\mtg-inventory.pdf")) {
                        CardSet.all().sortedByDescending { it.dateOfRelease }.forEach { set ->
                            page(PDRectangle.A4) {
                                frame(PagePosition.RIGHT, 50f, 20f, 20f, 20f) {
                                    drawText("Inventory ${set.name}", fontTitle, be.quodlibet.boxable.HorizontalAlignment.CENTER, box.upperRightY - 10f, java.awt.Color.BLACK)

                                    // TODO calc metrics for all sets (so that formatting is the same for all pages)
                                    set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.let {
                                        frame(marginTop = fontTitle.height + 20f) {
                                            columns((it.size - 1) / 100 + 1, 100, 5f, 3.5f, Font(PDType1Font.HELVETICA, 6.0f)) {
                                                var i = 0
                                                it.filter { !it.token }.forEach {
                                                    val ownedCountEN = it.possessions.filter { it.language == "en" }.count()
                                                    val ownedCountDE = it.possessions.filter { it.language == "de" }.count()
                                                    this@columns.get(i) {
                                                        drawTextWithRects("${it.rarity} ${it.numberInSet} ${it.name}", ownedCountEN, ownedCountDE)
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
                                                    val ownedCountEN = it.possessions.filter { it.language == "en" }.count()
                                                    val ownedCountDE = it.possessions.filter { it.language == "de" }.count()
                                                    this@columns.get(i) {
                                                        drawTextWithRects("T ${it.numberInSet} ${it.name}", ownedCountEN, ownedCountDE)
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
        button("Export Collection") {
            action {
                transaction {
                    Paths.get("D:\\export.csv").toFile().printWriter().use { out ->
                        out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")

                        set?.cards?.forEach {
                            ((Cards innerJoin CardPossessions).slice(CardPossessions.id.count(), Cards.name, Cards.token, Cards.promo, Cards.numberInSet, CardPossessions.condition, CardPossessions.foil, CardPossessions.language).select { CardPossessions.card.eq(it.id) }.groupBy(CardPossessions.card, CardPossessions.condition, CardPossessions.foil, CardPossessions.language)).forEach {
                                val count = it[CardPossessions.id.count()]
                                val cardName = it[Cards.name]
                                val cardNumberInSet = it[Cards.numberInSet]
                                val token = it[Cards.token]
                                val promo = it[Cards.promo]
                                val condition = when (it[CardPossessions.condition]) {
                                    Condition.NEAR_MINT -> "Near Mint"
                                    Condition.EXCELLENT -> "Good (Lightly Played)"
                                    Condition.GOOD -> "Played"
                                    Condition.PLAYED -> "Heavily Played"
                                    Condition.POOR -> "Poor"
                                    else -> throw Exception("unknown condition")
                                }
                                val prereleasePromo = it[CardPossessions.foil] == Foil.PRERELASE_STAMPED_FOIL
                                val foil = if (it[CardPossessions.foil].isFoil) "foil" else ""
                                val language = when (it[CardPossessions.language]) {
                                    "en" -> "English"
                                    "de" -> "German"
                                    "jp" -> "Japanese"
                                    "ru" -> "Russian"
                                    else -> throw Exception("unknown language")
                                }
                                val setName = when {
                                    prereleasePromo -> "Prerelease Events: ${set?.name}"
                                    token -> "Extras: ${set?.name}"
                                    else -> set?.name
                                }

                                out.println("$count,0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,$foil,,,,,,,")
                            }
                        }
                    }
                }
            }
        }
        button("Export Missing") {
            action {
                transaction {
                    Paths.get("D:\\wants.txt").toFile().printWriter().use { out ->
                        set?.cards?.sortedBy { it.numberInSet }?.filter { !it.promo }?.forEach {
                            val neededCount = max(0, cardPossesionTargtNonPremium - it.possessions.count())
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
