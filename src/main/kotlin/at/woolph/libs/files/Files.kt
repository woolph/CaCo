package at.woolph.libs.files

import at.charlemagne.libs.log.logger
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.function.BiPredicate
import java.util.jar.JarInputStream
import java.util.zip.CRC32
import kotlin.coroutines.experimental.buildSequence
import kotlin.streams.asSequence


object FileUtil {
	val LOG by logger()
}

fun path(first: String, vararg more: String) = Paths.get(first, *more)!!
fun String.toPath() = path(this)

val Path.exists: Boolean inline get() = Files.exists(this)
val Path.lastModified inline get() = Files.getLastModifiedTime(this)!!
val Path.isDirectory: Boolean inline get() = Files.isDirectory(this)
val Path.isEmpty: Boolean inline get() = if(isDirectory) directoryStream().use { dirStream -> !dirStream.iterator().hasNext() } else size == 0L
val Path.isExecutable inline get() = Files.isExecutable(this)
val Path.isHidden inline get() = Files.isHidden(this)
val Path.isReadable inline get() = Files.isReadable(this)
val Path.isRegularFile: Boolean inline get() = Files.isRegularFile(this)
val Path.isWritable inline get() = Files.isWritable(this)
val Path.size inline get() = Files.size(this)

operator fun Path.div(other: Any?) = this.resolve(other?.toString())!!
operator fun Path.div(other: String?) = this.resolve(other)!!
operator fun Path.div(other: Path?) = this.resolve(other)!!

fun Path.readText(charset: Charset = Charsets.UTF_8) = toFile().readText(charset)

val Path.attributes: BasicFileAttributes inline get() = Files.readAttributes(this, BasicFileAttributes::class.java)

fun Path.touch(): Path {
	if (exists) {
		Files.setLastModifiedTime(this, FileTime.fromMillis(System.currentTimeMillis()))
	} else {
		createFile()
	}
	return this
}

fun Path.createFile(): Path {
	if (!parent.exists) {
		parent.createDirectory()
	}
	return Files.createFile(this)
}
fun Path.createDirectory(): Path = Files.createDirectories(this)

fun Path.delete() = Files.delete(this)
fun Path.deleteIfExists() = Files.deleteIfExists(this)

fun Path.directoryStream() = Files.newDirectoryStream(this)!!
fun Path.directoryStream(glob: String) = Files.newDirectoryStream(this, glob)!!
fun Path.directoryStream(filter: (Path) -> Boolean) = Files.newDirectoryStream(this, filter)!!

fun <T> DirectoryStream<T>.useAsSequence() = sequence { use { ds -> ds.forEach { yield(it) } } }

fun Path.inputStream(vararg options: OpenOption) = Files.newInputStream(this, *options)!!

fun Path.outputStream(vararg options: OpenOption) = Files.newOutputStream(this, *options)!!

fun Path.bufferedReader(cs: Charset) = Files.newBufferedReader(this, cs)!!
fun Path.bufferedReader() = Files.newBufferedReader(this)!!

fun Path.bufferedWriter(cs: Charset, vararg options: OpenOption) = Files.newBufferedWriter(this, cs, *options)!!
fun Path.bufferedWriter(vararg options: OpenOption) = Files.newBufferedWriter(this, *options)!!

enum class FileVisitorOp {
	PRE_VISIT_DIRECTORY,
	VISIT_FILE,
	VISIT_FILE_FAILED,
	POST_VISIT_DIRECTORY,
}

fun Path.walkFileTree(visitor: FileVisitor<Path>) {
	Files.walkFileTree(this, visitor)
}

fun Path.walkFileTree(handler: (op: FileVisitorOp, p: Path, attrs: BasicFileAttributes?, exc: IOException?) -> FileVisitResult) {
	Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
		@Throws(IOException::class)
		override fun preVisitDirectory(p: Path, attrs: BasicFileAttributes)
				= handler(FileVisitorOp.PRE_VISIT_DIRECTORY, p, attrs, null)

		@Throws(IOException::class)
		override fun visitFile(p: Path, attrs: BasicFileAttributes)
				= handler(FileVisitorOp.VISIT_FILE, p, attrs, null)

		@Throws(IOException::class)
		override fun visitFileFailed(p: Path, exc: IOException?)
				= handler(FileVisitorOp.VISIT_FILE_FAILED, p, null, exc)

		@Throws(IOException::class)
		override fun postVisitDirectory(p: Path, exc: IOException?)
			= handler(FileVisitorOp.POST_VISIT_DIRECTORY, p, null, exc)
	})
}

/**
 * deletes a directory and all the files and subdirectories recursively
 * @param directory
 * @throws IOException
 */
@Throws(IOException::class)
fun Path.deleteRecursively() {
	if (exists) {
		this.walkFileTree {op, p, _, _ ->
			when(op) {
				FileVisitorOp.VISIT_FILE -> {
					FileUtil.LOG.debug { "deleting file $p" }
					p.deleteIfExists()
				}
				FileVisitorOp.POST_VISIT_DIRECTORY -> {
					FileUtil.LOG.debug { "deleting directory $p" }
					p.deleteIfExists()
				}
				else -> {}
			}
			return@walkFileTree FileVisitResult.CONTINUE
		}
	}
}

/**
 *
 * @param path
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun Path.checksumCRC32(): Long {
	return if (exists) this.inputStream().use { inputStream ->
		val crc32 = CRC32()
		val b = ByteArray(1024 * 4)
		var len = inputStream.read(b)
		while (len != -1) {
			crc32.update(b, 0, len)
			len = inputStream.read(b)
		}
		return@use crc32.value
	} else throw IOException("can't calc crc32 checksum, because file \"$this\" does not exist")
}

/**
 *
 * @param path of the jar file
 * @return true if the jar file does not exist or is empty
 */
val Path.isEmptyJarFile: Boolean
	get() {
		var result = true
		if (Files.exists(this)) {
			try {
				JarInputStream(this.inputStream()).use { inputStream ->
					if (null != inputStream.nextJarEntry) {
						result = false
					}
				}
			} catch (e: IOException) {
			}

		}
		return result
	}

/**
 *
 * @param directory
 * @param fileFilter
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun Path.doesDirectoryContainFile(fileFilter: (Path, BasicFileAttributes) -> Boolean) = FileUtil.LOG {
    try {
        Files.find(this, Integer.MAX_VALUE, BiPredicate<Path, BasicFileAttributes> { p: Path, bfa: BasicFileAttributes ->
            bfa.isRegularFile && fileFilter.invoke(p, bfa)
        })
                .use { files -> files.filter { p -> p != this }.findAny().isPresent }
    } catch (e: IOException) {
        throw FileUtil.LOG.throwing(e) // UNDONE maybe not even necessary as IOException is to be expected to be catched
    }
}

/**
 *
 * @param directory
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun Path.doesDirectoryContainFile() = this.doesDirectoryContainFile { _, _ -> true }

fun Path.list() = try {
		Files.list(this).asSequence()
	} catch (e: IOException) {
		emptySequence<Path>()
	}

operator fun FileTime?.compareTo(that: FileTime?)
		= that?.let { this?.compareTo(that) ?: -1 } ?: (if(this==null) 0 else 1)


/**
 * wasModifiedSince
 */
infix fun Path.wasModifiedSince(fileTime: FileTime): Pair<Boolean, FileTime?> {
	val lastModified = this.lastModified
	return (lastModified>fileTime) to lastModified
}

/**
 * wasDirectoryModifiedSince
 */
infix fun Path.wasDirectoryModifiedSince(fileTime: FileTime): Pair<Boolean, FileTime?> {
	var latestLastModified: FileTime? = null

	walkFileTree { op, _, attrs, _ ->
		when(op) {
			FileVisitorOp.POST_VISIT_DIRECTORY -> {
				attrs?.lastModifiedTime()?.let {
					if(latestLastModified < it) {
						latestLastModified = it
					}
				}
			}
			else -> {}
		}
		return@walkFileTree FileVisitResult.CONTINUE
	}

	return (lastModified>fileTime) to latestLastModified
}

/**
 *
 */
infix fun Path.copyTo(target: Path) {
	target.parent.createDirectory()
	Files.copy(this, target)
}

infix fun Path.copyToReplaceExisting(target: Path) {
	target.parent.createDirectory()
	Files.copy(this, target, StandardCopyOption.REPLACE_EXISTING)
}


/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.

 * @param charset character set to use. By default uses UTF-8 charset.
 * @return the value returned by [block].
 */
inline fun <T> Path.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T =
		bufferedReader(charset).use { block(it.lineSequence()) }
