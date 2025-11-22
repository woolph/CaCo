/* Copyright 2025 Wolfgang Mayer */
package at.woolph.lib.clikt

import com.github.ajalt.clikt.core.UsageError
import java.nio.file.Path

class NoFileFoundError(
    val location: Path,
    message: String? = null,
    paramName: String? = null,
) : UsageError(message, paramName, 2)
