/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.manabase

import at.woolph.caco.cli.DecklistEntry
import at.woolph.caco.datamodel.ColorIdentity
import at.woolph.caco.datamodel.MtgColor
import at.woolph.caco.datamodel.decks.Format
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.v1.core.match
import kotlin.collections.filterNot
import kotlin.math.round
import kotlin.text.Regex
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.pow

data class DecklistEntryCard(
    val card: Card,
    val count: Int = 1,
) {
  override fun toString() = "$count ${card.name}"
}

fun Map<String, Int>.toDecklistEntries(): Collection<DecklistEntry> = map { (cardName, count) ->
  DecklistEntry(cardName, count)
}

fun Collection<DecklistEntry>.toDecklistEntryCards(): Collection<DecklistEntryCard> = transaction {
  map { (cardName, count) ->
    DecklistEntryCard(
        try {
          Card.find { Cards.name match cardName }.limit(1).first()
        } catch (t: Throwable) {
          throw IllegalArgumentException("card $cardName not found", t)
        },
        count,
    )
  }
}

// https://www.channelfireball.com/article/How-Many-Lands-Do-You-Need-in-Your-Deck-An-Updated-Analysis/cd1c1a24-d439-4a8e-b369-b936edb0b38a/
fun Collection<DecklistEntryCard>.suggestedLandCount(): Int {
  val baseLine = 31.42
  val averageManaValueFactor = 3.13
  val cheapDrawRampFactor = 0.28
  val untappedMdfcFactor = 1.0 // 0.74 according to Frank Karsten
  val tappedMdfcFactor = 1.0 // 0.38 according to Frank Karsten

  val averageManaValue = sumOf { it.card.manaValue.toDouble() } / size
  val untappedMdfcCount = count { it.card.isMDFCLandUntapped }
  val tappedMdfcCount = count { it.card.isMDFCLandTapped }
  val cheapDrawCount = count { it.card.isCheapCardDraw }
  val cheapRampCount = count { it.card.isCheapRamp }
  val cheapDrawRampCount = cheapDrawCount + cheapRampCount

  println("averageManaValue = $averageManaValue")
  println("untappedMdfcCount = $untappedMdfcCount")
  println("tappedMdfcCount = $tappedMdfcCount")
  println("cheapDrawCount = $cheapDrawCount")
  println("cheapRampCount = $cheapRampCount")

  return round(
          baseLine + averageManaValueFactor * averageManaValue -
              cheapDrawRampFactor * cheapDrawRampCount.toDouble() -
              tappedMdfcFactor * tappedMdfcCount -
              untappedMdfcFactor * untappedMdfcCount
      )
      .toInt()
}

fun Collection<DecklistEntryCard>.pipDistribution(): PipDistribution {
  val pipCount =
    MtgColor.entries
          .map { mtgColor ->
            mtgColor to
                mapNotNull { it.card.manaCost }
                    .sumOf { manaCost ->
                      Regex(Regex.escape(mtgColor.symbol))
                        .findAll(manaCost)
                        .count()
                        .toDouble().pow(2.0)
                        .toInt()
                    }
          }
          .filter { it.second > 0 }
          .associate { it }

  return PipDistribution(
      pipCount.values.sum().let { totalPipCount ->
        pipCount.mapValues { it.value.toDouble() / totalPipCount.toDouble() }
      }
  )
}

fun Collection<DecklistEntryCard>.meanPipRequirement() =
    PipDistribution(
        MtgColor.entries
            .associateWith { MtgColor ->
              this@meanPipRequirement.filterNot { it.card.isLand }
                  .mapNotNull { it.card.manaCost }
                  .map { Regex(Regex.escape(MtgColor.symbol)).findAll(it).count() }
                  .average()
            }
            .filterValues { it > 0.0 }
    )

fun Collection<LandCard>.meanProduction(totalCount: Int = this@meanProduction.size) =
    MtgColor.entries.associateWith { color ->
      this@meanProduction.count { color in it.card.producedMana!! }.toDouble() / totalCount
    }

fun generateManabase(
    selectionCriterion: SelectionCriterion,
    decklist: Collection<DecklistEntryCard>,
    deckFormat: Format,
): List<DecklistEntry> = transaction {
  val decklistWithoutLands = decklist.filterNot { it.card.isLand }
  val suggestedLandCount = decklistWithoutLands.suggestedLandCount()
  val pipDistribution = decklistWithoutLands.meanPipRequirement()
  println("suggested land count = $suggestedLandCount")
  println("pipDistribution = $pipDistribution")

  val neededColors = pipDistribution.pipDistribution.keys

  val (filteredBasicLands, filteredNonBasicLands) = getLands()
    .filter { it.card.isLegalIn(deckFormat) }
    .filter { selectionCriterion.commanderColorIdentity.contains(it) }
    .filter { (it.card.priceNormal?.value ?: 1000.0) <= selectionCriterion.maxPricePerCard }
    .partition { it.isBasic }

  val mutableFilteredNonBasicLands = filteredNonBasicLands.toMutableList()
  val selectedLands = mutableListOf<LandCard>()

  while (
      selectedLands.size < suggestedLandCount - selectionCriterion.minBasicLandCount &&
      filteredNonBasicLands.isNotEmpty()
  ) {
    val pickedLand =
      mutableFilteredNonBasicLands.maxBy {
          it.desirability(
              selectionCriterion,
              pipDistribution,
              selectedLands.meanProduction(suggestedLandCount),
          ) / (it.card.priceNormal?.value ?: 1000.0)
        }
    selectedLands.add(pickedLand)
    mutableFilteredNonBasicLands.remove(pickedLand)

    println("picked ${pickedLand.card.name}")
  }

  // each basic once
  filteredBasicLands
      .filter { it.card.producedMana?.colors?.any { neededColors.contains(it) } == true }
      .forEach { pickedLand ->
        selectedLands.add(pickedLand)
        println("picked ${pickedLand.card.name}")
      }

  while (selectedLands.size < suggestedLandCount) {
    val pickedLand =
        filteredBasicLands.maxBy {
          it.desirability(
              selectionCriterion,
              pipDistribution,
              selectedLands.meanProduction(suggestedLandCount),
          )
        }
    selectedLands.add(pickedLand)
    println("picked ${pickedLand.card.name}")
  }

  val lands = selectedLands.groupBy { it }.mapValues { (_, list) -> list.size }.entries

  println(
      "productionDistribution = ${MtgColor.entries.associateWith { color -> lands.filter { (land, _) -> color in land.card.producedMana!! }.sumOf { (_, count) -> count }.toDouble() / lands.size }}"
  )

  return@transaction lands.map { (land, count) -> DecklistEntry(land.card.name, count) }
}

class SelectionCriterion(
    val commanderColorIdentity: ColorIdentity,
    val minBasicLandCount: Int = commanderColorIdentity.colorIdentity.size,
    val basicLandFactor: Double = 1.0,
    val basicLandTypeFactors: Double = 0.05,
    val fastStartFactor: Double = 1.2,
    val maxPricePerCard: Double = Double.MAX_VALUE,
    val artifactFactor: Double =
        0.05, // lower because the can be interacted better (set high if you have artifact
    // synergies)
    val enchantmentFactor: Double =
        0.05, // lower because the can be interacted better (set high if you have enchantment
    // synergies)
    val snowFactor: Double = 0.05,
    val gateFactor: Double = 0.05,
    val painFactor: Double = 0.95,
    val lifegainFactor: Double = 1.05,
    val surveilFactor: Double = 1.15,
    val scryFactor: Double = 1.1,
)

operator fun ColorIdentity.contains(land: LandCard) = this.contains(land.card.colorIdentity)

class PipDistribution(val pipDistribution: Map<MtgColor, Double>) {

  override fun toString(): String = pipDistribution.toString()

  fun weighting(land: LandCard) =
      pipDistribution.entries
          .filter { (color, _) -> land.card.producedMana?.contains(color) == true }
          .sumOf { (_, pipWeight) -> pipWeight }

  operator fun minus(meanProduction: Map<MtgColor, Double>) =
      PipDistribution(
          pipDistribution.mapValues { (key, value) -> value - (meanProduction[key] ?: 0.0) }
      )
}
