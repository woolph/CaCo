/* Copyright 2025 Wolfgang Mayer */
package at.woolph.libs

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.ParameterFormatter
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.ProcessedArgument
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.mordant.terminal.ConfirmationPrompt
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.Prompt
import java.util.*

fun <T> ProcessedArgument<List<T>, T>.prompt(
    text: String? = null,
    default: List<T>? = null,
    hideInput: Boolean = false,
    promptSuffix: String = ": ",
    showDefault: Boolean = true,
    requireConfirmation: Boolean = false,
    confirmationPrompt: String = "Repeat for confirmation",
    confirmationMismatchMessage: String = "Values do not match, try again",
): ProcessedArgument<List<T>, T> = transformAll { invocations ->
    val promptText = text ?: name.replace(Regex("\\W"), " ")
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    if (invocations.isNotEmpty()) return@transformAll invocations
    if (context.errorEncountered) throw Abort()

    val builder: (String) -> Prompt<List<T>> = {
        object : Prompt<List<T>>(
            prompt = it,
            terminal = context.terminal,
            default = default,
            showDefault = showDefault,
            hideInput = hideInput,
            promptSuffix = promptSuffix,
        ) {
            override fun convert(input: String): ConversionResult<List<T>> {
                val ctx = ArgumentTransformContext(this@transformAll, context)
                try {
//                    val v = listOf(transformValue(ctx, input))
                    val v = transformAll(ctx, input.split(" ").map { transformValue(ctx, it) })
                    transformValidator.invoke(this@transformAll, v)
                    return ConversionResult.Valid(v)
                } catch (e: UsageError) {
                    e.context = e.context ?: context
                    return ConversionResult.Invalid(
                        e.formatMessage(
                            context.localization,
                            ParameterFormatter.Plain
                        )
                    )
                }
            }
        }
    }
    val result = if (requireConfirmation) {
        ConfirmationPrompt.create(
            promptText,
            confirmationPrompt,
            confirmationMismatchMessage,
            builder
        ).ask()
    } else {
        builder(promptText).ask()
    }
    return@transformAll result ?: throw Abort()
}
