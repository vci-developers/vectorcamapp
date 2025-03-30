package com.vci.vectorcamapp.imaging.presentation

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenDetector
import com.vci.vectorcamapp.imaging.presentation.components.CameraPreview
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun ImagingScreen(
    state: ImagingState, onAction: (ImagingAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analyzer = remember {
        SpecimenImageAnalyzer(detector = TfLiteSpecimenDetector(
            context = context
        ),
            onDetectionUpdated = { onAction(ImagingAction.UpdateDetection(it)) },
            onSpecimenIdUpdated = { onAction(ImagingAction.UpdateSpecimenId(it)) })
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
            )
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context), analyzer
            )
            imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
    ) {
        if (state.currentImage != null) {
            Image(
                bitmap = state.currentImage.asImageBitmap(),
                contentDescription = "Specimen Image",
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {

            CameraPreview(controller = controller, modifier = modifier.fillMaxSize())

            if (state.detection != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = if (state.detection.confidence > 0.8) Color.Green else Color.Red,
                        topLeft = Offset(state.detection.topLeftX, state.detection.topLeftY),
                        size = Size(
                            width = state.detection.width, height = state.detection.height
                        ),
                        style = Stroke(width = 4f)
                    )
                }
            }

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        controller.takePicture(ContextCompat.getMainExecutor(context),
                            object : OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    super.onCaptureSuccess(image)

                                    val rotatedBitmap = image.toUprightBitmap()

                                    onAction(
                                        ImagingAction.CaptureComplete(
                                            Result.Success(rotatedBitmap)
                                        )
                                    )
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    super.onError(exception)

                                    exception.message?.let { Log.e("ERROR", it) }
                                    onAction(
                                        ImagingAction.CaptureComplete(
                                            Result.Error(ImagingError.CANNOT_CAPTURE)
                                        )
                                    )
                                }
                            })
                    }, modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primary, shape = CircleShape
                        )
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
    }
}

@PreviewLightDark
@Composable
fun ImagingScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ImagingScreen(
                state = ImagingState(), onAction = { }, modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
