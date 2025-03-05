package at.woolph.caco

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
//    println(request<ArchidektDecklist>("https://archidekt.com/api/decks/8921539/"))
    sequence<String> { yield(readln()) }.onEach { println("readLine: $it") }.takeWhile { it.isNotBlank() }.forEach { println("line: $it") }
}
