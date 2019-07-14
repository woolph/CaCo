package at.charlemagne.libs.concurrency

import java.util.concurrent.ThreadFactory

class NamedThreadFactory(val threadName: String, val daemon: Boolean = false): ThreadFactory {
	var threadCount = 0

	override fun newThread(r: Runnable?): Thread {
		return Thread(r).apply {
			name = String.format(threadName, threadCount)
			isDaemon = daemon
		}
	}
}