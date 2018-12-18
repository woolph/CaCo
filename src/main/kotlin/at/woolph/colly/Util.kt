package at.woolph.colly

import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.Tooltip
import javafx.util.Callback
import org.joda.time.DateTime
import org.joda.time.Duration
import tornadofx.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.ceil
import kotlin.math.max
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun String.toPath() = Paths.get(this)!!

/**
 * doesn't work anyway
 */
class DelegateWrapper<in R, D, T>(private val wrappedDelegate: ReadWriteProperty<R, T>, private val get: (T)->D, private val set: (D)->T): ReadWriteProperty<R, D> {
	override operator fun getValue(thisRef: R, property: KProperty<*>): D {
		return get(wrappedDelegate.getValue(thisRef, property))
	}

	override operator fun setValue(thisRef: R, property: KProperty<*>, value: D) {
		wrappedDelegate.setValue(thisRef, property, set(value))
	}
}


inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
	var sum: Long = 0
	for (element in this) {
		sum += selector(element)
	}
	return sum
}


operator fun DateTime?.minus(dateTime: DateTime) = if(this!=null) Duration(dateTime, this) else Duration(0)

fun gcd(_a: Long, _b: Long): Long {
	var a = _a
	var b = _b
	while (b != 0L) {
		val t = b
		b = a % b
		a = t
	}
	return a
}

const val maxFraction = 15.0
const val maxMin = (60/maxFraction).toLong()
fun Duration.toPolarion(): String {
	val hourComponent = max(this.standardHours, 0L)
	val minuteComponent = ceil((this.standardMinutes - hourComponent*60)/maxFraction).toLong()

	var gcd = gcd(maxMin, minuteComponent)

	return (if(hourComponent!=0L || minuteComponent==0L) "$hourComponent " else "") +
			(if(minuteComponent!=0L) "${minuteComponent/gcd}/${maxMin/gcd} " else "") + "h"
}


var Tooltip.wrapText : Boolean
	get() = this.wrapTextProperty().get()
	set(value) = this.wrapTextProperty().set(value)

open class SmartTableCell2<S, T>(scope: Scope = DefaultScope, owningColumn: TableColumn<S, T>) : SmartTableCell<S, T>(scope, owningColumn) {
	private val cellFormat2: (TableCell<S, T>.(T) -> Unit)? get() = owningColumn.properties["tornadofx.cellFormat2"] as (TableCell<S, T>.(T) -> Unit)?
	override fun updateItem(item: T, empty: Boolean) {
		super.updateItem(item, empty)
		cellFormat2?.invoke(this, item)
	}
}

fun <S, T> TableColumn<S, T>.cellFormat2(scope: Scope = DefaultScope, formatter: TableCell<S, T>.(T) -> Unit) {
	properties["tornadofx.cellFormat2"] = formatter
	if (properties["tornadofx.cellFormatCapable"] != true)
		cellFactory = Callback { SmartTableCell2(scope, it) }
}

fun <S> javafx.scene.control.TableColumn<S, Boolean?>.useCheckbox2() = apply {
	cellFormat {
		graphic = cache {
			alignment = Pos.CENTER
			checkbox {
				selectedProperty().bind(itemProperty())
				setDisable(true)
				style {
					opacity = 1.0
				}
			}
		}
	}
}

fun <S, T> javafx.scene.control.TableColumn<S, T?>.sortable(sortable : Boolean = true) = apply {
	setSortable(sortable)
}

//fun <T> ObservableValue<T>.addListenerAndPerformOnce(listener : ChangeListener<in T>) {
fun <T> ObservableValue<T>.addListenerAndPerformOnce(listener : (ObservableValue<out T>, T?, T?) -> Unit) {
	this.addListener(listener)
	listener(this, null, this.value)
}

class PathStringConverter : javafx.util.StringConverter<Path>() {
	override fun fromString(s : String?) : Path? = Paths.get(s)
	override fun toString(p : Path?) = p?.toString() ?: ""
}

class PathFileNameStringConverter : javafx.util.StringConverter<Path>() {
	override fun fromString(s : String?) : Path? = Paths.get(s)
	override fun toString(p : Path?) = p?.fileName?.toString() ?: ""
}
