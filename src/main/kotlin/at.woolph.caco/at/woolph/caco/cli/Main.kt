package at.woolph.caco.cli

import org.jline.reader.LineReader
import org.jline.terminal.Terminal
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy


@SpringBootApplication
class MainCliApplication {
    @Bean
    fun inputReader(@Lazy lineReader: LineReader) = InputReader(lineReader)

    @Bean
    fun output(@Lazy terminal: Terminal) =  Output(terminal)
}

fun main(args: Array<String>) {
    runApplication<MainCliApplication>(*args)
}
