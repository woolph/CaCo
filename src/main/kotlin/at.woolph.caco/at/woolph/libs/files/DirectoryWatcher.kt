package at.woolph.libs.files

import at.woolph.libs.log.logger
import java.io.IOException
import java.nio.file.*
import kotlin.concurrent.thread

data class DirectoryChangedEvent(val path: Path, val kind: WatchEvent.Kind<*>)

@FunctionalInterface
interface DirectoryListener {
	fun directoryChanged(e: DirectoryChangedEvent)

	companion object {
		operator fun invoke(f: (DirectoryChangedEvent) -> Unit) = object : DirectoryListener {
			override fun directoryChanged(e: DirectoryChangedEvent) = f(e)
		}
	}
}

fun Path.watch() = DirectoryWatcher(this).apply { start() }
fun Path.watch(listener: DirectoryListener) = watch().apply { addDirectoryListener(listener) }
fun Path.watch(handler: (DirectoryChangedEvent) -> Unit) = watch(DirectoryListener(handler))

class DirectoryWatcher(val path: Path) : Runnable {
	companion object {
		val LOG by logger()
	}

	private var watcherThread: Thread? = null
	private val directoryListeners = mutableListOf<DirectoryListener>()

	fun addDirectoryListener(dl: DirectoryListener) {
		synchronized(directoryListeners) {
			directoryListeners.add(dl)
		}
	}

	fun removeDirectoryListener(dl: DirectoryListener) {
		synchronized(directoryListeners) {
			directoryListeners.remove(dl)
		}
	}

	fun start() {
        LOG {
            if (!path.exists) {
                throw NoSuchFileException(path.toString())
            }
            if (!path.isDirectory) {
                throw NotDirectoryException(path.toString())
            }

            if (watcherThread?.isAlive != true) {
                LOG.debug("starting new thread")
                watcherThread = thread(start = true, isDaemon = true, block = this::run)
            }
        }
	}

	fun stop() {
		watcherThread?.interrupt()
	}

	fun restart() {
		stop()
		start()
	}

	override fun run() {
		try {
			val watcher = FileSystems.getDefault().newWatchService()
			path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)

			while (!Thread.currentThread().isInterrupted) {
				// wait for key to be signaled
				val key = watcher.take()

				for (event in key.pollEvents()) {
					//Platform.runLater { fireDirectoryChangedEvent(DirectoryChangedEvent(event.context() as Path, event.kind())) }
					fireDirectoryChangedEvent(DirectoryChangedEvent(event.context() as Path, event.kind()))
				}

				// Reset the key -- this step is critical if you want to
				// receive further watch events.  If the key is no longer valid,
				// the directory is inaccessible so exit the loop.
				if (!key.reset()) {
					// TODO exception handling?
					LOG.warn("watch key for path {} was no longer valid, thread {} will finish", path.toString(), Thread.currentThread().name)
					break
				}
			}
		} catch (x: InterruptedException) {
			LOG.debug("thread {} was interrupted", Thread.currentThread().name)
		} catch (x: IOException) {
			// TODO exception handling?
			LOG.error("exception occured", x)
		}

	}

	private fun fireDirectoryChangedEvent(e: DirectoryChangedEvent) {
		synchronized(directoryListeners) {
			for (dl in directoryListeners) {
				dl.directoryChanged(e)
			}
		}
	}
}
