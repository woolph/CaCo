package at.woolph.caco

import at.woolph.caco.importer.deck.ArchidektDecklist
import at.woolph.caco.importer.deck.request
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
//    println(request<ArchidektDecklist>("https://archidekt.com/api/decks/8921539/"))
    sequence<String> { yield(readln()) }.onEach { println("readLine: $it") }.takeWhile { it.isNotBlank() }.forEach { println("line: $it") }
}
