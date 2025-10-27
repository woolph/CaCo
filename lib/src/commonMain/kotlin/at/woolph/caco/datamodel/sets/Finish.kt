/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Finish {
  @SerialName("nonfoil") Normal,
  @SerialName("foil") Foil,
  @SerialName("etched") Etched,
  ;

  companion object {
    fun parse(finish: String) =
        when (finish.lowercase()) {
          "normal",
          "nonfoil" -> Normal
          "foil" -> Foil
          "etched" -> Etched
          else -> throw IllegalArgumentException("Unknown finish $finish")
        }
  }
}
