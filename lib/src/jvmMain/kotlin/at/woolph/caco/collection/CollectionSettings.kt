/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Finish
import kotlin.math.max

data class CollectionSettings(
    val targetCardPossessions: Map<Finish, IntRange>,
) {
  fun possessionTarget(card: Card, finish: Finish): Int =
      if (card.finishes.contains(finish)) {
        val possessionTargetForFinish = targetCardPossessions[finish]?.first ?: 0
        if (finish != Finish.Normal && !card.finishes.contains(Finish.Normal)) {
          max(possessionTargetForFinish, targetCardPossessions[Finish.Normal]?.first ?: 0)
        } else {
          possessionTargetForFinish
        }
      } else {
        0
      }
}

val DEFAULT_COLLECTION_SETTINGS =
    CollectionSettings(
        targetCardPossessions =
            mapOf(Finish.Normal to 1..33, Finish.Foil to 0..0, Finish.Etched to 0..0)
    )
