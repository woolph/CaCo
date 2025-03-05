package at.woolph.caco.gui.view

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import java.util.function.Predicate


fun <T> filterPredicate(observableValues: Collection<ObservableValue<*>>, block: (T) -> Boolean): ObservableValue<Predicate<T>> =
    Bindings.createObjectBinding({ Predicate { item: T -> block(item) } }, *observableValues.toTypedArray())

fun <T> ObservableList<T>.filteredBy(observableValues: Collection<ObservableValue<*>>, block: (T) -> Boolean): FilteredList<T> =
    FilteredList(this).apply {
        predicateProperty().bind(filterPredicate(observableValues, block))
    }
