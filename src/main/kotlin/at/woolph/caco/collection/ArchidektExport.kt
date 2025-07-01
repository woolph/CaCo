package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset

fun Iterable<CardCollectionItem>.exportArchidekt(file: Path) = export(
  file,
  mapOf(
    "Quantity" to { quantity.toString() },
    "Finish" to { when {
      // TODO etched is missing
      cardCollectionItemId.foil -> "Foil"
      else -> "Normal"
    }},
    "Condition" to { when (cardCollectionItemId.condition) {
      CardCondition.UNKNOWN -> throw IllegalArgumentException("Unknown card condition")
      CardCondition.NEAR_MINT -> "NM"
      CardCondition.EXCELLENT -> "LP" // Lightly Played
      CardCondition.GOOD -> "MP" // Moderately Played
      CardCondition.PLAYED -> "HP" // Heavily Played
      CardCondition.POOR -> "D" // Damaged
    }},
    "Language" to { when (cardCollectionItemId.language) {
      CardLanguage.UNKNOWN -> throw IllegalArgumentException("Unknown card language")
      else -> cardCollectionItemId.language.toString()
    }},
    "Scryfall ID" to { cardCollectionItemId.actualScryfallId.toString() },
    "Card name" to { cardCollectionItemId.card.name }, // is optional but better for human interpretation
    "Set" to { cardCollectionItemId.card.set.code }, // is optional but better for human interpretation
    "Date added" to { LocalDate.from(dateAdded.atOffset(ZoneOffset.UTC)).toString() },
    "Purchase Price" to { purchasePrice?.toString() ?: "" },
  ),
)
