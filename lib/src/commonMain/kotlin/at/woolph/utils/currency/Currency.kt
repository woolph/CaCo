/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils.currency

expect class Currency {
  val defaultFractionDigits: Int
  val symbol: String
}
