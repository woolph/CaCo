/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import qrcode.QRCode
import qrcode.color.Colors
import qrcode.color.QRCodeColorFunction
import qrcode.render.QRCodeGraphics
import java.awt.Color
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream
import kotlin.math.*

class DeckBuildingListPrinter2 {
    // TODO exclude from list every CardPossession which is used for a deck
    suspend fun printList(deckLink: String, name: String, file: Path) {
        createPdfDocument {
            val logoBytes = ClassLoader.getSystemResourceAsStream("images/gruul.png")?.readBytes() ?: ByteArray(0)

            val pageFormat = PDRectangle.A4

            val fontColor = Color.BLACK
            val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)
            val fontTitle = Font(fontFamily72Black, 12f)
            val fontCard = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f)
            val fontPrice = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8f)
            val fontCode = Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 6f)

            val POINTS_PER_MM = 2.8346457f

            page(pageFormat) {
                val labelWidth = 70*POINTS_PER_MM
                val labelHeight = 37*POINTS_PER_MM
                frameRelative(HorizontalAlignment.LEFT, 0f, VerticalAlignment.TOP, 0f, labelWidth, labelHeight) {
                    drawBorder(1f, Color.BLACK)
                    drawText("${name}", fontTitle, HorizontalAlignment.CENTER, 0f, fontTitle.height, fontColor)

                    val helloWorld = QRCode.ofRoundedSquares()
                        .withRadius(6)
                        .withSize(16) // Default is 25
                        .withBeforeRenderAction {
//                            it.fillRect(0, 0, it.width, it.height, Colors.WHITE)
                            it.drawImage(logoBytes, (it.width-383)/2, (it.height-640)/2)
                        }
                        .withCustomColorFunction(object: QRCodeColorFunction {
                            override fun bg(row: Int, col: Int, qrCode: QRCode, qrCodeGraphics: QRCodeGraphics): Int {
                                return Colors.TRANSPARENT
                            }

                            override fun fg(row: Int, col: Int, qrCode: QRCode, qrCodeGraphics: QRCodeGraphics): Int {
                                val distance = (sqrt((row*row + col*col).toDouble()) / (sqrt(2.0)*qrCode.rawData.size)).coerceIn(0.0..1.0)

                                val startComponents = Colors.getRGBA(Colors.GREEN)
                                val endComponents = Colors.getRGBA(Colors.RED)

                                val r = startComponents[0] * (1 - distance) + endComponents[0] * distance
                                val g = startComponents[1] * (1 - distance) + endComponents[1] * distance
                                val b = startComponents[2] * (1 - distance) + endComponents[2] * distance

                                return Colors.rgba(
                                    r.roundToInt().coerceIn(0..255),
                                    g.roundToInt().coerceIn(0..255),
                                    b.roundToInt().coerceIn(0..255),
                                    200,
                                )
                            }
                        })
//                        .withLogo(logoBytes, 32, 32, clearLogoArea = true)
                        .build(deckLink)

                    val qrCode = helloWorld.render()


                    val image = PDImageXObject.createFromByteArray(document.document, qrCode.getBytes(), null)

                    drawImage(image, 0f, 0f, 72f, 72f)

                    file.resolveSibling("$name QR.png").createParentDirectories().outputStream().use { it.write(qrCode.getBytes()) }
                }
            }

            save(file)
        }
    }
}

suspend fun main() {
    sequenceOf(
        "https://archidekt.com/decks/8175415/erinis_ponza" to "Erinis Gruul Ponza",
    ).forEach { (deckLink, deckName) ->
        DeckBuildingListPrinter2().printList(
            deckLink,
            deckName,
            Path.of("C:\\Users\\001121673\\private\\magic\\deckbox-label.pdf"),
        )
    }
}
