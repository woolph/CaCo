package at.woolph.utils.currency

actual class Currency(val currency: java.util.Currency) {
  actual val defaultFractionDigits: Int
    get() = currency.defaultFractionDigits
  actual val symbol: String
    get() = currency.symbol
}
