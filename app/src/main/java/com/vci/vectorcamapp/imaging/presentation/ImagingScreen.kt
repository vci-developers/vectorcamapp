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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.vci.vectorcamapp.imaging.data.GpuDelegateManager
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenDetector
import com.vci.vectorcamapp.imaging.presentation.components.CameraPreview
import com.vci.vectorcamapp.imaging.presentation.extensions.cropToBoundingBoxAndPad
import com.vci.vectorcamapp.imaging.presentation.extensions.resizeTo
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImagingScreen(
    state: ImagingState, onAction: (ImagingAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val captureScope = rememberCoroutineScope()
    val detector = remember {
        TfLiteSpecimenDetector(context = context)
    }
    val analyzer = remember(detector) {
        SpecimenImageAnalyzer(detector = detector,
            onBoundingBoxUiUpdated = { onAction(ImagingAction.UpdateBoundingBoxUi(it)) },
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
            GpuDelegateManager.close()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
    ) {
        if (state.currentImage != null) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    bitmap = state.currentImage.asImageBitmap(),
                    contentDescription = "Specimen Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { onAction(ImagingAction.RetakeImage) },

                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.error, shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = "Retake Image",
                        tint = Color.White
                    )
                }
            }
        } else {

            CameraPreview(controller = controller, modifier = modifier.fillMaxSize())

            if (state.currentBoundingBoxUi != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = if (state.currentBoundingBoxUi.confidence > 0.8) Color.Green else Color.Red,
                        topLeft = Offset(
                            state.currentBoundingBoxUi.topLeftX, state.currentBoundingBoxUi.topLeftY
                        ),
                        size = Size(
                            width = state.currentBoundingBoxUi.width,
                            height = state.currentBoundingBoxUi.height
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

                                    captureScope.launch {
                                        val tensorWidth = detector.getInputTensorShape().first
                                        val tensorHeight = detector.getInputTensorShape().second

                                        val bitmap = image.toUprightBitmap()

                                        val boundingBox = detector.detect(
                                            bitmap.resizeTo(
                                                tensorWidth, tensorHeight
                                            )
                                        )

                                        val croppedAndPaddedBitmap =
                                            boundingBox?.let { bitmap.cropToBoundingBoxAndPad(it) }

                                        withContext(Dispatchers.Main) {
                                            onAction(
                                                ImagingAction.CaptureComplete(
                                                    Result.Success(croppedAndPaddedBitmap ?: bitmap)
                                                )
                                            )
                                        }
                                    }
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
