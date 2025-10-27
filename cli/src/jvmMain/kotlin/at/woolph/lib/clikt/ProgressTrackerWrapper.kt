/* Copyright 2025 Wolfgang Mayer */
package at.woolph.lib.clikt

import at.woolph.utils.ProgressTracker
import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressTaskAnimator
import com.github.ajalt.mordant.animation.progress.advance

class ProgressTrackerWrapper<Context, Unit : Number>(
    val coroutineProgressTaskAnimator: CoroutineProgressTaskAnimator<Context>,
) : ProgressTracker<Context, Unit> {
  override fun updateContext(newContext: Context) {
    coroutineProgressTaskAnimator.update { context = newContext }
  }

  override fun advance(units: Unit) {
    coroutineProgressTaskAnimator.advance(units)
  }

  override fun setTotal(totalUnits: Unit) {
    coroutineProgressTaskAnimator.update { total = totalUnits.toLong() }
  }
}
