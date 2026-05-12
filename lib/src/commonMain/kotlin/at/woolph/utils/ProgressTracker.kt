/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils

interface ProgressTracker<Context, Unit : Number> {
  fun updateContext(newContext: Context)

  fun advance(units: Unit)

  fun setTotal(totalUnits: Unit)

  fun finished()
}
