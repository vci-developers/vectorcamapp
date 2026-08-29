package com.vci.vectorcamapp.imaging.data

import android.content.Context
import com.google.ai.edge.litert.CompiledModel
import java.io.File

internal object TfLiteModelLoader {
    fun create(
        context: Context,
        assetOrAbsolutePath: String,
        options: CompiledModel.Options,
    ): CompiledModel {
        val file = File(assetOrAbsolutePath)
        return if (file.isAbsolute && file.exists()) {
            CompiledModel.create(file.absolutePath, options)
        } else {
            CompiledModel.create(context.assets, assetOrAbsolutePath, options)
        }
    }
}
