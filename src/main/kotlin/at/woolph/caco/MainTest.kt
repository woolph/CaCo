package at.woolph.caco

import kotlinx.coroutines.runBlocking
import kotlin.text.replace

fun main() = runBlocking {
    println("100s★".replace(Regex("s(?=★?$)"), ""))
//    println(request<ArchidektDecklist>("https://archidekt.com/api/decks/8921539/"))
//    sequence<String> { yield(readln()) }.onEach { println("readLine: $it") }.takeWhile { it.isNotBlank() }.forEach { println("line: $it") }
}
