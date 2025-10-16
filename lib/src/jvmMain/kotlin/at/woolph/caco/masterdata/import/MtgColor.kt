/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

import at.woolph.caco.cli.manabase.ColorIdentity
import at.woolph.caco.cli.manabase.ManaColor
import java.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MtgColor {
  @SerialName("W") White,
  @SerialName("U") Blue,
  @SerialName("B") Black,
  @SerialName("R") Red,
  @SerialName("G") Green,
  @SerialName("C") Colorless,
  @SerialName("T") Tap,
  ;

  fun toManaColor() =
      when (this) {
        White -> ManaColor.White
        Blue -> ManaColor.Blue
        Black -> ManaColor.Black
        Red -> ManaColor.Red
        Green -> ManaColor.Green
        Colorless -> ManaColor.Colorless
        else -> null
      }
}

inline fun <reified E : Enum<E>> Sequence<E>.toEnumSet() =
    this.toSet().let { if (it.isEmpty()) EnumSet.noneOf(E::class.java) else EnumSet.copyOf(it) }

fun Set<MtgColor>.toColorIdentity() =
    ColorIdentity(this.asSequence().mapNotNull(MtgColor::toManaColor).toEnumSet())
