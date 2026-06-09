/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.currency

data class CurrencyValue(
    val value: Double,
    val currency: Currency,
) : Comparable<CurrencyValue> {
  override fun compareTo(other: CurrencyValue): Int = value.compareTo(other.value)

  override fun toString() =
      String.format("%s %.${currency.defaultFractionDigits}f", currency.symbol, value)

  operator fun unaryMinus() = copy(value = -value)

  operator fun unaryPlus() = this

  operator fun plus(other: CurrencyValue): CurrencyValue =
      if (currency != other.currency)
          throw IllegalArgumentException("Currency values can only be added to other currency")
      else copy(value = value + other.value)

  operator fun plus(other: Number): CurrencyValue = copy(value = value + other.toDouble())

  operator fun minus(other: CurrencyValue): CurrencyValue =
      if (currency != other.currency)
          throw IllegalArgumentException("Currency values can only be added to other currency")
      else copy(value = value - other.value)

  operator fun minus(other: Number): CurrencyValue = copy(value = value - other.toDouble())

  operator fun times(other: Number): CurrencyValue = copy(value = value * other.toDouble())

  operator fun div(other: Number): CurrencyValue = copy(value = value / other.toDouble())

  companion object {
    fun eur(value: Double): CurrencyValue = CurrencyValue(value, Currencies.EUR)

    fun usd(value: Double): CurrencyValue = CurrencyValue(value, Currencies.USD)
  }
}

fun Collection<CurrencyValue>.sum(): CurrencyValue =
    reduceOrNull { acc, value -> acc + value } ?: CurrencyValue.usd(0.0)
