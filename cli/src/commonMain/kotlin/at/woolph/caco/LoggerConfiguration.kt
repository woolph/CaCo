/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

object CacoLogger :
    Logger(
        config =
            loggerConfigInit(
                platformLogWriter(NoTagFormatter),
                minSeverity = Severity.Verbose,
            ),
        tag = "CaCo",
    )
