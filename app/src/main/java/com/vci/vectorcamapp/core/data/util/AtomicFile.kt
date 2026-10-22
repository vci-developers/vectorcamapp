package com.vci.vectorcamapp.core.data.util

import com.vci.vectorcamapp.core.logging.ProgramModelLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object AtomicFile {
    fun writeText(target: File, text: String) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(temp).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        if (!replace(temp, target)) {
            temp.delete()
            throw IOException("Failed to replace ${target.name}")
        }
    }

    fun sync(file: File) {
        FileOutputStream(file, true).use { output -> output.fd.sync() }
    }

    /**
     * Publishes [source] as [target] without deleting [target] first. A failed swap leaves the
     * previous file in place.
     */
    fun replace(source: File, target: File): Boolean {
        target.parentFile?.mkdirs()
        return try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
            true
        } catch (e: IOException) {
            ProgramModelLog.w(
                "Atomic replace failed for %s (%s); restoring via backup",
                target.name,
                e.message ?: e.javaClass.simpleName,
            )
            replaceWithBackup(source, target)
        }
    }

    private fun replaceWithBackup(source: File, target: File): Boolean {
        val backup = File(target.parentFile, "${target.name}.bak")
        if (backup.exists() && !backup.delete()) {
            ProgramModelLog.e("Could not remove backup %s", backup.absolutePath)
            return false
        }
        val hadTarget = target.exists()
        if (hadTarget && !target.renameTo(backup)) {
            ProgramModelLog.e("Could not move current file aside %s", target.absolutePath)
            return false
        }
        if (source.renameTo(target)) {
            if (hadTarget) backup.delete()
            return true
        }
        ProgramModelLog.e("Could not move %s into place; restoring previous file", source.absolutePath)
        if (hadTarget) backup.renameTo(target)
        return false
    }
}
