package at.woolph.libs.util

fun <T> Sequence<T>.printEach(formatter: (T) -> String = { it.toString() }) = this.onEach { println(formatter(it)) }
