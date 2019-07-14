package at.woolph.caco.view

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Set
import at.woolph.pdf.columns
import at.woolph.pdf.drawText
import at.woolph.pdf.frame
import at.woolph.pdf.page
import javafx.scene.control.ToolBar
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.action
import tornadofx.button
import tornadofx.observable


class EnglishPaperCollectionView: CollectionView() {
    override val cardPossesionTargtNonPremium get() = 4
    override val cardPossesionTargtPremium get() = 1

    override fun Card.getPossesionsNonPremium() = this.possessions.filter { it.language == "en" && !it.foil.isFoil }.count()
    override fun Card.getPossesionsPremium() = this.possessions.filter { it.language == "en" && it.foil.isFoil }.count()

    override fun Card.filterView(): Boolean = true

    override fun getRelevantSets() = transaction {
        Set.all().toList().filter { !it.digitalOnly }.observable().sorted { t1: Set, t2: Set ->
            -t1.dateOfRelease.compareTo(t2.dateOfRelease)
        }
    }

    override fun ToolBar.addFeatureButtons() {
        button("Import Collection") {
            action {
                at.woolph.caco.importer.collection.getLatestDeckboxCollectionExport(java.io.File("D:\\woolph\\Downloads\\"))?. let { at.woolph.caco.importer.collection.importDeckbox(it) }
            }
        }
        button("Print Inventory") {
            action {
                val fontTitle = at.woolph.pdf.Font(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10f)

                org.jetbrains.exposed.sql.transactions.transaction {
                    at.woolph.pdf.createPdfDocument(java.nio.file.Paths.get("D:\\woolph\\Dropbox\\mtg-inventory.pdf")) {
                        at.woolph.caco.datamodel.sets.Set.all().sortedByDescending { it.dateOfRelease }.forEach { set ->
                            page(org.apache.pdfbox.pdmodel.common.PDRectangle.A4) {
                                frame(at.woolph.pdf.PagePosition.RIGHT, 50f, 20f, 20f, 20f) {
                                    drawText("Inventory ${set.name}", fontTitle, be.quodlibet.boxable.HorizontalAlignment.CENTER, box.upperRightY - 10f, java.awt.Color.BLACK)

                                    // TODO calc metrics for all sets (so that formatting is the same for all pages)
                                    set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.let {
                                        frame(marginTop = fontTitle.height + 20f) {
                                            columns((it.size - 1) / 100 + 1, 100, 5f, 3.5f, at.woolph.pdf.Font(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 6.0f)) {
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
                org.jetbrains.exposed.sql.transactions.transaction {
                    java.nio.file.Paths.get("D:\\export.csv").toFile().printWriter().use { out ->
                        out.println("Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price")

                        set.cards.forEach {
                            ((at.woolph.caco.datamodel.sets.Cards innerJoin at.woolph.caco.datamodel.collection.CardPossessions).slice(at.woolph.caco.datamodel.collection.CardPossessions.id.count(), at.woolph.caco.datamodel.sets.Cards.name, at.woolph.caco.datamodel.sets.Cards.token, at.woolph.caco.datamodel.sets.Cards.promo, at.woolph.caco.datamodel.sets.Cards.numberInSet, at.woolph.caco.datamodel.collection.CardPossessions.condition, at.woolph.caco.datamodel.collection.CardPossessions.foil, at.woolph.caco.datamodel.collection.CardPossessions.language).select { at.woolph.caco.datamodel.collection.CardPossessions.card.eq(it.id) }.groupBy(at.woolph.caco.datamodel.collection.CardPossessions.card, at.woolph.caco.datamodel.collection.CardPossessions.condition, at.woolph.caco.datamodel.collection.CardPossessions.foil, at.woolph.caco.datamodel.collection.CardPossessions.language)).forEach {
                                val count = it[at.woolph.caco.datamodel.collection.CardPossessions.id.count()]
                                val cardName = it[at.woolph.caco.datamodel.sets.Cards.name]
                                val cardNumberInSet = it[at.woolph.caco.datamodel.sets.Cards.numberInSet]
                                val token = it[at.woolph.caco.datamodel.sets.Cards.token]
                                val promo = it[at.woolph.caco.datamodel.sets.Cards.promo]
                                val condition = when (it[at.woolph.caco.datamodel.collection.CardPossessions.condition]) {
                                    at.woolph.caco.datamodel.collection.Condition.NEAR_MINT -> "Near Mint"
                                    at.woolph.caco.datamodel.collection.Condition.EXCELLENT -> "Good (Lightly Played)"
                                    at.woolph.caco.datamodel.collection.Condition.GOOD -> "Played"
                                    at.woolph.caco.datamodel.collection.Condition.PLAYED -> "Heavily Played"
                                    at.woolph.caco.datamodel.collection.Condition.POOR -> "Poor"
                                    else -> throw Exception("unknown condition")
                                }
                                val prereleasePromo = it[at.woolph.caco.datamodel.collection.CardPossessions.foil] == at.woolph.caco.datamodel.sets.Foil.PRERELASE_STAMPED_FOIL
                                val foil = if (it[at.woolph.caco.datamodel.collection.CardPossessions.foil].isFoil) "foil" else ""
                                val language = when (it[at.woolph.caco.datamodel.collection.CardPossessions.language]) {
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

                                out.println("$count,0,\"$cardName\",\"$setName\",$cardNumberInSet,$condition,$language,$foil,,,,,,,")
                            }
                        }
                    }
                }
            }
        }
        button("Export Missing") {
            action {
                org.jetbrains.exposed.sql.transactions.transaction {
                    java.nio.file.Paths.get("D:\\wants.txt").toFile().printWriter().use { out ->
                        set.cards.sortedBy { it.numberInSet }.filter { !it.promo }.forEach {
                            val neededCount = kotlin.math.max(0, cardPossesionTargtNonPremium - it.possessions.count())
                            //val n = if(set.cards.count { that -> it.name == that.name } > 1) " (#${it.numberInSet})" else ""
                            if (neededCount > 0) {
                                out.println("${neededCount} ${it.name} (${set.name})")
                            }
                        }
                    }
                }
            }
        }
    }
}
