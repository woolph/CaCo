/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils

fun <A : Comparable<A>> A?.compareToNullable(
    other: A?,
    isNullLowerWeightThanValue: Boolean = true,
): Int? {
  if (this == null && other == null) return null
  if (this == null) return if (isNullLowerWeightThanValue) -1 else 1
  if (other == null) return if (isNullLowerWeightThanValue) 1 else -1
  val compareResult = this.compareTo(other)
  if (compareResult == 0) return null
  return compareResult
}
