package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardVersion
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import com.opencsv.CSVWriter
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

fun Iterable<CardCollectionItem>.exportDeckbox(file: Path) {
  println("exporting collection for deckbox to $file")
  CSVWriter(file.bufferedWriter()).use { writer ->
    writer.writeNext(arrayOf("Count","Tradelist Count","Name","Edition","Edition Code","Card Number","Condition","Language","Foil","Signed","Artist Proof","Altered Art","Misprint","Promo","Textless","My Price", "Last Updated"), false)
    filter(CardCollectionItem::isNotEmpty).forEach { item -> writer.writeNext(item.toDeckboxCsvRow(), false) }
  }
}

internal fun CardCollectionItem.toDeckboxCsvRow(): Array<String> = arrayOf(
  quantity.toString(),
  "0", // tradelist count
  cardCollectionItemId.card.name, // TODO reverse mapping of Emblems, Surgeon General Commander, etc.
  (if (cardCollectionItemId.card.set.code == "plst")
    cardCollectionItemId.card.collectorNumber.split("-", limit = 2).first().lowercase().let { setCode ->
      ScryfallCardSet.find { ScryfallCardSets.code eq setCode }.singleOrNull()?.name ?: "The List"
    } else cardCollectionItemId.card.set.name).let { setName ->
    (setNameMapping.asSequence().firstOrNull { it.value == setName }?.key ?: setName).let { mappedSetName ->
      when {
        cardCollectionItemId.cardVersion == CardVersion.PrereleaseStamped -> "Prerelease Events: ${mappedSetName}"
        cardCollectionItemId.card.token -> "Extras: ${mappedSetName}"
        else -> mappedSetName
      }
    }
  },
  cardCollectionItemId.card.set.code.uppercase(),
  if (cardCollectionItemId.card.set.code == "plst") {
    cardCollectionItemId.card.collectorNumber.split("-", limit = 2).last()
  } else {
    cardCollectionItemId.card.collectorNumber
  },
  cardCollectionItemId.condition.toDeckboxCondition(),
  cardCollectionItemId.language.toLanguageDeckbox(),
  if (cardCollectionItemId.foil) "foil" else "",
  "",
  if (cardCollectionItemId.card.set.code == "plst" || cardCollectionItemId.cardVersion == CardVersion.TheList) "proof" else "",
  "",
  "",
  if (cardCollectionItemId.cardVersion == CardVersion.PromopackStamped) "promo" else "",
  "",
  "",
  DATE_FORMAT_DECKBOX.format(dateAdded),
)

internal fun CardLanguage.toLanguageDeckbox(): String = when (this) {
  CardLanguage.ENGLISH -> "English"
  CardLanguage.GERMAN -> "German"
  CardLanguage.JAPANESE -> "Japanese"
  CardLanguage.RUSSIAN -> "Russian"
  CardLanguage.SPANISH -> "Spanish"
  CardLanguage.KOREAN -> "Korean"
  CardLanguage.ITALIAN -> "Italian"
  CardLanguage.PORTUGUESE -> "Portuguese"
  CardLanguage.FRENCH -> "French"
  CardLanguage.CHINESE -> "Chinese"
  CardLanguage.CHINESE_TRADITIONAL -> "Traditional Chinese"
  else -> throw Exception("unknown language")
}

internal fun CardCondition.toDeckboxCondition(): String = when (this) {
  CardCondition.NEAR_MINT -> "Near Mint"
  CardCondition.EXCELLENT -> "Good (Lightly Played)"
  CardCondition.GOOD -> "Played"
  CardCondition.PLAYED -> "Heavily Played"
  CardCondition.POOR -> "Poor"
  else -> throw Exception("unknown condition")
}
