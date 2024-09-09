package at.woolph.libs.util

import at.woolph.libs.log.logger
import java.time.Duration


private val LOG by logger("Execution")

fun <T> retry(maxRetries: Int, pause: Duration, execution: () -> T): T {
	var retry = 0
	while (true) {
		try {
			return execution()
		} catch (e: Exception) {
			LOG.debug(e) { "execution #$retry failed" }
			retry++
			if(retry<maxRetries) {
				LOG.debug(e) { "retry in ${pause.toMillis()} ms" }
				Thread.sleep(pause.toMillis())
			} else {
				throw e
			}
		}
	}
}
