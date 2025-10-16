package at.woolph.caco

interface ProgressTracker<Context, Unit: Number> {
  fun updateContext(newContext: Context)
  fun advance(units: Unit)
  fun setTotal(totalUnits: Unit)
}
