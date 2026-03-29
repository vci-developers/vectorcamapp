package com.vci.vectorcamapp.imaging.presentation

import android.view.Surface
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.empty.EmptySpace
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.imaging.data.camera.CameraMetadataListenerImplementation
import com.vci.vectorcamapp.imaging.presentation.components.camera.LiveCameraPreview
import com.vci.vectorcamapp.imaging.presentation.components.icon.AnimatedArrowIcon
import com.vci.vectorcamapp.imaging.presentation.components.specimen.CapturedSpecimenTile
import com.vci.vectorcamapp.imaging.presentation.components.specimen.SpecimenImageOverlay
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun ImagingScreen(
    state: ImagingState, onAction: (ImagingAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()

    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val metadataListener = remember { CameraMetadataListenerImplementation() }

    val isReviewing by rememberUpdatedState(newValue = state.currentImageBytes != null)
    val analyzer = remember {
        SpecimenImageAnalyzer { frame ->
            if (!isReviewing) {
                onAction(ImagingAction.ProcessFrame(frame))
            } else {
                frame.close()
            }
        }
    }

    val view = LocalView.current

    val rotation = view.display?.rotation ?: Surface.ROTATION_0

    LaunchedEffect(lifecycleOwner, rotation) {
        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }

        val previewUseCase = Preview.Builder()
            .setTargetRotation(rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .build().apply {
                setSurfaceProvider { request ->
                    surfaceRequest = request
                }
            }

        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                    .build()
            )

        Camera2Interop.Extender(imageCaptureBuilder)
            .setSessionCaptureCallback(metadataListener)

        val imageCapture = imageCaptureBuilder.build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                imageCapture,
                imageAnalysis
            )

            imageCaptureUseCase = imageCapture
            camera = boundCamera

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.specimensWithImagesAndInferenceResults.size,
        pageCount = { state.specimensWithImagesAndInferenceResults.size + 1 })

    var isImageLoaded by remember { mutableStateOf(false) }

    // Local slider value for the manual focus distance control (0 = far, 1 = near)
    var focusSliderValue by remember { mutableFloatStateOf(0.5f) }

    // Multi-focus capture state: list of (focusValue, jpegBytes) pairs, null when not showing results
    var multiFocusImages by remember { mutableStateOf<List<Pair<Float, ByteArray>>?>(null) }
    var isMultiFocusCapturing by remember { mutableStateOf(false) }
    // Full-screen viewer: holds the (focusValue, bytes) pair currently being previewed
    var fullScreenImage by remember { mutableStateOf<Pair<Float, ByteArray>?>(null) }

    // Reset slider position when returning to auto focus
    LaunchedEffect(state.manualFocusDistance) {
        if (state.manualFocusDistance == null) {
            focusSliderValue = 0.5f
        }
    }

    LaunchedEffect(state.specimensWithImagesAndInferenceResults.size) {
        pagerState.scrollToPage(state.specimensWithImagesAndInferenceResults.size)
    }

    HorizontalPager(
        state = pagerState, modifier = modifier.fillMaxSize()
    ) { page ->
        when {
            page < state.specimensWithImagesAndInferenceResults.size -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = modifier.fillMaxHeight().wrapContentWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = MaterialTheme.dimensions.paddingMedium)
                                .fillMaxWidth()
                        ) {
                            AnimatedArrowIcon(
                                isLeft = true,
                                enabled = page > 0,
                                onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } }
                            )

                            Box(
                                modifier = Modifier.background(
                                    color = MaterialTheme.colors.accent,
                                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium)
                                )
                            ) {
                                Text(
                                    text = "Specimen ${page + 1} of ${state.specimensWithImagesAndInferenceResults.size} in this Session",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colors.textPrimary,
                                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
                                )
                            }

                            AnimatedArrowIcon(
                                isLeft = false,
                                enabled = true,
                                onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val imageList =
                                state.specimensWithImagesAndInferenceResults[page].specimenImagesAndInferenceResults
                            itemsIndexed(imageList) { index, (specimenImage, inferenceResult) ->
                                CapturedSpecimenTile(
                                    specimen = state.specimensWithImagesAndInferenceResults[page].specimen,
                                    specimenImage = specimenImage,
                                    inferenceResult = inferenceResult,
                                    badgeText = "${index + 1} of ${imageList.size}",
                                    modifier = Modifier
                                        .fillParentMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                if (state.showExitDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            onAction(ImagingAction.DismissExitDialog)
                        },
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = MaterialTheme.dimensions.paddingSmall
                                    )
                            ) {
                                Text(
                                    text = if (state.pendingAction == null) "Exit session?" else "Confirm Action",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { onAction(ImagingAction.DismissExitDialog) },
                                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "Close dialog",
                                        tint = MaterialTheme.colors.icon,
                                        modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraLarge)
                                    )
                                }
                            }
                        },
                        text = {
                            val dialogText = when (state.pendingAction) {
                                null -> "Would you like to save this session for later or submit it now?"
                                is ImagingAction.SaveSessionProgress -> "Are you sure you want to save the session and exit?"
                                is ImagingAction.SubmitSession -> "Are you sure you want to submit the session?"
                                else -> ""
                            }
                            Column {
                                if (dialogText.isNotEmpty()) {
                                    Text(
                                        text = dialogText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.textSecondary
                                    )
                                }

                                if (state.specimensWithImagesAndInferenceResults.isEmpty() && state.pendingAction is ImagingAction.SubmitSession) {
                                    Text(
                                        text = "Warning: You are about to submit a session with zero specimens.",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colors.error,
                                        modifier = Modifier.padding(top = MaterialTheme.dimensions.paddingMedium)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            if (state.pendingAction == null) {
                                OutlinedButton(
                                    onClick = { onAction(ImagingAction.SelectPendingAction(ImagingAction.SubmitSession)) },
                                    border = BorderStroke(
                                        MaterialTheme.dimensions.borderThicknessThick,
                                        MaterialTheme.colors.successConfirm
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_cloud_upload),
                                        contentDescription = "Submit Icon",
                                        tint = MaterialTheme.colors.successConfirm,
                                        modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                                    )
                                    Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
                                    Text(
                                        text = "Submit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.successConfirm
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onAction(ImagingAction.ConfirmPendingAction)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colors.error
                                    )
                                ) {
                                    Text(
                                        text = "Yes, Confirm",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.buttonText
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            if (state.pendingAction == null) {
                                OutlinedButton(
                                    onClick = { onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveSessionProgress)) },
                                    border = BorderStroke(
                                        MaterialTheme.dimensions.borderThicknessThick,
                                        MaterialTheme.colors.info
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_save),
                                        contentDescription = "Save Icon",
                                        tint = MaterialTheme.colors.info,
                                        modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                                    )
                                    Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
                                    Text(
                                        "Save",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.info
                                    )
                                }
                            } else {
                                TextButton(onClick = { onAction(ImagingAction.ClearPendingAction) }) {
                                    Text(
                                        "Back",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    )
                }

                val capturedImages = multiFocusImages
                if (capturedImages != null) {
                    AlertDialog(
                        onDismissRequest = { multiFocusImages = null },
                        title = {
                            Text(
                                text = "Multi-Focus Capture (${capturedImages.size} images)",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                        },
                        text = {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                            ) {
                                itemsIndexed(capturedImages) { _, (focusValue, bytes) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colors.accent,
                                                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${(focusValue * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colors.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(bytes)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Focus ${(focusValue * 100).toInt()}%",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(150.dp)
                                                .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall))
                                                .clickable { fullScreenImage = Pair(focusValue, bytes) }
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { multiFocusImages = null }) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colors.textPrimary
                                )
                            }
                        }
                    )
                }

                fullScreenImage?.let { (focusValue, bytes) ->
                    Dialog(
                        onDismissRequest = { fullScreenImage = null },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                                .clickable { fullScreenImage = null },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bytes)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Focus ${(focusValue * 100).toInt()}% full screen",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(MaterialTheme.dimensions.paddingMedium)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Focus: ${(focusValue * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(MaterialTheme.dimensions.paddingMedium)
                                    .size(MaterialTheme.dimensions.componentHeightMedium)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        shape = CircleShape
                                    )
                                    .clickable { fullScreenImage = null }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Close full screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                                )
                            }
                        }
                    }
                }

                if (state.currentSpecimen.shouldProcessFurther) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_info),
                                    contentDescription = "Info Icon",
                                    tint = MaterialTheme.colors.icon,
                                    modifier = Modifier
                                        .size(MaterialTheme.dimensions.iconSizeLarge)
                                )
                                Spacer(modifier.size(MaterialTheme.dimensions.spacingMedium))
                                Text(
                                    text = "Specimen Selected for Further Processing",
                                    color = MaterialTheme.colors.icon
                                )
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Please package the specimen separately for further laboratory processing before continuing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colors.textSecondary
                                )

                                Spacer(modifier = Modifier.size(MaterialTheme.dimensions.paddingMedium))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = state.hasConfirmedPackaging,
                                        onCheckedChange = { onAction(ImagingAction.TogglePackagingConfirmation(it)) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colors.successConfirm
                                        )
                                    )
                                    Text(
                                        text = "I have packaged this specimen for further processing",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.textPrimary,
                                        modifier = Modifier.padding(start = MaterialTheme.dimensions.paddingSmall)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { onAction(ImagingAction.SaveImageToSession) },
                                enabled = state.hasConfirmedPackaging,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colors.successConfirm,
                                    disabledContainerColor = MaterialTheme.colors.textSecondary
                                )
                            ) {
                                Text(
                                    text = "Continue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colors.buttonText
                                )
                            }
                        }
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = modifier
                            .padding(
                                vertical = MaterialTheme.dimensions.paddingMedium
                            )
                            .fillMaxHeight()
                            .width(IntrinsicSize.Max)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = MaterialTheme.dimensions.paddingSmall,
                                    horizontal = MaterialTheme.dimensions.paddingMedium
                                )
                        ) {
                            if (state.currentImageBytes != null) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(MaterialTheme.dimensions.componentHeightMedium)
                                        .background(
                                            color = MaterialTheme.colors.error,
                                            shape = CircleShape
                                        )
                                ) {
                                    IconButton(
                                        onClick = { onAction(ImagingAction.RetakeImage) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_cancel),
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colors.buttonText,
                                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                                        )
                                    }
                                }
                            } else {
                                AnimatedArrowIcon(
                                    isLeft = true,
                                    enabled = page > 0,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } }
                                )
                            }

                            Spacer(modifier = Modifier.width(MaterialTheme.dimensions.spacingExtraSmall))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.background(
                                    color = MaterialTheme.colors.accent,
                                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium)
                                )
                            ) {
                                Text(
                                    text = "Specimen ${page + 1} in this Session",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colors.textPrimary,
                                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
                                )
                            }

                            Spacer(modifier = Modifier.width(MaterialTheme.dimensions.spacingExtraSmall))

                            if (state.currentImageBytes != null) {
                                EmptySpace(
                                    width = MaterialTheme.dimensions.iconSizeExtraLarge,
                                    height = MaterialTheme.dimensions.iconSizeExtraLarge
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(MaterialTheme.dimensions.componentHeightMedium)
                                        .background(
                                            color = MaterialTheme.colors.successConfirm,
                                            shape = CircleShape
                                        )
                                ) {
                                    IconButton(
                                        onClick = { onAction(ImagingAction.ShowExitDialog) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_exit),
                                            contentDescription = "Exit",
                                            tint = MaterialTheme.colors.buttonText,
                                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                                        )
                                    }
                                }
                            }
                        }

                        if (state.currentImageBytes != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
                                    .padding(MaterialTheme.dimensions.paddingMedium)
                                    .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)),
                                contentAlignment = Alignment.Center
                            ) {
                                SpecimenImageOverlay(
                                    inferenceResult = state.currentInferenceResult,
                                    showBoundingBoxOverlay = isImageLoaded,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(state.currentImageBytes)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = state.currentSpecimen.id,
                                        contentScale = ContentScale.Crop,
                                        onSuccess = { isImageLoaded = true },
                                        onError = { isImageLoaded = false },
                                        error = painterResource(R.drawable.specimen_image_placeholder_not_uploaded),
                                        fallback = painterResource(R.drawable.specimen_image_placeholder_not_uploaded),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            InfoTile(
                                modifier = Modifier.height(MaterialTheme.dimensions.componentHeightExtraExtraExtraLarge)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(
                                            vertical = MaterialTheme.dimensions.paddingExtraLarge,
                                            horizontal = MaterialTheme.dimensions.paddingExtraLarge
                                        )
                                ) {
                                    TextEntryField(
                                        label = "Specimen ID",
                                        value = state.currentSpecimen.id,
                                        onValueChange = {
                                            onAction(ImagingAction.CorrectSpecimenId(it))
                                        },
                                        singleLine = true,
                                        error = state.specimenIdError,
                                        showErrorMessage = false,
                                    )

                                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.paddingSmall)
                                        ) {
                                            if (state.currentSpecimenImage.species != null) {
                                                Text(
                                                    text = "Species: ${state.currentSpecimenImage.species}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }

                                            if (state.currentSpecimenImage.sex != null) {
                                                Text(
                                                    text = "Sex: ${state.currentSpecimenImage.sex}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }

                                            if (state.currentSpecimenImage.abdomenStatus != null) {
                                                Text(
                                                    text = "Abdomen Status: ${state.currentSpecimenImage.abdomenStatus}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onAction(ImagingAction.SaveImageToSession) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colors.successConfirm
                                            ),
                                            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium)
                                        ) {
                                            Text(
                                                text = "Save",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colors.buttonText
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
                                    .padding(MaterialTheme.dimensions.paddingMedium)
                                    .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)),
                                contentAlignment = Alignment.Center,
                            ) {
                                LiveCameraPreview(
                                    surfaceRequest = surfaceRequest,
                                    camera = camera,
                                    inferenceResults = if (state.shouldRunInference) state.previewInferenceResults else emptyList(),
                                    focusPoint = state.focusPoint,
                                    onFocusAt = { normalizedOffset ->
                                        onAction(
                                            ImagingAction.FocusAt(
                                                normalizedOffset
                                            )
                                        )
                                    },
                                    onCancelFocus = { onAction(ImagingAction.CancelFocus) },
                                    modifier = Modifier.fillMaxSize(),
                                    isManualFocusing = state.isManualFocusing,
                                    isProcessing = state.isProcessing,
                                    manualFocusDistance = state.manualFocusDistance
                                )

                                VerticalFocusSlider(
                                    value = focusSliderValue,
                                    isManualMode = state.manualFocusDistance != null,
                                    onValueChange = { value ->
                                        focusSliderValue = value
                                        onAction(ImagingAction.SetFocusDistance(value))
                                    },
                                    onResetToAuto = {
                                        onAction(ImagingAction.SetFocusDistance(null))
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(52.dp)
                                )
                            }

                            InfoTile(
                                modifier = Modifier.height(MaterialTheme.dimensions.componentHeightExtraExtraExtraLarge)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(
                                            vertical = MaterialTheme.dimensions.paddingExtraExtraLarge,
                                            horizontal = MaterialTheme.dimensions.paddingExtraLarge
                                        )
                                ) {
                                    if (state.allowModelInferenceToggle) {
                                        ToggleField(
                                            label = "Run Model Inference",
                                            checked = state.shouldRunInference,
                                            onCheckedChange = {
                                                onAction(
                                                    ImagingAction.ToggleModelInference(
                                                        it
                                                    )
                                                )
                                            },
                                        )
                                        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingLarge))
                                    }

                                    Text(
                                        text = if (state.currentSpecimen.id == "") "Specimen ID will appear here" else state.currentSpecimen.id,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = if (state.currentSpecimen.id == "") MaterialTheme.colors.textSecondary else MaterialTheme.colors.textPrimary,
                                    )

                                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

                                    ActionButton(
                                        label = if (isMultiFocusCapturing) "Capturing…" else "Capture",
                                        onClick = {
                                            val captureUseCase = imageCaptureUseCase ?: return@ActionButton
                                            scope.launch {
                                                isMultiFocusCapturing = true
                                                val captured = mutableListOf<Pair<Float, ByteArray>>()
                                                val focusValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

                                                for (focusValue in focusValues) {
                                                    focusSliderValue = focusValue
                                                    onAction(ImagingAction.SetFocusDistance(focusValue))
                                                    // Allow lens time to physically move to the target distance
                                                    delay(700L)

                                                    val bytes = suspendCancellableCoroutine<ByteArray?> { cont ->
                                                        captureUseCase.takePicture(
                                                            ContextCompat.getMainExecutor(context),
                                                            object : ImageCapture.OnImageCapturedCallback() {
                                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                                    val buffer = image.planes[0].buffer
                                                                    val data = ByteArray(buffer.remaining())
                                                                    buffer.get(data)
                                                                    image.close()
                                                                    cont.resume(data)
                                                                }

                                                                override fun onError(exception: ImageCaptureException) {
                                                                    cont.resume(null)
                                                                }
                                                            }
                                                        )
                                                    }
                                                    bytes?.let { captured.add(Pair(focusValue, it)) }
                                                }

                                                multiFocusImages = captured
                                                isMultiFocusCapturing = false

                                                // Restore auto-focus and reset slider
                                                onAction(ImagingAction.SetFocusDistance(null))
                                                focusSliderValue = 0.5f
                                            }
                                        },
                                        iconPainter = painterResource(id = R.drawable.ic_camera),
                                        enabled = (!state.isProcessing && state.isCameraReady && !isMultiFocusCapturing),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalFocusSlider(
    value: Float,
    isManualMode: Boolean,
    onValueChange: (Float) -> Unit,
    onResetToAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colors.warning
    val inactiveColor = MaterialTheme.colors.textSecondary.copy(alpha = 0.6f)
    val thumbColor = if (isManualMode) activeColor else inactiveColor

    var sliderHeightPx by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
            )
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // AF / Manual toggle button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    color = if (!isManualMode) activeColor.copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.12f)
                )
                .clickable { onResetToAuto() }
        ) {
            Text(
                text = "AF",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (!isManualMode) Color.White else inactiveColor,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // "Near" label
        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall,
            color = if (isManualMode && value > 0.7f) activeColor else inactiveColor,
            fontWeight = if (isManualMode && value > 0.7f) FontWeight.Bold else FontWeight.Normal,
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical slider track
        Box(
            modifier = Modifier
                .weight(1f)
                .width(24.dp)
                .onSizeChanged { sliderHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (sliderHeightPx > 0f) {
                            val newValue = 1f - (down.position.y / sliderHeightPx).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        }
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (sliderHeightPx > 0f) {
                                val newValue = 1f - (change.position.y / sliderHeightPx).coerceIn(0f, 1f)
                                onValueChange(newValue)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackX = size.width / 2f
                val thumbY = (1f - value) * size.height

                // Background track
                drawLine(
                    color = inactiveColor,
                    start = Offset(trackX, 0f),
                    end = Offset(trackX, size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Active portion of track (from thumb down to "far" end)
                if (isManualMode) {
                    drawLine(
                        color = activeColor.copy(alpha = 0.7f),
                        start = Offset(trackX, thumbY),
                        end = Offset(trackX, size.height),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Thumb circle
                drawCircle(
                    color = thumbColor,
                    radius = 9.dp.toPx(),
                    center = Offset(trackX, if (isManualMode) thumbY else size.height / 2f)
                )

                // Thumb outline ring
                drawCircle(
                    color = Color.White.copy(alpha = if (isManualMode) 0.85f else 0.3f),
                    radius = 9.dp.toPx(),
                    center = Offset(trackX, if (isManualMode) thumbY else size.height / 2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // "Far" label
        Text(
            text = "F",
            style = MaterialTheme.typography.labelSmall,
            color = if (isManualMode && value < 0.3f) activeColor else inactiveColor,
            fontWeight = if (isManualMode && value < 0.3f) FontWeight.Bold else FontWeight.Normal,
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(6.dp))
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
