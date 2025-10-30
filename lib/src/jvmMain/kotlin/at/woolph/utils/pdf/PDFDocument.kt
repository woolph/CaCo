package at.woolph.utils.pdf

import io.ktor.utils.io.core.writeFully
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

actual class PDFDocument internal constructor(
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

  internal fun save(sink: Sink) {
    sink.asOutputStream().use(document::save)
  }

  actual fun loadFont(resource: String): Font =
    Font(PDType0Font.load(document, javaClass.getResourceAsStream(resource)
      ?: throw IllegalArgumentException("resource $resource not found")))

    fun createFromByteArray(image: ByteArray, imageName: String? = null) =
      createFromSource(Buffer().apply {
        writeFully(image)
      }, imageName)

  fun createFromPath(path: kotlinx.io.files.Path) =
    createFromSource(SystemFileSystem.source(path).buffered(), imageName = path.toString())

  fun createFromSource(source: Source, imageName: String? = null) =
    source.use { PDImageXObject.createFromByteArray(document, it.readByteArray(), imageName)  }

//  fun createFromFile(file: Path) =
//      PDImageXObject.createFromFileByContent(file.toFile(), this.document)
//
}

@PdfDsl
actual fun pdfDocument(
  sink: Sink,
  startingPagePosition: PagePosition,
  defaultPageFormat: PDRectangle,
  block: PDFDocument.() -> Unit,
): Unit = PDFDocument(PDDocument(), defaultPageFormat, startingPagePosition).use {
  it.block()
  it.save(sink)
}
