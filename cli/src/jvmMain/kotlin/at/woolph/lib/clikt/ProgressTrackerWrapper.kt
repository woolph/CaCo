package at.woolph.lib.clikt

import at.woolph.caco.ProgressTracker
import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressTaskAnimator
import com.github.ajalt.mordant.animation.progress.advance

class ProgressTrackerWrapper<Context, Unit: Number>(
  val coroutineProgressTaskAnimator: CoroutineProgressTaskAnimator<Context>,
): ProgressTracker<Context> {
  override fun updateContext(newContext: Context) {
    coroutineProgressTaskAnimator.update {
      context = newContext
    }
  }

  override fun advance(units: Number) {
    coroutineProgressTaskAnimator.advance(units.toLong())
  }

  override fun setTotal(totalUnits: Number) {
    coroutineProgressTaskAnimator.update {
      total = totalUnits.toLong()
    }
  }
}
