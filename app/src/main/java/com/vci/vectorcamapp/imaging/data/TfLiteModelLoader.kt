package com.vci.vectorcamapp.imaging.data

import android.content.Context
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

internal object TfLiteModelLoader {
    fun load(context: Context, assetOrAbsolutePath: String): MappedByteBuffer {
        val file = File(assetOrAbsolutePath)
        return if (file.isAbsolute && file.exists()) {
            FileInputStream(file).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
        } else {
            FileUtil.loadMappedFile(context, assetOrAbsolutePath)
        }
    }
}
