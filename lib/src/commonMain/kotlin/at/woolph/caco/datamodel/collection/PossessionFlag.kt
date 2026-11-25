/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.collection

enum class PossessionFlag {
  COLLECTION,
  DUPLICATE,
  EXCESS,
  ;

  override fun toString() =
      when (this) {
        COLLECTION -> "collection"
        DUPLICATE -> "duplicate"
        EXCESS -> "excess"
      }
}
