package com.vci.vectorcamapp.imaging.data.util

import com.vci.vectorcamapp.imaging.domain.cache.FocusWarpCache
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2
import kotlin.math.min

@Singleton
class FocusStackFusion @Inject constructor() {

    data class FocusStackFusionOutput(
        val jpeg: ByteArray,
        val width: Int,
        val height: Int,
    )

    fun fuse(
        colorMats: List<Mat>,
        warpCache: FocusWarpCache? = null,
    ): FocusStackFusionOutput {
        require(colorMats.isNotEmpty()) { "At least one frame required" }
        val referenceIndex = min(REFERENCE_FRAME_INDEX, colorMats.size - 1)
        val originalSize = colorMats[0].size()

        val referenceGrayFull = Mat()
        Imgproc.cvtColor(colorMats[referenceIndex], referenceGrayFull, Imgproc.COLOR_BGR2GRAY)
        val referenceGrayEcc = downscaleForEcc(referenceGrayFull)
        referenceGrayFull.release()

        val alignedColorMats = colorMats.mapIndexed { index, colorMat ->
            if (index == referenceIndex) return@mapIndexed colorMat.clone()
            val cachedWarp = warpCache?.get(index)
            if (cachedWarp != null) {
                applyWarp(colorMat, cachedWarp)
            } else {
                val (aligned, computedWarp) = alignEcc(colorMat, referenceGrayEcc)
                if (computedWarp != null) warpCache?.put(index, computedWarp)
                aligned
            }
        }
        referenceGrayEcc.release()
        colorMats.forEach { it.release() }

        val downscaledMats = alignedColorMats.map { mat ->
            val down = Mat()
            Imgproc.pyrDown(mat, down)
            down
        }
        alignedColorMats.forEach { it.release() }

        val downscaledComposite = fuseLaplacianPyramid(downscaledMats)
        downscaledMats.forEach { it.release() }

        val composite = Mat()
        Imgproc.pyrUp(downscaledComposite, composite, originalSize)
        downscaledComposite.release()

        val compositeBgr = Mat()
        composite.convertTo(compositeBgr, CvType.CV_8UC3, 255.0)
        composite.release()

        val jpegOut = MatOfByte()
        val jpegParams = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, COMPOSITE_JPEG_QUALITY)
        Imgcodecs.imencode(".jpg", compositeBgr, jpegOut, jpegParams)
        val jpegBytes = jpegOut.toArray()
        val width = compositeBgr.cols()
        val height = compositeBgr.rows()
        compositeBgr.release()
        jpegOut.release()
        jpegParams.release()

        return FocusStackFusionOutput(jpegBytes, width, height)
    }

    private fun alignEcc(sourceColor: Mat, referenceGrayEcc: Mat): Pair<Mat, FloatArray?> {
        val sourceGrayFull = Mat()
        Imgproc.cvtColor(sourceColor, sourceGrayFull, Imgproc.COLOR_BGR2GRAY)
        val sourceGrayEcc = downscaleForEcc(sourceGrayFull)
        sourceGrayFull.release()

        val warp = Mat.eye(2, 3, CvType.CV_32F)
        val criteria = TermCriteria(
            TermCriteria.COUNT or TermCriteria.EPS,
            ECC_MAX_ITERATIONS,
            ECC_EPSILON,
        )

        val aligned = Mat()
        var computedWarp: FloatArray? = null
        try {
            Video.findTransformECC(
                referenceGrayEcc,
                sourceGrayEcc,
                warp,
                Video.MOTION_AFFINE,
                criteria,
                Mat(),
                ECC_GAUSS_FILTER_SIZE,
            )
            scaleWarpTranslation(warp, ECC_DOWNSAMPLE_FACTOR)
            Imgproc.warpAffine(
                sourceColor,
                aligned,
                warp,
                sourceColor.size(),
                Imgproc.INTER_LINEAR or Imgproc.WARP_INVERSE_MAP,
            )
            computedWarp = FloatArray(6).also { warp.get(0, 0, it) }
        } catch (e: Exception) {
            sourceColor.copyTo(aligned)
        }
        sourceGrayEcc.release()
        warp.release()
        return aligned to computedWarp
    }

    private fun applyWarp(sourceColor: Mat, warpData: FloatArray): Mat {
        val warp = Mat(2, 3, CvType.CV_32F)
        warp.put(0, 0, warpData)
        val aligned = Mat()
        Imgproc.warpAffine(
            sourceColor,
            aligned,
            warp,
            sourceColor.size(),
            Imgproc.INTER_LINEAR or Imgproc.WARP_INVERSE_MAP,
        )
        warp.release()
        return aligned
    }

    private fun downscaleForEcc(sourceGray: Mat): Mat {
        var current: Mat = sourceGray.clone()
        repeat(ECC_DOWNSAMPLE_LEVELS) {
            val next = Mat()
            Imgproc.pyrDown(current, next)
            current.release()
            current = next
        }
        val normalized = Mat()
        current.convertTo(normalized, CvType.CV_32F, 1.0 / 255.0)
        current.release()
        return normalized
    }

    private fun scaleWarpTranslation(warp: Mat, scale: Float) {
        val data = FloatArray(6)
        warp.get(0, 0, data)
        data[2] *= scale
        data[5] *= scale
        warp.put(0, 0, data)
    }

    private fun fuseLaplacianPyramid(alignedColorMats: List<Mat>): Mat {
        val minDimension = min(alignedColorMats[0].rows(), alignedColorMats[0].cols())
        val depthFromSize = (log2(minDimension.toDouble()) - log2(PYRAMID_MIN_DIMENSION.toDouble()))
            .toInt()
            .coerceAtLeast(1)
        val levels = min(MAX_PYRAMID_LEVELS, depthFromSize)

        val laplacianPyramids = alignedColorMats.map { colorMat ->
            val floatMat = Mat()
            colorMat.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 255.0)
            val gaussian = buildGaussianPyramid(floatMat, levels)
            floatMat.release()
            val laplacian = buildLaplacianPyramid(gaussian)
            gaussian.forEach { it.release() }
            laplacian
        }

        val fusedPyramid = mutableListOf<Mat>()
        for (level in 0..levels) {
            val perFrameLevels = laplacianPyramids.map { it[level] }
            fusedPyramid.add(fuseLevel(perFrameLevels))
        }
        laplacianPyramids.forEach { pyramid -> pyramid.forEach { it.release() } }

        val collapsed = collapsePyramid(fusedPyramid)
        fusedPyramid.forEach { it.release() }
        return collapsed
    }

    private fun buildGaussianPyramid(base: Mat, levels: Int): List<Mat> {
        val pyramid = mutableListOf<Mat>()
        pyramid.add(base.clone())
        for (level in 1..levels) {
            val down = Mat()
            Imgproc.pyrDown(pyramid.last(), down)
            pyramid.add(down)
        }
        return pyramid
    }

    private fun buildLaplacianPyramid(gaussian: List<Mat>): List<Mat> {
        val pyramid = mutableListOf<Mat>()
        for (level in 0 until gaussian.size - 1) {
            val expanded = Mat()
            Imgproc.pyrUp(gaussian[level + 1], expanded, gaussian[level].size())
            val laplacian = Mat()
            Core.subtract(gaussian[level], expanded, laplacian)
            expanded.release()
            pyramid.add(laplacian)
        }
        pyramid.add(gaussian.last().clone())
        return pyramid
    }

    private fun fuseLevel(perFrameLevels: List<Mat>): Mat {
        val size = perFrameLevels[0].size()
        val type = perFrameLevels[0].type()

        val sharpnessMaps = perFrameLevels.map { computeSharpnessMap(it) }
        val sharpnessSum = Mat.zeros(size, CvType.CV_32F)
        sharpnessMaps.forEach { Core.add(sharpnessSum, it, sharpnessSum) }
        Core.add(sharpnessSum, Scalar.all(SHARPNESS_EPSILON), sharpnessSum)

        val fused = Mat.zeros(size, type)
        for (index in perFrameLevels.indices) {
            val weight = Mat()
            Core.divide(sharpnessMaps[index], sharpnessSum, weight)
            val weightThreeChannel = Mat()
            Core.merge(listOf(weight, weight, weight), weightThreeChannel)
            val weighted = Mat()
            Core.multiply(perFrameLevels[index], weightThreeChannel, weighted)
            Core.add(fused, weighted, fused)
            weight.release()
            weightThreeChannel.release()
            weighted.release()
        }
        sharpnessMaps.forEach { it.release() }
        sharpnessSum.release()
        return fused
    }

    private fun computeSharpnessMap(colorLevel: Mat): Mat {
        val channels = mutableListOf<Mat>()
        Core.split(colorLevel, channels)
        val summed = Mat.zeros(colorLevel.size(), CvType.CV_32F)
        for (channel in channels) {
            val absChannel = Mat()
            Core.absdiff(channel, Scalar.all(0.0), absChannel)
            Core.add(summed, absChannel, summed)
            absChannel.release()
            channel.release()
        }
        val blurred = Mat()
        Imgproc.blur(
            summed,
            blurred,
            Size(SHARPNESS_BLUR_KERNEL_SIZE, SHARPNESS_BLUR_KERNEL_SIZE),
        )
        summed.release()
        return blurred
    }

    private fun collapsePyramid(fusedPyramid: List<Mat>): Mat {
        var current = fusedPyramid.last().clone()
        for (level in fusedPyramid.size - 2 downTo 0) {
            val expanded = Mat()
            Imgproc.pyrUp(current, expanded, fusedPyramid[level].size())
            current.release()
            val added = Mat()
            Core.add(expanded, fusedPyramid[level], added)
            expanded.release()
            current = added
        }
        return current
    }

    companion object {
        private const val REFERENCE_FRAME_INDEX = 1
        private const val MAX_PYRAMID_LEVELS = 6
        private const val PYRAMID_MIN_DIMENSION = 16
        private const val ECC_MAX_ITERATIONS = 50
        private const val ECC_EPSILON = 1e-4
        private const val ECC_GAUSS_FILTER_SIZE = 5
        private const val ECC_DOWNSAMPLE_LEVELS = 3
        private const val ECC_DOWNSAMPLE_FACTOR = 8f
        private const val SHARPNESS_BLUR_KERNEL_SIZE = 5.0
        private const val SHARPNESS_EPSILON = 1e-6
        private const val COMPOSITE_JPEG_QUALITY = 100
    }
}
