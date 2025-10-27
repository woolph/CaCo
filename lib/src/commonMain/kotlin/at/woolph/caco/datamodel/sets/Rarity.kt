/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

enum class Rarity {
  OTHER,
  COMMON,
  UNCOMMON,
  RARE,
  MYTHIC,
  ;

  override fun toString() =
      when (this) {
        COMMON -> "C"
        UNCOMMON -> "U"
        RARE -> "R"
        MYTHIC -> "M"
        else -> "?"
      }
}

fun String.parseRarity() =
    when (this) {
      "common" -> Rarity.COMMON
      "uncommon" -> Rarity.UNCOMMON
      "rare" -> Rarity.RARE
      "mythic" -> Rarity.MYTHIC
      else -> Rarity.OTHER
    }
