package com.vci.vectorcamapp.imaging.presentation

import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
import com.vci.vectorcamapp.imaging.presentation.components.camera.LiveCameraPreview
import com.vci.vectorcamapp.imaging.presentation.components.icon.AnimatedArrowIcon
import com.vci.vectorcamapp.imaging.presentation.components.specimen.CapturedSpecimenTile
import com.vci.vectorcamapp.imaging.presentation.components.specimen.SpecimenImageOverlay
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                    .build()
            )
            .build()

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

    LaunchedEffect(state.specimensWithImagesAndInferenceResults.size) {
        pagerState.scrollToPage(state.specimensWithImagesAndInferenceResults.size)
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                            val previewResults = if (state.shouldRunInference) state.previewInferenceResults else emptyList()
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
                                    inferenceResults = previewResults,
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
                                    isProcessing = state.isProcessing
                                )
                                if (previewResults.isNotEmpty() || state.previewProcessingTimeMs != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colors.cardBackground.copy(alpha = 0.95f))
                                            .padding(8.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            state.previewProcessingTimeMs?.let { ms ->
                                                Text(
                                                    text = "Preview: ${ms}ms",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }
                                            previewResults.take(2).forEachIndexed { index, r ->
                                                Text(
                                                    text = "[$index] x=%.3f y=%.3f w=%.3f h=%.3f conf=%.2f".format(
                                                        r.bboxTopLeftX, r.bboxTopLeftY, r.bboxWidth, r.bboxHeight, r.bboxConfidence
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }
                                        }
                                    }
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
                                        label = "Capture",
                                        onClick = {
                                            imageCaptureUseCase?.let {
                                                onAction(ImagingAction.CaptureImage(it))
                                            }
                                        },
                                        iconPainter = painterResource(id = R.drawable.ic_camera),
                                        enabled = (!state.isProcessing && state.isCameraReady),
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

        state.debugRawCaptureImageBytes?.let { rawBytes ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall))
                    .background(MaterialTheme.colors.cardBackground.copy(alpha = 0.95f))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.captureProcessingTimeMs?.let { ms ->
                        Text(
                            text = "Capture: ${ms}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(rawBytes)
                            .crossfade(false)
                            .build(),
                        contentDescription = "Raw capture (debug)",
                        modifier = Modifier
                            .size(120.dp, 160.dp)
                            .clip(RoundedCornerShape(4.dp))
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
