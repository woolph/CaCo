package at.woolph.caco.cli

import org.apache.commons.lang3.StringUtils
import org.jline.reader.LineReader

class InputReader(
    private val lineReader: LineReader,
    private val mask: Char = at.woolph.caco.cli.InputReader.Companion.DEFAULT_MASK,
) {
    @JvmOverloads
    fun prompt(prompt: String, defaultValue: String? = null, echo: Boolean = true): String? {
        var answer: String? = ""
        answer = if (echo) {
            lineReader.readLine("$prompt: ")
        } else {
            lineReader.readLine("$prompt: ", mask)
        }
        return if (StringUtils.isEmpty(answer)) {
            defaultValue
        } else answer
    }

    companion object {
        const val DEFAULT_MASK = '*'
    }
}
