package at.woolph.caco.cli

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.imagecache.ImageCache
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.nio.file.Path
import java.util.*

class CollectionPagePreview {
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun printLabel(setCode: String, file: Path) {
        Databases.init()

        val cardList = transaction {
            CardSet.findById(setCode)?.cards ?: emptyList()
        }.sortedBy { it.numberInSet }.asSequence().map{ Optional.of(it) }

        val collectionPages = cardList.chunked(9)

        createPdfDocument(file, PagePosition.LEFT) {
            val pageFormat = PDRectangle.A4

            val fontColor = Color.BLACK
            val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)
            val fontCode = Font(fontFamily72Black, 10f)

            val mtgCardBack = createFromFile(Path.of("./card-back.jpg"))

            data class Position(
                val x: Float,
                val y: Float,
            ) {
                operator fun plus(p: Position) = Position(x + p.x, y + p.y)
                operator fun times(d: Float) = Position(x * d, y * d)
                operator fun times(p: Position) = Position(x * p.x, y * p.y)
                operator fun div(d: Float) = Position(x / d, y / d)
                operator fun div(p: Position) = Position(x / p.x, y / p.y)
                operator fun minus(p: Position) = Position(x - p.x, y -p.y)
                operator fun unaryPlus() = this
                operator fun unaryMinus() = Position(-x, -y)
            }
            fun PDRectangle.toPosition() = Position(width, height)

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
                    pageContent.forEachIndexed { index, cardOptional ->
                        cardOptional.ifPresent { card ->
                            val cardPosition = position(index)
                            try {
                                val byteArray = runBlocking {
                                    ImageCache.getImageByteArray(card.thumbnail.toString()) {
                                        try {
                                            print("card #$${card.numberInSet} ${card.name} image downloading\r")
                                            card.thumbnail?.toURL()?.readBytes()
                                        } catch (t: Throwable) {
                                            print("card #\$${card.numberInSet} ${card.name} image is not loaded\r")
                                            null
                                        }
                                    }
                                }!!
                                val cardImage = createFromByteArray(byteArray, card.name)
                                print("card #\$${card.numberInSet} ${card.name} image rendering\r")
                                drawImage(cardImage, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
                                print("card #\$${card.numberInSet} ${card.name} image rendered\r")
                            } catch (t: Throwable) {
                                drawImage(mtgCardBack, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
                                print("card #\$${card.numberInSet} ${card.name} cardback rendered\r")
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

suspend fun main() {
    CollectionPagePreview().printLabel("4ed", Path.of("C:\\Users\\001121673\\private\\magic\\4ed.pdf"))
    CollectionPagePreview().printLabel("5ed", Path.of("C:\\Users\\001121673\\private\\magic\\5ed.pdf"))
}
