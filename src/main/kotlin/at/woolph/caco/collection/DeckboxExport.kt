package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardVariant
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import java.nio.file.Path
import java.time.ZoneOffset
import kotlin.math.max

fun Iterable<CardCollectionItem>.exportDeckbox(file: Path) = export(
  file,
  mapOf(
    "Count" to { quantity.toString() },
    "Tradelist Count" to {
      max(
        0,
        quantity.toInt() - (cardCollectionItemId.card.specialDeckRestrictions ?: 1)
      ).toString()
  }, // tradelist count (todo recognize other printings in other languages too)
    "Name" to { cardCollectionItemId.card.name },// TODO reverse mapping of Emblems, Surgeon General Commander, etc.
    "Edition" to {
      (if (cardCollectionItemId.card.set.code == "plst")
        cardCollectionItemId.card.collectorNumber.split("-", limit = 2).first().lowercase().let { setCode ->
          ScryfallCardSet.find { ScryfallCardSets.code eq setCode }.singleOrNull()?.name ?: "The List"
        } else cardCollectionItemId.card.set.name).let { setName ->
        (setNameMapping.asSequence().firstOrNull { it.value == setName }?.key ?: setName).let { mappedSetName ->
          when {
            cardCollectionItemId.variantType == CardVariant.Type.PrereleaseStamped -> "Prerelease Events: ${mappedSetName}"
            cardCollectionItemId.card.token -> "Extras: ${mappedSetName}"
            else -> mappedSetName
          }
        }
      }
    },
    "Edition Code" to { cardCollectionItemId.card.set.code.uppercase() }, // FIXME the list cards are exported the wrong way
    "Card Number" to { if (cardCollectionItemId.card.set.code == "plst") {
      cardCollectionItemId.card.collectorNumber.split("-", limit = 2).last()
    } else {
      cardCollectionItemId.card.collectorNumber
    } },
    "Condition" to { cardCollectionItemId.condition.toDeckboxCondition() },
    "Language"  to { cardCollectionItemId.language.toLanguageDeckbox() },
    "Foil" to { if (cardCollectionItemId.foil) "foil" else "" },
    "Signed" to { "" },
    "Artist Proof" to { if (cardCollectionItemId.variantType == CardVariant.Type.TheList) "proof" else "" },
    "Altered Art" to { "" },
    "Misprint" to { "" },
    "Promo" to { if (cardCollectionItemId.variantType == CardVariant.Type.PromopackStamped) "promo" else "" },
    "Textless" to { "" },
    "My Price" to { purchasePrice?.let { "$$it" } ?: "" },
    "Last Updated" to { DATE_FORMAT_DECKBOX.format(dateAdded.atOffset(ZoneOffset.UTC)) },
  ),
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
