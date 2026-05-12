/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils.io

import java.io.File
import java.nio.file.Path
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem

fun kotlinx.io.files.Path.createParentDirectories(
    fileSystem: FileSystem = SystemFileSystem
): kotlinx.io.files.Path =
    this.also {
      fileSystem.createDirectories(
          parent ?: throw IllegalArgumentException("$this has no parent directory")
      )
    }

fun kotlinx.io.files.Path.asSink(fileSystem: FileSystem = SystemFileSystem): Sink =
    fileSystem.sink(this).buffered()

fun Path.toKotlinxPath(): kotlinx.io.files.Path = kotlinx.io.files.Path(toString())

fun File.toKotlinxPath(): kotlinx.io.files.Path = kotlinx.io.files.Path(toString())

fun Path.asSink(fileSystem: FileSystem = SystemFileSystem): Sink =
    fileSystem.sink(toKotlinxPath()).buffered()
