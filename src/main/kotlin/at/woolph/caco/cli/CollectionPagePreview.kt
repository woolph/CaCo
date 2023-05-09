package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.drawAsImage
import at.woolph.caco.imagecache.ImageCache
import at.woolph.caco.toPDImage
import at.woolph.caco.view.collection.CardModel
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.util.*

class CollectionPagePreview {
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun printLabel(setCode: String, file: String) {
        Databases.init()

        val cardList = transaction {
            CardSet.findById(setCode)?.cards ?: emptyList()
        }.sortedBy { it.numberInSet }.asSequence().map{ Optional.of(it) }

        val collectionPages = sequenceOf(
            generateSequence { Optional.empty<Card>() }.take(9),
            cardList,
        ).flatten().chunked(18)

        createPdfDocument {
            val pageFormat = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)

            val mtgCardBack = PDImageXObject.createFromFile("./card-back.jpg", this)

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

            val pageSize = (pageFormat.toPosition() - (margin * 2.0f) - Position(pageGap, 0f)) / Position(2.0f, 1.0f)
            val rows = 3
            val columns = 3
            val cardGap = Position(5.0f, 5.0f)
            val cardSize = Position((pageSize.x - cardGap.x * (columns - 1))/columns, (pageSize.y - cardGap.y * (rows - 1))/rows)

            val pageOffset = Position(pageSize.x + pageGap, 0f)
            val cardOffset = Position(cardSize.x + cardGap.x, cardSize.y + cardGap.y)


            fun position(index: Int): Position {
                val page = when(index) {
                    in 0 ..< 9 -> 0
                    in 9 ..< 18 -> 1
                    else -> throw IllegalArgumentException("index $index is not between 0 ..< 18")
                }

                val row = when(index - 9 * page) {
                    in 0 ..< 3 -> 0
                    in 3 ..< 6 -> 1
                    in 6 ..< 9 -> 2
                    else -> throw IllegalStateException()
                }

                val column = index - 9 * page - 3 * row
                require(column in 0 ..< 3)

                return margin +
                        pageOffset * page.toFloat() +
                        cardOffset * Position(column.toFloat(), rows - row.toFloat() - 1)
            }
            collectionPages.forEach { pageContent ->
                page(pageFormat) {
                    pageContent.forEachIndexed { index, cardOptional ->
                        cardOptional.ifPresent { card ->
                            val cardPosition = position(index)
                            try {
                                println("card #$${card.numberInSet} ${card.name} image is being downloaded")
                                val byteArray = runBlocking {
                                    ImageCache.getImageByteArray(card.image.toString()) {
                                        try {
                                            card.image?.toURL()?.readBytes()
                                        } catch (t: Throwable) {
                                            println("unable to load")
                                            null
                                        }
                                    }
                                }!!
                                val cardImage = PDImageXObject.createFromByteArray(this@createPdfDocument, byteArray, card.name)
                                drawImage(cardImage, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
                                println("card #\$${card.numberInSet} ${card.name} image is rendered")
                            } catch (t: Throwable) {
                                drawImage(mtgCardBack, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
                                println("card #\$${card.numberInSet} ${card.name} cardback rendered")
                            }
                        }
                    }
                }
            }

            save(file)
        }
    }
}

suspend fun main() {
    CollectionPagePreview().printLabel("mom", "C:\\Users\\001121673\\mom.pdf")
    CollectionPagePreview().printLabel("one", "C:\\Users\\001121673\\one.pdf")
}
