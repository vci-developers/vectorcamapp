package com.vci.vectorcamapp.imaging.data.util

import android.content.Context
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.LiteRtException
import com.google.ai.edge.litert.TensorBuffer
import com.vci.vectorcamapp.imaging.data.TfLiteModelLoader
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Chooses the accelerator for one classifier asset, preferring GPU but only where GPU demonstrably
 * agrees with CPU.
 *
 * LiteRT's GPU accelerator compiles the NCHW species/sex/abdomen models without error and returns
 * logits that do vary with the input, yet predicts the wrong class - so neither a successful
 * `CompiledModel.create` nor a plausibility check on the output is enough to trust it. Which GPU
 * configuration works is driver-dependent (precision, buffer storage, backend all matter), so each
 * candidate is scored against the CPU result for an identical input and the first one that agrees
 * wins. The verdict is cached per asset, so the sweep costs one extra model build per asset per
 * install rather than one per imaging session.
 */
object ClassifierAcceleratorSelector {

    data class Selection(
        val model: CompiledModel,
        val usingGpu: Boolean,
        val variantName: String,
    )

    private const val PREFS_NAME = "classifier_accelerator"
    private const val CPU_VARIANT = "cpu"

    // GPU math legitimately differs from CPU in the last digits; a wrong prediction differs by far
    // more than this, so compare with a tolerance scaled to the reference spread.
    private const val RELATIVE_TOLERANCE = 0.05f
    private const val ABSOLUTE_TOLERANCE = 1e-3f
    private const val MIN_REFERENCE_SPREAD = 1e-6f

    // A fixed seed keeps the probe input identical across accelerators, models and app runs.
    private const val PROBE_SEED = 20260823
    private const val PROBE_SPAN = 4f
    private const val PROBE_OFFSET = 2f

    // A sweep holds two models at once, so keep the three classifiers from sweeping together.
    private val sweepLock = Any()

    private class Variant(val name: String, val createOptions: () -> CompiledModel.Options)

    private val gpuVariants: List<Variant> = listOf(
        Variant("gpu-default") { CompiledModel.Options(Accelerator.GPU) },
        Variant("gpu-fp32") {
            gpuAcceleratorOptions(precision = CompiledModel.GpuOptions.Precision.FP32)
        },
        Variant("gpu-fp32-buffer") {
            gpuAcceleratorOptions(
                precision = CompiledModel.GpuOptions.Precision.FP32,
                bufferStorageType = CompiledModel.GpuOptions.BufferStorageType.BUFFER,
            )
        },
        Variant("gpu-fp32-texture2d") {
            gpuAcceleratorOptions(
                precision = CompiledModel.GpuOptions.Precision.FP32,
                bufferStorageType = CompiledModel.GpuOptions.BufferStorageType.TEXTURE_2D,
            )
        },
        Variant("gpu-opencl-fp32") {
            gpuAcceleratorOptions(
                precision = CompiledModel.GpuOptions.Precision.FP32,
                backend = CompiledModel.GpuOptions.Backend.OPENCL,
            )
        },
        Variant("gpu-opengl-fp32") {
            gpuAcceleratorOptions(
                precision = CompiledModel.GpuOptions.Precision.FP32,
                backend = CompiledModel.GpuOptions.Backend.OPENGL,
            )
        },
    )

    fun selectModel(
        context: Context,
        assetName: String,
        signature: String,
        inputTensorName: String,
    ): Selection {
        val cachedVariant = prefs(context).getString(assetName, null)
        if (cachedVariant != null) {
            return buildCachedVariant(context, assetName, cachedVariant)
        }
        return synchronized(sweepLock) {
            sweepVariants(context, assetName, signature, inputTensorName)
        }
    }

    private fun buildCachedVariant(
        context: Context,
        assetName: String,
        variantName: String,
    ): Selection {
        val variant = gpuVariants.firstOrNull { it.name == variantName }
            ?: return Selection(createCpuModel(context, assetName), false, CPU_VARIANT)

        return try {
            Selection(
                TfLiteModelLoader.create(context, assetName, variant.createOptions()),
                usingGpu = true,
                variantName = variant.name,
            )
        } catch (e: LiteRtException) {
            Timber.w("Cached variant ${variant.name} no longer builds for $assetName: ${e.message}")
            Selection(createCpuModel(context, assetName), false, CPU_VARIANT)
        }
    }

    private fun sweepVariants(
        context: Context,
        assetName: String,
        signature: String,
        inputTensorName: String,
    ): Selection {
        val cpuModel = createCpuModel(context, assetName)
        val probe = createProbeInput(cpuModel, signature, inputTensorName)
        val reference = probe?.let { runOnce(cpuModel, it) }

        if (reference == null || !reference.hasSignal()) {
            Timber.w("No usable CPU reference for $assetName; keeping CPU without sweeping GPU")
            return Selection(cpuModel, false, CPU_VARIANT)
        }

        for (variant in gpuVariants) {
            val candidate = createGpuModel(context, assetName, variant) ?: continue
            val logits = runOnce(candidate, probe)

            if (logits != null && agreesWith(reference, logits)) {
                Timber.i("GPU variant ${variant.name} matches CPU for $assetName; using GPU")
                cpuModel.close()
                persistVariant(context, assetName, variant.name)
                return Selection(candidate, usingGpu = true, variantName = variant.name)
            }

            Timber.w(
                "GPU variant ${variant.name} disagrees with CPU for $assetName " +
                    "(cpu=${reference.toList()}, gpu=${logits?.toList()})"
            )
            candidate.close()
        }

        Timber.w("No GPU variant matched CPU for $assetName; using CPU")
        persistVariant(context, assetName, CPU_VARIANT)
        return Selection(cpuModel, false, CPU_VARIANT)
    }

    private fun createGpuModel(
        context: Context,
        assetName: String,
        variant: Variant,
    ): CompiledModel? = try {
        TfLiteModelLoader.create(context, assetName, variant.createOptions())
    } catch (e: LiteRtException) {
        Timber.w("GPU variant ${variant.name} failed to build for $assetName: ${e.message}")
        null
    }

    private fun createCpuModel(context: Context, assetName: String): CompiledModel {
        // LiteRT's CPU accelerator does not thread across all cores by default, which makes these
        // classifiers noticeably slower than the TFLite Interpreter setup they replaced.
        val options = CompiledModel.Options(Accelerator.CPU).apply {
            cpuOptions = CompiledModel.CpuOptions(
                numThreads = Runtime.getRuntime().availableProcessors(),
            )
        }
        return TfLiteModelLoader.create(context, assetName, options)
    }

    private fun createProbeInput(
        model: CompiledModel,
        signature: String,
        inputTensorName: String,
    ): FloatArray? = try {
        val floatCount =
            model.getInputBufferRequirements(inputTensorName, signature).bufferSize /
                Float.SIZE_BYTES
        // Values spanning the range real normalized pixels land in; a uniform fill can't tell a
        // correct accelerator apart from one that mangles the input layout.
        val random = Random(PROBE_SEED)
        FloatArray(floatCount) { random.nextFloat() * PROBE_SPAN - PROBE_OFFSET }
    } catch (e: LiteRtException) {
        Timber.w("Could not size probe input: ${e.message}")
        null
    }

    private fun runOnce(model: CompiledModel, input: FloatArray): FloatArray? {
        var inputBuffers: List<TensorBuffer> = emptyList()
        var outputBuffers: List<TensorBuffer> = emptyList()
        return try {
            inputBuffers = model.createInputBuffers()
            outputBuffers = model.createOutputBuffers()
            inputBuffers[0].writeFloat(input)
            model.run(inputBuffers, outputBuffers)
            outputBuffers[0].readFloat()
        } catch (e: LiteRtException) {
            Timber.w("Probe inference failed: ${e.message}")
            null
        } finally {
            (inputBuffers + outputBuffers).forEach { it.close() }
        }
    }

    private fun agreesWith(reference: FloatArray, candidate: FloatArray): Boolean {
        if (reference.size != candidate.size) return false
        if (candidate.any { !it.isFinite() }) return false
        if (reference.indexOfMax() != candidate.indexOfMax()) return false

        val spread = reference.max() - reference.min()
        val tolerance = max(spread * RELATIVE_TOLERANCE, ABSOLUTE_TOLERANCE)
        return reference.indices.all { abs(reference[it] - candidate[it]) <= tolerance }
    }

    private fun FloatArray.hasSignal(): Boolean {
        if (isEmpty() || any { !it.isFinite() }) return false
        return (max() - min()) > MIN_REFERENCE_SPREAD
    }

    private fun FloatArray.indexOfMax(): Int =
        indices.maxByOrNull { this[it] } ?: -1

    private fun persistVariant(context: Context, assetName: String, variantName: String) {
        prefs(context).edit().putString(assetName, variantName).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun gpuAcceleratorOptions(
        precision: CompiledModel.GpuOptions.Precision =
            CompiledModel.GpuOptions.Precision.DEFAULT,
        bufferStorageType: CompiledModel.GpuOptions.BufferStorageType =
            CompiledModel.GpuOptions.BufferStorageType.DEFAULT,
        backend: CompiledModel.GpuOptions.Backend =
            CompiledModel.GpuOptions.Backend.AUTOMATIC,
    ) = CompiledModel.Options(Accelerator.GPU).apply {
        gpuOptions = CompiledModel.GpuOptions(
            precision = precision,
            bufferStorageType = bufferStorageType,
            backend = backend,
        )
    }
}
