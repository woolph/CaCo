package at.charlemagne.libs.util

import java.util.function.Predicate

fun <T> Sequence<Predicate<T>>.combineOr() = Predicate<T> { x: T ->  this.any { it.test(x) } }
fun <T> Sequence<Predicate<T>>.combineAnd() = Predicate<T> { x: T ->  this.all { it.test(x) } }

fun <T> Collection<Predicate<T>>.combineOr() = Predicate<T> { x: T ->  this.any { it.test(x) } }
fun <T> Collection<Predicate<T>>.combineAnd() = Predicate<T> { x: T ->  this.all { it.test(x) } }