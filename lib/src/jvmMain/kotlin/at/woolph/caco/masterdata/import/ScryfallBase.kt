/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.masterdata.import

interface ScryfallBase {
  fun isValid(): Boolean

  fun checkValid() {
    if (!isValid()) {
      throw IllegalStateException("object is not valid")
    }
  }
}
