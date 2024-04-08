package at.woolph

import at.woolph.libs.pdf.createPdfDocument
import at.woolph.libs.pdf.drawBorder
import at.woolph.libs.pdf.frameRelative
import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.awt.Color
import java.nio.file.Path


fun main() {
    createPdfDocument {
        page(PDRectangle.A4) {
            frameRelative(HorizontalAlignment.CENTER, 0f, VerticalAlignment.MIDDLE, 0f, 100f, 100f) {
                drawBorder(2f, Color.CYAN)
            }
            frameRelative(HorizontalAlignment.CENTER, 50f, VerticalAlignment.MIDDLE, 50f, 100f, 100f) {
                drawBorder(2f, Color.BLACK)
            }
            frameRelative(HorizontalAlignment.CENTER, 0f, VerticalAlignment.BOTTOM, -50f, 100f, 100f) {
                drawBorder(2f, Color.BLUE)
            }
            frameRelative(HorizontalAlignment.RIGHT, -50f, VerticalAlignment.TOP, 50f, 100f, 100f) {
                drawBorder(2f, Color.GREEN)
            }
            frameRelative(HorizontalAlignment.LEFT, -50f, VerticalAlignment.TOP, -50f, 100f, 100f) {
                drawBorder(2f, Color.RED)
            }
        }

        save(Path.of("C:\\Users\\001121673\\test.pdf"))
    }
}
