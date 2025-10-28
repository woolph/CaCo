package at.woolph.utils.pdf

import arrow.core.raise.either
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

class PDFDocument internal constructor(
  val document: PDDocument,
  val defaultPageFormat: PDRectangle,
  startingPagePosition: PagePosition,
) : AutoCloseable by document {
  var currentPagePosition = startingPagePosition
    private set

  @PdfDsl
  fun page(format: PDRectangle = defaultPageFormat, block: Page.() -> Unit): Page =
    Page(this, getAndAlternateCurrentPagePosition(), PDPage(format))
      .use { it.apply(block) }
      .also { document.addPage(it.pdPage) }

  @PdfDsl
  suspend fun suspendingPage(format: PDRectangle = defaultPageFormat, block: suspend Page.() -> Unit): Page =
    Page(this, getAndAlternateCurrentPagePosition(), PDPage(format))
      .use { it.apply {
          block()
        }
      }
      .also { document.addPage(it.pdPage) }

  @PdfDsl
  fun emptyPage(format: PDRectangle = defaultPageFormat) =
    Page(this, getAndAlternateCurrentPagePosition(), PDPage(format))
      .also { document.addPage(it.pdPage) }

  internal fun getAndAlternateCurrentPagePosition(): PagePosition {
    val oldCurrentPage = currentPagePosition
    currentPagePosition =
        when (currentPagePosition) {
          PagePosition.LEFT -> PagePosition.RIGHT
          PagePosition.RIGHT -> PagePosition.LEFT
        }
    return oldCurrentPage
  }

  internal fun save(outputStream: OutputStream) {
    document.save(outputStream)
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

@PdfDsl
fun pdfDocument(
  outputStream: OutputStream,
  startingPagePosition: PagePosition = PagePosition.RIGHT,
  defaultPageFormat: PDRectangle = PDRectangle.A4,
  block: PDFDocument.() -> Unit,
): Unit = PDFDocument(PDDocument(), defaultPageFormat, startingPagePosition).use {
  it.block()
  outputStream.use(it::save)
}

@PdfDsl
suspend fun suspendingPdfDocument(
  outputStream: OutputStream,
  startingPagePosition: PagePosition = PagePosition.RIGHT,
  defaultPageFormat: PDRectangle = PDRectangle.A4,
  block: suspend PDFDocument.() -> Unit,
): Unit = PDFDocument(PDDocument(), defaultPageFormat, startingPagePosition).use {
  it.block()
  outputStream.use(it::save)
}
