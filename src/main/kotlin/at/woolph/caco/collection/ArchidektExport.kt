package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossessions
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID
import kotlin.io.path.bufferedWriter

data class ArchidektCollectionExportItem(
  val quantity : UInt,
  val foil: Boolean,
  val condition: CardCondition,
  val language: CardLanguage,
  val scryfallId: UUID,
  val dateAdded: LocalDate,
) {
  internal fun toCsvRow(): Array<String> = arrayOf(
    quantity.toString(),
    finish,
    archidektCondition,
    archidektLanguage,
    scryfallId.toString(),
    dateAdded.toString()
  )

  private val finish: String get() = when {
    // TODO etched is missing
    foil -> "Foil"
    else -> "Normal"
  }

  private val archidektCondition: String get() = when(condition) {
    CardCondition.UNKNOWN -> throw IllegalArgumentException("Unknown card condition")
    CardCondition.NEAR_MINT -> "NM"
    CardCondition.EXCELLENT -> "LP" // Lightly Played
    CardCondition.GOOD -> "MP" // Moderately Played
    CardCondition.PLAYED -> "HP" // Heavily Played
    CardCondition.POOR -> "D" // Damaged
  }

  private val archidektLanguage: String get() = when(language) {
    CardLanguage.UNKNOWN -> throw IllegalArgumentException("Unknown card language")
    else -> language.toString()
  }
}

fun Iterable<ArchidektCollectionExportItem>.exportArchidekt(file: Path) {
  println("exporting collection for archidekt to $file")
    CSVWriter(file.bufferedWriter()).use { writer ->
      writer.writeNext(arrayOf("Quantity","Finish","Condition","Language","Scryfall ID","Date Added"))
      filter { it.quantity > 0U }.forEach { item -> writer.writeNext(item.toCsvRow()) }
    }
}

fun exportArchidekt(file: Path) {
  println("exporting collection for archidekt to $file")
  transaction {
    CardPossessions.select(CardPossessions.id.count(), CardPossessions.card, CardPossessions.foil, CardPossessions.language, CardPossessions.condition, CardPossessions.dateOfAddition)
      .groupBy(CardPossessions.card, CardPossessions.foil, CardPossessions.language, CardPossessions.condition, CardPossessions.dateOfAddition)
      .map { record ->
        ArchidektCollectionExportItem(
          record[CardPossessions.id.count()].toUInt(),
          record[CardPossessions.foil],
          record[CardPossessions.condition],
          record[CardPossessions.language],
          record[CardPossessions.card].value,
          record[CardPossessions.dateOfAddition],
        )
      }
      .exportArchidekt(file)
  }
}
