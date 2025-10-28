package at.woolph.utils.pdf

import arrow.core.raise.either
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

class PDFDocument(
  val document: PDDocument,
  startingPagePosition: PagePosition,
) : AutoCloseable by document {
  var currentPagePosition = startingPagePosition

  inline fun page(format: PDRectangle, block: Page.() -> Unit): Page {
    val page = Page(this, currentPagePosition, PDPage(format)).use { it.apply(block) }
    alternateCurrentPagePosition()
    document.addPage(page.pdPage)
    return page
  }

  fun emptyPage(format: PDRectangle) = page(format) {}

  fun alternateCurrentPagePosition() {
    currentPagePosition =
        when (currentPagePosition) {
          PagePosition.LEFT -> PagePosition.RIGHT
          PagePosition.RIGHT -> PagePosition.LEFT
        }
  }

  fun save(file: Path) {
    document.save(file.createParentDirectories().toFile())
  }

  fun loadType0Font(resource: String) = either {
    loadType0Font(
      javaClass.getResourceAsStream(resource)
        ?: raise(IllegalArgumentException("resource $resource not found"))
    )
  }

  fun loadType0Font(inputStream: InputStream) = PDType0Font.load(document, inputStream)

  fun createFromFile(file: Path) =
      PDImageXObject.createFromFileByContent(file.toFile(), this.document)

  fun createFromByteArray(image: ByteArray, imageName: String? = null) =
      PDImageXObject.createFromByteArray(document, image, imageName)
}

inline fun pdfDocument(
  file: Path,
  startingPagePosition: PagePosition = PagePosition.RIGHT,
  block: PDFDocument.() -> Unit,
) = PDFDocument(PDDocument(), startingPagePosition).use { it.apply(block).save(file) }

inline fun pdfDocument(
  startingPagePosition: PagePosition = PagePosition.RIGHT,
  block: PDFDocument.() -> Unit,
) = PDFDocument(PDDocument(), startingPagePosition).use { it.apply(block) }