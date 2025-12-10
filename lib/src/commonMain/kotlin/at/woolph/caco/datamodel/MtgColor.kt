/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MtgColor(
  val symbol: String,
  val basicLandType: String,
) {
  @SerialName("W") White("{W}", "Plains"),
  @SerialName("U") Blue("{U}", "Island"),
  @SerialName("B") Black("{B}", "Swamp"),
  @SerialName("R") Red("{R}", "Mountain"),
  @SerialName("G") Green("{G}", "Forest"),
  @SerialName("C") Colorless("{C}", "Wastes"),
  @SerialName("T") Tap("{T}", "n/a"),
  ;
}

fun Set<MtgColor>.toColorIdentity() = ColorIdentity(this)

fun Set<MtgColor>.toColor() = Color(this)
