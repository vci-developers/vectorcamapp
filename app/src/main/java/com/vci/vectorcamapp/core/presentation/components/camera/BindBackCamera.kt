package com.vci.vectorcamapp.core.presentation.components.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun defaultCameraResolutionSelector(): ResolutionSelector {
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()
}

suspend fun bindBackCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    rotation: Int,
    onSurfaceRequest: (SurfaceRequest) -> Unit,
    additionalUseCases: List<UseCase> = emptyList(),
): Camera {
    val provider = withContext(Dispatchers.IO) {
        ProcessCameraProvider.getInstance(context).get()
    }
    val previewUseCase = Preview.Builder()
        .setTargetRotation(rotation)
        .setResolutionSelector(defaultCameraResolutionSelector())
        .build()
        .apply {
            setSurfaceProvider(onSurfaceRequest)
        }
    val useCaseGroupBuilder = UseCaseGroup.Builder().addUseCase(previewUseCase)
    additionalUseCases.forEach { useCase ->
        useCaseGroupBuilder.addUseCase(useCase)
    }

    provider.unbindAll()
    return provider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        useCaseGroupBuilder.build(),
    )
}

suspend fun unbindCamera(context: Context) {
    val provider = withContext(Dispatchers.IO) {
        ProcessCameraProvider.getInstance(context).get()
    }
    provider.unbindAll()
}
