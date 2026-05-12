/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardPrintVariant
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.time.toJavaInstant
import kotlinx.io.files.Path
import org.jetbrains.exposed.v1.core.eq

fun Iterable<CardCollectionItem>.exportDeckbox(file: Path) =
    export(
        file,
        mapOf(
            "Count" to { quantity.toString() },
            "Tradelist Count" to
                {
                  max(
                          0,
                          quantity.toInt() -
                              (cardCollectionItemId.cardPrint.card.specialDeckRestrictions ?: 1),
                      )
                      .toString()
                }, // tradelist count (todo recognize other printings in other languages too)
            "Name" to
                {
                  cardCollectionItemId.cardPrint.name
                }, // TODO reverse mapping of Emblems, Surgeon General Commander, etc.
            "Edition" to
                {
                  (if (cardCollectionItemId.cardPrint.set.code == "plst") {
                        cardCollectionItemId.cardPrint.collectorNumber
                            .split("-", limit = 2)
                            .first()
                            .lowercase()
                            .let { setCode ->
                              ScryfallCardSet.find { ScryfallCardSets.code eq setCode }
                                  .singleOrNull()
                                  ?.name ?: "The List"
                            }
                      } else {
                        cardCollectionItemId.cardPrint.set.name
                      })
                      .let { setName ->
                        (setNameMapping.asSequence().firstOrNull { it.value == setName }?.key
                                ?: setName)
                            .let { mappedSetName ->
                              when {
                                cardCollectionItemId.variantType ==
                                    CardPrintVariant.Type.PrereleaseStamped ->
                                    "Prerelease Events: $mappedSetName"
                                cardCollectionItemId.cardPrint.card.token ->
                                    "Extras: $mappedSetName"
                                else -> mappedSetName
                              }
                            }
                      }
                },
            "Edition Code" to
                {
                  cardCollectionItemId.cardPrint.set.code.uppercase()
                }, // FIXME the list cards are exported the wrong way
            "Card Number" to
                {
                  if (cardCollectionItemId.cardPrint.set.code == "plst") {
                    cardCollectionItemId.cardPrint.collectorNumber.split("-", limit = 2).last()
                  } else {
                    cardCollectionItemId.cardPrint.collectorNumber
                  }
                },
            "Condition" to { cardCollectionItemId.condition.toDeckboxCondition() },
            "Language" to { cardCollectionItemId.language.toLanguageDeckbox() },
            "Foil" to { if (cardCollectionItemId.finish != Finish.Normal) "foil" else "" },
            "Signed" to { "" },
            "Artist Proof" to
                {
                  if (cardCollectionItemId.variantType == CardPrintVariant.Type.TheList) "proof"
                  else ""
                },
            "Altered Art" to { "" },
            "Misprint" to { "" },
            "Promo" to
                {
                  if (cardCollectionItemId.variantType == CardPrintVariant.Type.PromopackStamped)
                      "promo"
                  else ""
                },
            "Textless" to { "" },
            "My Price" to { purchasePrice?.let { "$$it" } ?: "" },
            "Last Updated" to
                {
                  DATE_FORMAT_DECKBOX.format(dateAdded.toJavaInstant().atOffset(ZoneOffset.UTC))
                },
        ),
    )

internal fun CardLanguage.toLanguageDeckbox(): String =
    when (this) {
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

internal fun CardCondition.toDeckboxCondition(): String =
    when (this) {
      CardCondition.NEAR_MINT -> "Near Mint"
      CardCondition.EXCELLENT -> "Good (Lightly Played)"
      CardCondition.GOOD -> "Played"
      CardCondition.PLAYED -> "Heavily Played"
      CardCondition.POOR -> "Poor"
      else -> throw Exception("unknown condition")
    }
