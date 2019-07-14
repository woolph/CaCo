package at.charlemagne.libs.concurrency

import at.charlemagne.libs.log.logger
import java.util.concurrent.locks.StampedLock

class StampedLocked(val lock: StampedLock, var stamp: Long) {
	companion object {
		private val LOG by logger()
	}

	val lockValid get() = lock.validate(stamp)

	fun <T> convertToWriteLock(action: StampedLocked.() -> T): T {
		LOG.trace { "convertToWriteLock" }
		stamp = lock.tryConvertToWriteLock(stamp)
		if (stamp == 0L) {
			LOG.debug { "Could not convert to write lock" }
			stamp = lock.writeLock()
		}
		return this.action()
	}
}

inline fun <T> StampedLock.readLock(action: StampedLocked.() -> T): T {
	val stampedLock = StampedLocked(this, readLock())
	try {
		return stampedLock.action()
	} finally {
		unlock(stampedLock.stamp)
	}
}


inline fun <T> StampedLock.writeLock(action: StampedLocked.() -> T): T {
	val stampedLock = StampedLocked(this, writeLock())
	try {
		return stampedLock.action()
	} finally {
		unlock(stampedLock.stamp)
	}
}
