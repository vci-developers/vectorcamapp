package com.vci.vectorcamapp.imaging.data.util

import android.content.Context
import android.content.pm.PackageManager
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.vci.vectorcamapp.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Backs LiteRT's GPU program cache so shader compilation is paid once per install rather than once
 * per [CompiledModel] build.
 *
 * Building a GPU model is dominated by compiling the accelerator's shaders - over a second per
 * model even on fast hardware - and that work is repeated every time the model is rebuilt. LiteRT
 * can serialize the compiled program and reuse it, keyed by
 * [CompiledModel.GpuOptions.modelCacheKey] under [CompiledModel.GpuOptions.serializationDir].
 *
 * A serialized program is only valid for the exact model bytes and runtime that produced it. Rather
 * than encoding model and library versions into the key by hand - which has to be remembered on
 * every model release, and fails silently when it isn't - the whole cache lives in a directory
 * named for the current install. Any app update, dependency bump or reinstall changes the
 * directory name, so a program built for different bytes is never read; it is missed and
 * recompiled. Superseded directories are deleted the first time the cache is used.
 */
object GpuModelCache {

    private const val ROOT_DIR_NAME = "litert_gpu"

    // Serializing the transformed weight tensors as well would cut the build further, but costs
    // roughly the size of the model assets on disk (~92MB). Shader compilation is the dominant
    // term, so start with the program cache alone and only revisit this if the build durations
    // logged by the classifiers and detector say it isn't enough.
    private const val SERIALIZE_EXTERNAL_TENSORS = false

    private val lock = Any()
    private var directory: File? = null
    private var directoryResolved = false

    /**
     * GPU options for [cacheKey], serializing the compiled program unless [serialize] is false.
     *
     * [cacheKey] must identify the model bytes *and* the accelerator configuration, since the same
     * asset compiled for a different backend or precision produces a different program.
     */
    fun options(
        context: Context,
        cacheKey: String,
        precision: CompiledModel.GpuOptions.Precision =
            CompiledModel.GpuOptions.Precision.DEFAULT,
        bufferStorageType: CompiledModel.GpuOptions.BufferStorageType =
            CompiledModel.GpuOptions.BufferStorageType.DEFAULT,
        backend: CompiledModel.GpuOptions.Backend =
            CompiledModel.GpuOptions.Backend.AUTOMATIC,
        serialize: Boolean = true,
    ): CompiledModel.Options {
        val serializationDir = if (serialize) directory(context) else null
        val fingerprint = assetFingerprint(context, cacheKey.substringBefore('#'))
        val programKey = GpuCacheIdentity.programCacheKey(cacheKey, fingerprint)

        return CompiledModel.Options(Accelerator.GPU).apply {
            gpuOptions = CompiledModel.GpuOptions(
                precision = precision,
                bufferStorageType = bufferStorageType,
                backend = backend,
                serializationDir = serializationDir?.absolutePath,
                modelCacheKey = serializationDir?.let { programKey },
                serializeProgramCache = serializationDir?.let { true },
                serializeExternalTensors = serializationDir?.let { SERIALIZE_EXTERNAL_TENSORS },
            )
        }
    }

    /**
     * Identifier for the current install, or null when it cannot be determined. Anything derived
     * from the app's own binaries - a serialized GPU program, a recorded accelerator verdict - is
     * only valid for one value of this.
     */
    fun installToken(context: Context): String? = try {
        val lastUpdateTime = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .lastUpdateTime
        GpuCacheIdentity.installToken(BuildConfig.VERSION_CODE, lastUpdateTime)
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.w(e, "Could not read install time; treating derived caches as unavailable")
        null
    }

    /**
     * Cheap identity for an asset: uncompressed length. `.tflite` files are stored uncompressed,
     * so this does not require reading the model. Two different models with the exact same length
     * would collide; the install-token directory still separates app updates.
     */
    fun assetFingerprint(context: Context, assetName: String): String? = try {
        context.assets.openFd(assetName).use { fd -> fd.length.toString() }
    } catch (e: IOException) {
        Timber.w(e, "Could not fingerprint $assetName; GPU program key will omit it")
        null
    }

    private fun directory(context: Context): File? = synchronized(lock) {
        if (!directoryResolved) {
            directoryResolved = true
            directory = prepareDirectory(context)
        }
        directory
    }

    private fun prepareDirectory(context: Context): File? {
        // Without a token there is no way to tell a stale program from a current one, and reading a
        // stale one is silently wrong rather than a crash, so go without the cache instead.
        val token = installToken(context) ?: return null

        // codeCacheDir rather than cacheDir: the platform already clears it on app and OS upgrade,
        // which is a second line of defence behind the token.
        val root = File(context.codeCacheDir, ROOT_DIR_NAME)
        val current = File(root, token)

        return try {
            if (!current.mkdirs() && !current.isDirectory) {
                Timber.w("Could not create GPU program cache at ${current.absolutePath}")
                return null
            }

            root.listFiles()
                ?.filter { it.name != token }
                ?.forEach { superseded ->
                    if (superseded.deleteRecursively()) {
                        Timber.d("Removed superseded GPU program cache ${superseded.name}")
                    }
                }

            Timber.d("GPU program cache at ${current.absolutePath}")
            current
        } catch (e: SecurityException) {
            Timber.w(e, "GPU program cache unavailable: ${e.message}")
            null
        }
    }
}
