/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils.pdf

expect class Font {
  fun withSize(size: Float): SizedFont
}
