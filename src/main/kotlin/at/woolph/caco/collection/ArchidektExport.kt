package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import com.opencsv.CSVWriter
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.bufferedWriter

fun Iterable<CardCollectionItem>.exportArchidekt(file: Path) {
  println("exporting collection for archidekt to $file")
    CSVWriter(file.bufferedWriter()).use { writer ->
      writer.writeNext(arrayOf("Quantity","Finish","Condition","Language","Scryfall ID","Card name","Set","Date added"))
      filter(CardCollectionItem::isNotEmpty).forEach { item -> writer.writeNext(item.toArchidektCsvRow()) }
    }
}

internal fun CardCollectionItem.toArchidektCsvRow(): Array<String> = arrayOf(
  quantity.toString(),
  when {
    // TODO etched is missing
    cardCollectionItemId.foil -> "Foil"
    else -> "Normal"
  },
  when(cardCollectionItemId.condition) {
    CardCondition.UNKNOWN -> throw IllegalArgumentException("Unknown card condition")
    CardCondition.NEAR_MINT -> "NM"
    CardCondition.EXCELLENT -> "LP" // Lightly Played
    CardCondition.GOOD -> "MP" // Moderately Played
    CardCondition.PLAYED -> "HP" // Heavily Played
    CardCondition.POOR -> "D" // Damaged
  },
  when(cardCollectionItemId.language) {
    CardLanguage.UNKNOWN -> throw IllegalArgumentException("Unknown card language")
    else -> cardCollectionItemId.language.toString()
  },
  cardCollectionItemId.card.scryfallId.toString(),
  cardCollectionItemId.card.name,
  cardCollectionItemId.card.set.code,
  LocalDate.from(dateAdded).toString(),
)