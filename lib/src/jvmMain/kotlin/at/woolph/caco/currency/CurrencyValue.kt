/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.currency

import java.util.Currency

data class CurrencyValue(
    val value: Double,
    val currency: Currency,
) : Comparable<CurrencyValue> {
  override fun compareTo(other: CurrencyValue): Int = value.compareTo(other.value)

  override fun toString() =
      String.format("%.${currency.defaultFractionDigits}f%s", value, currency.symbol)

  companion object {
    fun eur(value: Double): CurrencyValue = CurrencyValue(value, Currencies.EUR)

    fun usd(value: Double): CurrencyValue = CurrencyValue(value, Currencies.USD)
  }
}
