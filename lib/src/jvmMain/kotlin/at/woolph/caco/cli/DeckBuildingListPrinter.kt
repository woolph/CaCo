/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.utils.io.asSink
import at.woolph.utils.pdf.SizedFont
import at.woolph.utils.pdf.HorizontalAlignment
import at.woolph.utils.pdf.PDFDocument
import at.woolph.utils.pdf.drawText
import at.woolph.utils.pdf.frame
import at.woolph.utils.pdf.loadFont72Black
import at.woolph.utils.pdf.loadHelveticaOblique
import at.woolph.utils.pdf.loadHelveticaRegular
import at.woolph.utils.pdf.pdfDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

class DeckBuildingListPrinter {
  // TODO exclude from list every CardPossession which is used for a deck
  fun printList(decks: Collection<DeckList>, file: Path) {
    Databases.init()

    pdfDocument(file.createParentDirectories().asSink()) {
      decks.forEach { printListToDocument(it) }
    }
  }

  fun PDFDocument.printListToDocument(deck: DeckList) {
    val pageFormat = PDRectangle.A4

    val fontColor = Color.BLACK
    val fontFamily72Black = loadFont72Black()
    val sizedFontTitle = fontFamily72Black.withSize(12f)
    val sizedFontCard = loadHelveticaRegular().withSize(8f)
    val sizedFontPrice = loadHelveticaOblique().withSize(8f)
    val sizedFontCode = sizedFontPrice.withSize(6f)

    val basics =
      listOf(
        "Plains",
        "Island",
        "Swamp",
        "Mountain",
        "Forest",
        "Snow-Covered Plains",
        "Snow-Covered Island",
        "Snow-Covered Swamp",
        "Snow-Covered Mountain",
        "Snow-Covered Forest",
      )
    val blackListForSetSearch =
      basics +
        listOf(
          "Command Tower",
          "Exotic Orchard",
          "Evolving Wilds",
          "Terramorphic Expanse",
          "Myriad Landscape",
          "Arcane Signet",
          "Sol Ring",
          "Commander's Sphere",
          "Mind Stone",
          "Fellwar Stone",
        )

    data class PossessionInfo(
      val setCodeNumber: String,
      val surplus: Int,
    ) {
      override fun toString(): String = if (surplus > 0) "$setCodeNumber+" else setCodeNumber
    }

    data class DeckListEntry(
      val amount: Int,
      val cardName: String,
      val cardSets: List<PossessionInfo>,
      val cardPrice: Double?,
    )

    page(pageFormat) {
      frame(12f, 12f, 12f, 12f) {

        fun Iterable<DeckListEntry>.totalPrice() = "\$%.2f".format(sumOf { it.cardPrice ?: 0.0 })
        fun Map<String, Int>.fetchPossessionInformation() = transaction {
          entries
            .sortedBy(Map.Entry<String, Int>::key)
            .filter { it.value > 0 }
            .map { (cardName, amount) ->
              val cardPrice =
                Cards.select(Cards.price)
                  .where { (Cards.name match cardName) }
                  .mapNotNull { it[Cards.price]?.toDouble() }
                  .minOrNull()
              val cardSets =
                CardPossessions.innerJoin(Cards)
                  .innerJoin(ScryfallCardSets)
                  .select(
                    ScryfallCardSets.code,
                    Cards.collectorNumber,
                    CardPossessions.finish,
                  )
                  .where { (Cards.name match cardName) }
                  .mapNotNull {
                    "${it[ScryfallCardSets.code].uppercase()} #${it[Cards.collectorNumber].uppercase()}" to
                      it[CardPossessions.finish]
                  }
                  .groupingBy { it.first }
                  .aggregate { setCode, acc: Map<Finish, Int>?, element, first ->
                    if (first) mapOf(element.second to 1)
                    else
                      (acc ?: mapOf()) +
                        (element.second to ((acc?.get(element.second) ?: 0) + 1))
                  }
                  .entries
                  .map { (setCodeNumber, amountPerFinish) ->
                    PossessionInfo(
                      setCodeNumber,
                      amountPerFinish.entries.sumOf { (_, amount) -> amount - 1 },
                    )
                  }
              DeckListEntry(amount, cardName, cardSets, cardPrice)
            }
        }

        val commandZone = deck.commandZone.fetchPossessionInformation()
        val mainboard =
          deck.mainboard.fetchPossessionInformation().let {
            // sorts the basics to the bottom of the list sorted in WUBRG order
            val (part1, part2) = it.partition { (_, cardName, _, _) -> cardName !in basics }
            part1 + part2.sortedBy { (_, cardName, _, _) -> basics.indexOf(cardName) }
          }
        val entries = commandZone + mainboard
        val maxEntriesPerColumn = 50
        val columns = entries.chunked(maxEntriesPerColumn)
        val columnWidth = box.width / columns.size

        drawText(
          "${deck.name} (${entries.totalPrice()})",
          sizedFontTitle,
          HorizontalAlignment.CENTER,
          0f,
          sizedFontTitle.height,
          fontColor,
        )
        columns.forEachIndexed { columnIndex, columnEntries ->
          val columnOffsetX = columnIndex * columnWidth
          var line = 0
          frame(PDRectangle(box.lowerLeftX + columnOffsetX, 0f, columnWidth, box.height)) {
            columnEntries.take(50).forEach { (amount, cardName, cardSets, cardPrice) ->
              val cardSetsString =
                if (cardName in blackListForSetSearch) "[*]"
                else
                  "[${
                    cardSets.sortedByDescending(PossessionInfo::surplus)
                      .joinToString()
                  }]"

              drawText(
                "$amount $cardName",
                sizedFontCard,
                HorizontalAlignment.LEFT,
                0f,
                sizedFontCard.totalHeight +
                  line * (3.0f + sizedFontCard.totalHeight + sizedFontCode.totalHeight),
                fontColor,
              )

              drawText(
                "(${cardPrice?.let { "\$%.2f".format(it) } ?: "$?,??"})",
                sizedFontPrice,
                HorizontalAlignment.RIGHT,
                -15f,
                sizedFontCard.totalHeight +
                  line * (3.0f + sizedFontCard.totalHeight + sizedFontCode.totalHeight),
                fontColor,
              )

              sizedFontCode.adjustedTextToFitWidth(
                cardSetsString,
                columnWidth - 15f,
              ) { title, font ->
                drawText(
                  title,
                  font,
                  HorizontalAlignment.LEFT,
                  5f,
                  sizedFontCard.totalHeight +
                    sizedFontCode.totalHeight +
                    line * (3.0f + sizedFontCard.totalHeight + sizedFontCode.totalHeight),
                  fontColor,
                )
              }
              line++
            }
          }
        }
      }
    }
  }
}
