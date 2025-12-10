/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.currency

@JvmInline
value class Percentage(
    val value: Double,
) {
  override fun toString() = String.format("%1.1f%%", value * 100)
}
