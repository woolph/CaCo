package at.woolph.caco.cli

import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.imagecache.ImageCache
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.nio.file.Path

class CollectionPagePreview(
    val terminal: Terminal,
) {
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun printLabel(setCode: String, file: Path) = coroutineScope {
        terminal.println("Generating collection page preview for set $setCode")
        val progress = progressBarContextLayout<String> {
            percentage()
            progressBar()
            completed(style = terminal.theme.success)
            timeRemaining(style = TextColors.magenta)
            text { "$context" }
        }.animateInCoroutine(terminal, context = "fetching cards")

        launch { progress.execute() }

        val cardList = transaction {
            CardSet.findById(setCode)?.cards ?: emptyList()
        }.sortedBy { it.numberInSet }

        progress.update { total = cardList.size.toLong() }

        val endsWithLetter = Regex("\\d+")
        val (ordinaryCardList, specialVersionCardList) = cardList.partition { it.numberInSet.matches(endsWithLetter) }
        val collectionPages = ordinaryCardList.chunked(9) + specialVersionCardList.chunked(9)

        createPdfDocument(file, PagePosition.LEFT) {
            val pageFormat = PDRectangle.A4

            val fontColor = Color.BLACK
            val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)
            val fontCode = Font(fontFamily72Black, 10f)

            val mtgCardBack = createFromFile(Path.of("./card-back.jpg"))

            val margin = Position(10.0f, 10.0f)
            val pageGap = 10.0f

            val pageSize = pageFormat.toPosition()
            val columns = 3
            val rows = 3
            val cardCount = Position(columns.toFloat(), rows.toFloat())
            val cardGap = Position(5.0f, 5.0f)
            val cardSize = (pageSize - margin * 2f - cardGap * cardCount + cardGap) / cardCount
            val cardOffset = cardSize + cardGap

            fun position(index: Int): Position {
                val row = when(index) {
                    in 0 ..< 3 -> 0
                    in 3 ..< 6 -> 1
                    in 6 ..< 9 -> 2
                    else -> throw IllegalStateException()
                }

                val column = index - 3 * row
                require(column in 0 ..< 3)

                return margin +
                        cardOffset * Position(column.toFloat(), row.toFloat())
            }

            emptyPage(pageFormat)
            collectionPages.forEachIndexed { pageNumber, pageContent ->
                page(pageFormat) {
                    pageContent.forEachIndexed { index, card ->
                        val cardPosition = position(index)
                        try {
                            val byteArray = ImageCache.getImageByteArray(card.thumbnail.toString()) {
                                try {
//                                        print("card #$${card.numberInSet} ${card.name} image downloading\r")
                                    progress.update {
                                        context = "card #\$${card.numberInSet} ${card.name} image downloading\r"
                                    }
                                    card.thumbnail?.toURL()?.readBytes()
                                } catch (t: Throwable) {
//                                        print("card #\$${card.numberInSet} ${card.name} image is not loaded\r")
                                    null
                                }
                            }!!
                            val cardImage = createFromByteArray(byteArray, card.name)
//                            print("card #\$${card.numberInSet} ${card.name} image rendering\r")
                            progress.update {
                                context = "card #\$${card.numberInSet} ${card.name} image rendering\r"
                            }
                            drawImage(cardImage, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
//                            print("card #\$${card.numberInSet} ${card.name} image rendered\r")
                            progress.update {
                                context = "card #\$${card.numberInSet} ${card.name} image rendered\r"
                                completed += 1
                            }
                        } catch (t: Throwable) {
                            drawImage(mtgCardBack, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
//                            print("card #\$${card.numberInSet} ${card.name} cardback rendered\r")
                            progress.update {
                                context = "card #\$${card.numberInSet} ${card.name} image rendered\r"
                                completed += 1
                            }
                        }
                    }
//                    val minNumberInSet = pageContent.minOf { it.map { it.numberInSet }.orElse("Z") }
//                    val maxNumberInSet = pageContent.maxOf { it.map { it.numberInSet }.orElse("0") }
//                    drawText("#$minNumberInSet - #$maxNumberInSet", fontCode, HorizontalAlignment.CENTER, 0f, box.height, fontColor)

                    when (pagePosition) {
                        PagePosition.RIGHT ->
                            drawText("Page %02d (Front)".format(pageNumber/2+1), fontCode, HorizontalAlignment.RIGHT, 0f, box.height, fontColor)
                        PagePosition.LEFT ->
                            drawText("Page %02d (Back)".format(pageNumber/2+1), fontCode, HorizontalAlignment.LEFT, 0f, box.height, fontColor)
                    }
                }
            }
        }
    }
}
