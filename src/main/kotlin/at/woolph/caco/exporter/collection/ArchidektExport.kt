package at.woolph.caco.exporter.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossessions
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

// TODO export collection to Archidekt using the field Scryfall ID, quantity, language, condition, date added?

val CardCondition.archidekt: String get() = when(this) {
  CardCondition.UNKNOWN -> throw IllegalArgumentException("Unknown card condition")
  CardCondition.NEAR_MINT -> "NM"
  CardCondition.EXCELLENT -> "LP" // Lightly Played
  CardCondition.GOOD -> "MP" // Moderately Played
  CardCondition.PLAYED -> "HP" // Heavily Played
  CardCondition.POOR -> "D" // Damaged
}

fun archidektTreatment(foil: Boolean) = when {
  // TODO etched is missing
  foil -> "Foil"
  else -> "Normal"
}

val CardLanguage.archidekt: String get() = when(this) {
  CardLanguage.UNKNOWN -> throw IllegalArgumentException("Unknown card language")
  else -> toString()
}

fun exportArchidekt(file: Path) {
  println("importing deckbox collection export file $file")
  transaction {

    CSVWriter(file.bufferedWriter()).use { writer ->
      writer.writeNext(arrayOf("Quantity","Finish","Condition","Language","Scryfall ID","Date Added"))

      CardPossessions.select(CardPossessions.id.count(), CardPossessions.card, CardPossessions.foil, CardPossessions.language, CardPossessions.condition, CardPossessions.dateOfAddition)
        .groupBy(CardPossessions.card, CardPossessions.foil, CardPossessions.language, CardPossessions.condition, CardPossessions.dateOfAddition)
        .forEach { record ->
          writer.writeNext(arrayOf(
            record[CardPossessions.id.count()].toString(),
            archidektTreatment(record[CardPossessions.foil]).toString(),
            record[CardPossessions.condition].archidekt,
            record[CardPossessions.language].archidekt,
            record[CardPossessions.card].value.toString(),
            record[CardPossessions.dateOfAddition].toString(),
            ))
        }
    }
  }
}
