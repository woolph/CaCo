package at.woolph.utils.pdf

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream

class Page(document: PDFDocument, val pagePosition: PagePosition, val pdPage: PDPage) :
    Node(document, PDPageContentStream(document.document, pdPage), pdPage.mediaBox), AutoCloseable {
  override fun close() {
    contentStream.close()
  }
}
