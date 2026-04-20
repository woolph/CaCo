/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui.libs

import at.woolph.utils.ProgressTracker
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProgressIndicator(
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate,
    var context: String = "",
    val progress: DoubleProperty = SimpleDoubleProperty(0.0),
    var maxValue: Double = 1.0,
) : ProgressTracker<String, Double> {
  val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

  override fun updateContext(newContext: String) {
    context = newContext
  }

  override fun advance(units: Double) {
    coroutineScope.launch { progress.setValue(progress.value.toDouble() + units) }
  }

  override fun setTotal(totalUnits: Double) {
    coroutineScope.launch { progress.setValue(progress.getValue() / maxValue * totalUnits) }
    maxValue = totalUnits
  }

  override fun finished() {
    coroutineScope.launch { progress.setValue(maxValue) }
  }
}
