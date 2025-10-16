/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.libs.pdf.*
import at.woolph.libs.prompt
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import kotlin.collections.plusAssign

class PrintExcessPossessions : SuspendingTransactionCliktCommand(name = "excess") {
  val output by option().path(canBeDir = false).required()
  val sets by argument(help = "The set code of the cards to be entered").convert {
    transaction {
      ScryfallCardSet.findByCode(it.lowercase()) ?: throw IllegalArgumentException("No set found for set code $it")
    }
  }.multiple().prompt("Enter the set codes to be printed")

  override suspend fun runTransaction() = coroutineScope {
    createPdfDocument(output) {
      val fontTitle = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)
      val fontLine = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA), 6.0f)
      val progressBar = progressBarContextLayout<ScryfallCardSet?>{
        percentage()
        progressBar()
        completed()
        timeRemaining()
        text { "printing ${this.context?.name}" }
      }.animateInCoroutine(terminal, context = null, total = sets.size.toLong(), completed = 0)

      launch { progressBar.execute() }

      sets.forEach { set ->
        progressBar.update {
          context = set
        }
        val maxRows = 85
        val maxColumns = 3

        val cards = set.cards.sorted().filter { !it.promo }
        cards.chunked(maxColumns*maxRows).withIndex().forEach { (pageIndex, items) ->
          page(PDRectangle.A4) {
            framePagePosition(20f, 20f, 20f, 20f) {
              drawText("Inventory ${set.name} Page ${pageIndex + 1}", fontTitle, HorizontalAlignment.CENTER, 0f, 10f, Color.BLACK)

            // TODO calc metrics for all sets (so that formatting is the same for all pages)
              frame(marginTop = fontTitle.height + 20f) {
                columns(maxColumns, maxRows, 5f, 3.5f, fontLine) {
                  var i = 0
                  items.forEach {
                    val wanted = 4
                    var totalSum = 0
                    val wantedFoil = 1
                    var totalSumFoil = 0

                    val excesses = CardLanguage.entries.map { language ->
                      language to it.possessions.count { it.language == language && it.finish == Finish.Normal }
                    }.map { (language, possessions) ->
                      val excess = (possessions - (wanted - totalSum)).coerceAtLeast(0)
                      totalSum = (totalSum + possessions).coerceAtMost(wanted)
                      language to excess
                    }.filter {
                      it.second > 0
                    }
                    val excessesFoil = CardLanguage.entries.map { language ->
                      language to it.possessions.count { it.language == language && it.finish != Finish.Normal }
                    }.map { (language, possessions) ->
                      val excess = (possessions - (wantedFoil - totalSumFoil)).coerceAtLeast(0)
                      totalSumFoil = (totalSumFoil + possessions).coerceAtMost(wantedFoil)
                      language to excess
                    }.filter {
                      it.second > 0
                    }

                    this@columns.get(i) {
                      val color = when {
                        excesses.isEmpty() -> Color.LIGHT_GRAY
                        (it.price ?: 0.0) > 1.0 -> Color.CYAN
                        (it.price ?: 0.0) > 0.2 -> Color.MAGENTA
                        else -> Color.BLACK
                      }
                      drawText("${it.collectorNumber.replace('\u2605','*')} ${it.name} ${it.price ?: 0.0} " +
                        "${excesses.joinToString { "${it.second}x ${it.first}" }}  Foil ${excessesFoil.joinToString { "${it.second}x ${it.first}" }} ", color)
                    }
                    i++
                  }
                }
              }
            }
          }
        }
        progressBar.update {
          completed plusAssign 1
        }
      }
    }
  }
}