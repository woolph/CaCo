/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.manabase

import at.woolph.caco.datamodel.MtgColor
import at.woolph.caco.datamodel.decks.Format
import at.woolph.caco.datamodel.initDatabase
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.min

fun main() {
  initDatabase()
  getLands()
    .filter { it.card.isLegalIn(Format.Commander) }
    .forEach { (landCard,_,doesTapForManaImmediately)  ->
      val colors = landCard.colorIdentity.toString()
      val production = landCard.producedMana.toString()
      println("${landCard.name} - $colors - $production - doesTapForManaImmediately: $doesTapForManaImmediately - ${landCard.lowestPrice}")
    }
}

fun multiply(first: Double, vararg factors: Double) = factors.reduce { acc, d -> acc * d }

data class LandCard(
  val card: Card,
  val entersTapped: Boolean = false,
  val doesTapForManaImmediately: Boolean = true,
) {
  fun isType(type: String) = card.type?.contains(type) == true
  val isBasic: Boolean get() = isType("Basic")
  val basicLandTypes: Set<MtgColor> = MtgColor.entries.filter { isType(it.basicLandType) }.toSet()

  fun desirability(
    selectionCriterion: SelectionCriterion,
    pipDistribution: PipDistribution,
    alreadyProduced: Map<MtgColor, Double>,
  ) =
    multiply(
      1.0,
      (pipDistribution - alreadyProduced).weighting(
        this
      ), // FIXME correct the alreadyProduced weighting
      min(
        1.0,
        ((card.producedMana?.colors?.size?.toDouble() ?: 0.0) + 1) /
          (selectionCriterion.commanderColorIdentity.colorIdentity.size.toDouble() + 1),
      ),
      (1.0 +
        basicLandTypes.size *
        selectionCriterion
          .basicLandTypeFactors), // increased desirability due to fetchability
//      additionalDesirability(selectionCriterion),
      if (isBasic) selectionCriterion.basicLandFactor
      else 1.0, // basics are more desirable with evolving wilds et al, rampant growth,
      // wayfarer's bauble, etc.
      if (doesTapForManaImmediately) selectionCriterion.fastStartFactor else 1.0,
      if (isType("Artifact")) selectionCriterion.artifactFactor else 1.0,
      if (isType("Enchantment")) selectionCriterion.enchantmentFactor else 1.0,
      if (isType("Snow")) selectionCriterion.snowFactor else 1.0,
      if (isType("Gate")) selectionCriterion.gateFactor else 1.0,
//      if (pain) selectionCriterion.painFactor else 1.0,
//      if (lifegain) selectionCriterion.lifegainFactor else 1.0,
//      if (surveil) selectionCriterion.surveilFactor else 1.0,
//      if (scry) selectionCriterion.scryFactor else 1.0,
    )
}

fun getLands(): Collection<LandCard> = transaction {
  Card.find { Cards.type like "%Land%" }
    .distinctBy { it.name }
    .map { landCard ->
      val entersTapped = landCard.oracleText.contains("This land enters tapped.")

      LandCard(
        card = landCard,
        entersTapped = entersTapped,
        doesTapForManaImmediately = !(entersTapped || landCard.type?.contains("Creature") == true),
      )
    }
}
