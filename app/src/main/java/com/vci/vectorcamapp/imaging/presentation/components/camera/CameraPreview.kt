package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(
    controller: LifecycleCameraController, modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                this.controller = controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        onRelease = {
            controller.unbind()
        },
        modifier = modifier
    )
}
