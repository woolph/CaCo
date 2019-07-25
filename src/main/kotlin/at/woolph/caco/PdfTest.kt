package at.woolph.caco

import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.awt.Color

// a4 842 pt x 595 pt
fun main(args: Array<String>) {
	createPdfDocument {
		val fontTitle = Font(PDType1Font.HELVETICA_BOLD, 10f)

		page(PDRectangle.A4) {
			println(box.height)
			frame(PagePosition.RIGHT, 50f, 20f, 20f, 20f) {
				drawBorder(1f, Color.BLACK)

				drawText("doc title", fontTitle, HorizontalAlignment.CENTER, box.upperRightY-10f, Color.BLACK)

				frame(marginTop = fontTitle.height+10f+10f) {
					/*
					// calc metrics
					val font = PDType1Font.HELVETICA
					val minFontSize = 6f
					val items = 267
					val lineSpacing = 3f
					val columnGap = 5f
					//val fontText = Font(PDType1Font.HELVETICA, 10f)

					println(box.height)
					var columnCount = 1
					var linesPerColumn = ceil(items.toFloat()/columnCount.toFloat()).roundToInt()
					var fontSize = floor((box.height/(linesPerColumn) - lineSpacing) * 1000 / font.fontDescriptor.fontBoundingBox.height)
					println("font size $fontSize for $columnCount columns with $linesPerColumn lines per column")
					while(fontSize<minFontSize && columnCount<10) {
						columnCount++
						linesPerColumn = ceil(items.toFloat()/columnCount.toFloat()).roundToInt()
						fontSize = floor((box.height/(linesPerColumn) - lineSpacing) * 1000 / font.fontDescriptor.fontBoundingBox.height)
						println("font size $fontSize for $columnCount columns with $linesPerColumn lines per column")
						println("estimated height = ${(fontSize+lineSpacing)*linesPerColumn}")
					}

					contentStream.columns(box, columnCount, columnGap, lineSpacing, PDType1Font.HELVETICA, fontSize) {
						for(i in 0 until items) {
							writeWithRects(i/linesPerColumn, i%linesPerColumn, "#${i+1} Entry blablalba", ThreadLocalRandom.current().nextInt(6), ThreadLocalRandom.current().nextInt(6))
						}
					}
					*/
				}
			}
		}

		save("testfile2.pdf")
	}
}

