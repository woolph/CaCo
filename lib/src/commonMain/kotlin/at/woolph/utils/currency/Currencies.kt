/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.currency

object Currencies {
  val USD: Currency = _USD
  val EUR: Currency = _EUR
}

internal expect val _USD: Currency
internal expect val _EUR: Currency
