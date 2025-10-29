package at.woolph.utils.pdf

import kotlinx.io.Sink
import org.apache.pdfbox.pdmodel.common.PDRectangle

expect class PDFDocument {
  fun loadFont(resource: String): Font
}

@PdfDsl
expect fun pdfDocument(
  sink: Sink,
  startingPagePosition: PagePosition = PagePosition.RIGHT,
  defaultPageFormat: PDRectangle = PDRectangle.A4,
  block: PDFDocument.() -> Unit,
)
