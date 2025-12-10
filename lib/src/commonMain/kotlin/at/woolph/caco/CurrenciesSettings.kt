/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco

import at.woolph.utils.currency.Currencies
import at.woolph.utils.currency.Currency
import at.woolph.utils.currency.CurrencyValue

object CurrenciesSettings {
  val exchangeRateToEur =
      mutableMapOf(
          Currencies.USD to 0.86,
          Currencies.EUR to 1.0,
      )

  fun exchangeRateFromTo(from: Currency, to: Currency): Double =
      exchangeRateToEur(from) / exchangeRateToEur(to)

  fun exchangeRateToEur(currency: Currency): Double =
      exchangeRateToEur[currency] ?: throw IllegalArgumentException("unknown currency $currency")
}

fun CurrencyValue.exchangeTo(newCurrency: Currency) =
    CurrencyValue(value * CurrenciesSettings.exchangeRateFromTo(currency, newCurrency), newCurrency)
