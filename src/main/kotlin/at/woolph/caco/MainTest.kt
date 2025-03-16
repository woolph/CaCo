package at.woolph.caco

import at.woolph.caco.collection.CardCollectionItem
import at.woolph.caco.collection.exportArchidekt
import at.woolph.caco.collection.importSequenceArchidekt
import at.woolph.caco.collection.importSequenceDeckbox
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardRepresentation
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlin.io.path.Path

fun main() = runBlocking {
  Databases.init()

  // FIXME commander display cards, raggadragga & rampant growth promo edged-foil is not detected correctly by archidekt due to wrong finish type foil (should be edged)
  // FIXME fix deckbox promo import issue!!!!

  transaction {
//    println("100s★".replace(Regex("s(?=★?$)"), ""))
//    println(request<ArchidektDecklist>("https://archidekt.com/api/decks/8921539/"))
//    sequence<String> { yield(readln()) }.onEach { println("readLine: $it") }.takeWhile { it.isNotBlank() }.forEach { println("line: $it") }

    val deckboxLatest = Path("C:\\Users\\001121673\\Downloads\\Inventory_woolph_2025.March.16.csv")
    val olderFile = deckboxLatest
//    val olderFile = Path("C:\\Users\\001121673\\Downloads\\archidekt-collection-export-2025-02-28.csv")
    val newerFile = Path("C:\\Users\\001121673\\Downloads\\archidekt-collection-export-2025-03-03.csv")
    val newerFileAdditions = Path("new-records.csv")
    val olderFileEntriesMissingInNewerFile = Path("old-missing.csv")

    val dateAdded = Instant.parse("2025-03-03T00:00:00Z")
    val newerFileEntries2 = importSequenceArchidekt(newerFile)
      .mapNotNull { it.getOrNull() }
      .groupBy { it.cardCollectionItemId.copy(
        variantType = null,
        language = when(it.cardCollectionItemId.language) {
          CardLanguage.JAPANESE -> CardLanguage.ENGLISH

          else -> it.cardCollectionItemId.language
        },
      )
      }
      .entries.map { entry -> CardCollectionItem(
        entry.value.sumOf { it.quantity },
        entry.key,
        dateAdded = dateAdded,
        purchasePrice = null) }
      .toMutableList()

    val unableToBeRemoved = importSequenceDeckbox(olderFile)
      .mapNotNull { it.getOrNull() }
      .groupBy { it.cardCollectionItemId.copy(
        variantType = null,
        language = when(it.cardCollectionItemId.language) {
          CardLanguage.JAPANESE -> CardLanguage.ENGLISH

          else -> it.cardCollectionItemId.language
        },
      )
      }
      .entries.map { entry -> CardCollectionItem(
        entry.value.sumOf { it.quantity},
        entry.key,
        dateAdded = dateAdded,
        purchasePrice = null) }
      .filterNot {
        newerFileEntries2.removeIf { nfe ->
          nfe.quantity == it.quantity && nfe.cardCollectionItemId == it.cardCollectionItemId
        }
      }
      .toMutableList()

    newerFileEntries2.exportArchidekt(newerFileAdditions)

//    val deckboxEntries = importSequenceDeckbox(deckboxLatest)
//      .mapNotNull {
//        it.getOrNull()
//      }
//      .toList()

    unableToBeRemoved
//      .filter {
//        deckboxEntries.none { dbe ->
//          it.cardCollectionItemId.card.isVariantOf(dbe.cardCollectionItemId.card)
//              && it.cardCollectionItemId.condition == dbe.cardCollectionItemId.condition
//              && it.cardCollectionItemId.language == dbe.cardCollectionItemId.language
//              && it.cardCollectionItemId.foil == dbe.cardCollectionItemId.foil
//              && dbe.quantity == it.quantity
//        }
//      }
      .exportArchidekt(olderFileEntriesMissingInNewerFile)
  }
}

fun CardRepresentation.isVariantOf(card: Card) = card.variants.any { it == this }