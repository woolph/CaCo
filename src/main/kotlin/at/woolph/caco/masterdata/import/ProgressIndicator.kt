package at.woolph.caco.masterdata.import

import javafx.beans.property.Property
import javafx.beans.property.SimpleDoubleProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.lang.Double.min

class ProgressIndicator(
    val progress: Property<Number> = SimpleDoubleProperty(0.0),
    val desiredIncrement: Double = 0.001,
    val maxValue: Double = 1.0,
) {
    var lastProgressUpdate = progress.value.toDouble()

    suspend fun update(current: Number, max: Number) {
        val currentProgress = maxValue * (current.toDouble() / max.toDouble())

        if (currentProgress - lastProgressUpdate < desiredIncrement) {
            withContext(Dispatchers.Main.immediate) {
                progress.setValue(min(currentProgress, maxValue))
            }
            lastProgressUpdate = currentProgress
        }
    }

    suspend fun finished() {
        withContext(Dispatchers.Main.immediate) {
            progress.setValue(maxValue)
        }
    }
}

fun <T> Flow<T>.updateProgressIndicator(progressIndicator: ProgressIndicator?, totalNumber: Int?): Flow<T> =
    if (progressIndicator != null && totalNumber != null) {
        var numberOfProcessedItems: Int = 0
        onEach {
            numberOfProcessedItems++
            progressIndicator.update(numberOfProcessedItems, totalNumber)
        }
    } else {
        this
    }
