package com.vci.vectorcamapp.imaging.presentation

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.imaging.presentation.components.camera.BoundingBoxOverlay
import com.vci.vectorcamapp.imaging.presentation.components.camera.CameraPreview
import com.vci.vectorcamapp.imaging.presentation.components.specimeninfocard.SpecimenInfoCard
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun ImagingScreen(
    state: ImagingState, onAction: (ImagingAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val analyzer = remember {
        SpecimenImageAnalyzer { frame ->
            onAction(ImagingAction.ProcessFrame(frame))
        }
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
                    contentScale = ContentScale.FillHeight
                )

                state.currentBoundingBoxUi?.let {
                    BoundingBoxOverlay(it, modifier = modifier)
                }

                IconButton(
                    onClick = { onAction(ImagingAction.RetakeImage) },
                    modifier = modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = "Retake Image",
                        tint = Color.White
                    )
                }

                SpecimenInfoCard(
                    specimenId = state.currentSpecimenId,
                    species = state.currentSpecies,
                    sex = state.currentSex,
                    abdomenStatus = state.currentAbdomenStatus,
                    onSpecimenIdCorrected = { onAction(ImagingAction.CorrectSpecimenId(it)) },
                    modifier = modifier.align(Alignment.BottomCenter),
                )
            }
        } else {

            CameraPreview(controller = controller, modifier = modifier.fillMaxSize())

            state.currentBoundingBoxUi?.let {
                BoundingBoxOverlay(it, modifier = modifier)
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
                                override fun onCaptureStarted() {
                                    super.onCaptureStarted()

                                    onAction(ImagingAction.CaptureStart)
                                }

                                override fun onCaptureSuccess(image: ImageProxy) {
                                    super.onCaptureSuccess(image)

                                    onAction(
                                        ImagingAction.CaptureComplete(
                                            Result.Success(image)
                                        )
                                    )
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    super.onError(exception)

                                    exception.message?.let { Log.e("ERROR", it) }
                                    onAction(
                                        ImagingAction.CaptureComplete(
                                            Result.Error(ImagingError.CAPTURE_ERROR)
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
