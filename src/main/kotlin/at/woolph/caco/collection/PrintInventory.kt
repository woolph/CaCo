package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.pdf.Font
import at.woolph.libs.pdf.HorizontalAlignment
import at.woolph.libs.pdf.columns
import at.woolph.libs.pdf.createPdfDocument
import at.woolph.libs.pdf.drawText
import at.woolph.libs.pdf.frame
import at.woolph.libs.pdf.framePagePosition
import at.woolph.libs.prompt
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

class PrintInventory: CliktCommand(name = "inventory") {
    val output by option().path(canBeDir = false).required()
    val sets by argument(help="The set code of the cards to be entered").convert {
      transaction {
        CardSet.Companion.findById(it.lowercase()) ?: throw IllegalArgumentException("No set found for set code $it")
      }
    }.multiple().prompt("Enter the set codes to be imported/updated")

    override fun run() = runBlocking {
      newSuspendedTransaction {
        createPdfDocument(output) {
          val fontTitle = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)
          val fontLine = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA), 6.0f)
          sets.forEach { set ->
            echo("inventory for set ${set.name} into $output")
            page(PDRectangle.A4) {
              framePagePosition(50f, 20f, 20f, 20f) {
                drawText("Inventory ${set.name}", fontTitle, HorizontalAlignment.CENTER, 0f, 10f, Color.BLACK)

                // TODO calc metrics for all sets (so that formatting is the same for all pages)
                set.cards.sortedBy { it.collectorNumber }.filter { !it.promo }.let {
                  frame(marginTop = fontTitle.height + 20f) {
                    columns((it.size - 1) / 100 + 1, 100, 5f, 3.5f, fontLine) {
                      var i = 0
                      it.filter { !it.token }.forEach {
                        echo("${it.name} processing")
                        val ownedCountEN = it.possessions.count { it.language == CardLanguage.ENGLISH }
                        val ownedCountDE = it.possessions.count { it.language == CardLanguage.GERMAN }
                        this@columns.get(i) {
                          drawTextWithRects("${it.rarity} ${it.collectorNumber} ${it.name}", ownedCountEN, ownedCountDE)
                        }
                        i++
                        echo("${it.name} done")
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