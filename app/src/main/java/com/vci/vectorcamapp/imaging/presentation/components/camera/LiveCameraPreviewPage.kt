package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

@Composable
fun LiveCameraPreviewPage(
    controller: LifecycleCameraController,
    boundingBoxesUiList: List<BoundingBoxUi>,
    onImageCaptured: () -> Unit,
    onSaveSessionProgress: () -> Unit,
    onSubmitSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        previewView.apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            this.controller = controller
        }
        controller.bindToLifecycle(lifecycleOwner)
    }

    LaunchedEffect(boundingBoxesUiList) {
        if (boundingBoxesUiList.isNotEmpty()) {
            val box = boundingBoxesUiList.first()
            val centerX = box.topLeftX + box.width / 2f
            val centerY = box.topLeftY + box.height / 2f
            val point = previewView.meteringPointFactory.createPoint(centerX, centerY)

            val action = FocusMeteringAction
                .Builder(point, FocusMeteringAction.FLAG_AF)
                .disableAutoCancel()
                .build()

            controller.cameraControl?.startFocusAndMetering(action)
        } else {
            controller.cameraControl?.cancelFocusAndMetering()
        }
    }


    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        boundingBoxesUiList.map {
            BoundingBoxOverlay(it, Modifier.fillMaxSize())
        }

        IconButton(
            onClick = onSaveSessionProgress,
            modifier = Modifier
                .padding(24.dp)
                .size(64.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_save),
                contentDescription = "Save Session Progress",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onTertiary
            )
        }

        IconButton(
            onClick = onSubmitSession,
            modifier = Modifier
                .padding(24.dp)
                .size(64.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_upload),
                contentDescription = "Submit Session",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onTertiary
            )
        }

        IconButton(
            onClick = onImageCaptured,
            modifier = Modifier
                .padding(bottom = 48.dp)
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .align(Alignment.BottomCenter)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = "Capture Image",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
