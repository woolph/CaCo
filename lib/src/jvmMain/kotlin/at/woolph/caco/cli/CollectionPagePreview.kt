package at.woolph.caco.cli

import arrow.core.Either
import arrow.core.getOrElse
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.image.ImageCache
import at.woolph.utils.ProgressTracker
import at.woolph.utils.io.asSink
import at.woolph.utils.io.createParentDirectories
import at.woolph.utils.pdf.HorizontalAlignment
import at.woolph.utils.pdf.PagePosition
import at.woolph.utils.pdf.Position
import at.woolph.utils.pdf.drawImage
import at.woolph.utils.pdf.drawText
import at.woolph.utils.pdf.loadFont72Black
import at.woolph.utils.pdf.pdfDocument
import at.woolph.utils.pdf.toPosition
import kotlinx.coroutines.coroutineScope
import kotlinx.io.files.Path
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

class CollectionPagePreview(
  val progressTracker: ProgressTracker<String, Int>,
) {
  suspend fun printLabel(setCode: String, file: Path) = coroutineScope {
    val cardList =
      transaction { (ScryfallCardSet.findByCode(setCode)?.cards ?: emptyList())
        .sortedBy { it.collectorNumber }
      }

    progressTracker.setTotal(cardList.size)

    val cardListWithImages = cardList.map { card ->
      card to ImageCache.getImageByteArray(card.thumbnail.toString()) {
        Either.catch {
          progressTracker.updateContext("card #\$${card.collectorNumber} ${card.name} image downloading\r")
          card.thumbnail?.toURL()?.readBytes() ?: throw Exception("thumbnail url missing")
        }
      }.getOrElse { throw it }
    }

    val endsWithLetter = Regex("\\d+")
    val (ordinaryCardList, specialVersionCardList) =
      cardListWithImages.partition { it.first.collectorNumber.matches(endsWithLetter) }
    val collectionPages = ordinaryCardList.chunked(9) + specialVersionCardList.chunked(9)

    pdfDocument(file.createParentDirectories().asSink(), startingPagePosition = PagePosition.LEFT) {
      val pageFormat = PDRectangle.A4

      val fontColor = Color.BLACK
      val sizedFontCode = loadFont72Black().withSize(10f)

      val mtgCardBack = createFromPath(kotlinx.io.files.Path("./assets/images/card-back.jpg"))

      val margin = Position(10.0f, 10.0f)

      val pageSize = pageFormat.toPosition()
      val columns = 3
      val rows = 3
      val cardCount = Position(columns.toFloat(), rows.toFloat())
      val cardGap = Position(5.0f, 5.0f)
      val cardSize = (pageSize - margin * 2f - cardGap * cardCount + cardGap) / cardCount
      val cardOffset = cardSize + cardGap

      fun position(index: Int): Position {
        val row =
          when (index) {
            in 0..<3 -> 0
            in 3..<6 -> 1
            in 6..<9 -> 2
            else -> throw IllegalStateException()
          }

        val column = index - 3 * row
        require(column in 0..<3)

        return margin + cardOffset * Position(column.toFloat(), row.toFloat())
      }

      emptyPage(pageFormat)
      collectionPages.forEachIndexed { pageNumber, pageContent ->
        page(pageFormat) {
          pageContent.forEachIndexed { index, it ->
            val cardPosition = position(index)
            val card = it.first
            val byteArray = it.second
            try {
              progressTracker.updateContext("card #\$${card.collectorNumber} ${card.name} image rendering\r")
              val cardImage = createFromByteArray(byteArray, card.name)
              drawImage(cardImage, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
              progressTracker.advance(1)
            } catch (_: Throwable) {
              drawImage(mtgCardBack, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
            }
          }
          when (pagePosition) {
            PagePosition.RIGHT ->
              drawText(
                "Page %02d (Front)".format(pageNumber / 2 + 1),
                sizedFontCode,
                HorizontalAlignment.RIGHT,
                0f,
                box.height,
                fontColor,
              )

            PagePosition.LEFT ->
              drawText(
                "Page %02d (Back)".format(pageNumber / 2 + 1),
                sizedFontCode,
                HorizontalAlignment.LEFT,
                0f,
                box.height,
                fontColor,
              )
          }
        }
      }
    }
  }
}