package com.wootan.ghostcamera.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.wootan.ghostcamera.camera.CameraRotation
import com.wootan.ghostcamera.camera.CaptureStore
import com.wootan.ghostcamera.camera.ComparisonStore
import com.wootan.ghostcamera.data.ReferencePhoto
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    cameraPermissionGranted: Boolean,
    references: List<ReferencePhoto>,
    selectedIndex: Int,
    ghostOpacity: Float,
    ghostScaleMode: GhostScaleMode,
    cameraRotationQuarterTurns: Int,
    bodyGuideMode: BodyGuideMode,
    bodyGuidePositions: BodyGuidePositions,
    onSelectedIndexChange: (Int) -> Unit,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostOpacityChangeFinished: () -> Unit,
    onGhostScaleModeChange: (GhostScaleMode) -> Unit,
    onRotateCamera: () -> Unit,
    onBodyGuideModeChange: (BodyGuideMode) -> Unit,
    onBodyGuidePositionsChange: (BodyGuidePositions) -> Unit,
    onBodyGuidePositionsChangeFinished: (BodyGuidePositions) -> Unit,
    onResetBodyGuidePositions: () -> Unit,
    onManageReferences: () -> Unit,
    onAddReferences: () -> Unit,
    onRequestCameraPermission: () -> Unit,
) {
    if (!cameraPermissionGranted) {
        CameraPermissionScreen(onRequestCameraPermission)
        return
    }

    ActiveCameraScreen(
        references = references,
        selectedIndex = selectedIndex,
        ghostOpacity = ghostOpacity,
        ghostScaleMode = ghostScaleMode,
        cameraRotationQuarterTurns = cameraRotationQuarterTurns,
        bodyGuideMode = bodyGuideMode,
        bodyGuidePositions = bodyGuidePositions,
        onSelectedIndexChange = onSelectedIndexChange,
        onGhostOpacityChange = onGhostOpacityChange,
        onGhostOpacityChangeFinished = onGhostOpacityChangeFinished,
        onGhostScaleModeChange = onGhostScaleModeChange,
        onRotateCamera = onRotateCamera,
        onBodyGuideModeChange = onBodyGuideModeChange,
        onBodyGuidePositionsChange = onBodyGuidePositionsChange,
        onBodyGuidePositionsChangeFinished = onBodyGuidePositionsChangeFinished,
        onResetBodyGuidePositions = onResetBodyGuidePositions,
        onManageReferences = onManageReferences,
        onAddReferences = onAddReferences,
    )
}

@Composable
private fun ActiveCameraScreen(
    references: List<ReferencePhoto>,
    selectedIndex: Int,
    ghostOpacity: Float,
    ghostScaleMode: GhostScaleMode,
    cameraRotationQuarterTurns: Int,
    bodyGuideMode: BodyGuideMode,
    bodyGuidePositions: BodyGuidePositions,
    onSelectedIndexChange: (Int) -> Unit,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostOpacityChangeFinished: () -> Unit,
    onGhostScaleModeChange: (GhostScaleMode) -> Unit,
    onRotateCamera: () -> Unit,
    onBodyGuideModeChange: (BodyGuideMode) -> Unit,
    onBodyGuidePositionsChange: (BodyGuidePositions) -> Unit,
    onBodyGuidePositionsChangeFinished: (BodyGuidePositions) -> Unit,
    onResetBodyGuidePositions: () -> Unit,
    onManageReferences: () -> Unit,
    onAddReferences: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
            setPinchToZoomEnabled(true)
            setTapToFocusEnabled(true)
        }
    }

    var useFrontCamera by rememberSaveable { mutableStateOf(false) }
    var flashMode by rememberSaveable { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraStreaming by remember { mutableStateOf(false) }
    var cameraInitializationFailed by remember { mutableStateOf(false) }
    var captureRunning by remember { mutableStateOf(false) }
    var captureFlashVisible by remember { mutableStateOf(false) }
    var lastCaptureUriValue by rememberSaveable { mutableStateOf<String?>(null) }
    var lastCaptureReferenceId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastCaptureReferenceRotationQuarterTurns by rememberSaveable { mutableIntStateOf(0) }
    var showLastCapture by rememberSaveable { mutableStateOf(false) }
    val cameraReady = cameraInitialized && cameraStreaming && !cameraInitializationFailed

    DisposableEffect(controller, lifecycleOwner) {
        var active = true
        var lifecycleBound = false
        val initializationFuture = controller.initializationFuture

        runCatching {
            controller.bindToLifecycle(lifecycleOwner)
            lifecycleBound = true
        }.onFailure {
            cameraInitializationFailed = true
            Toast.makeText(context, "카메라를 시작하지 못했습니다", Toast.LENGTH_LONG).show()
        }

        initializationFuture.addListener(
            {
                if (active) {
                    runCatching { initializationFuture.get() }
                        .onSuccess {
                            cameraInitialized = true
                            cameraInitializationFailed =
                                !lifecycleBound || controller.cameraInfo == null
                        }
                        .onFailure {
                            cameraInitialized = false
                            cameraInitializationFailed = true
                            Toast.makeText(
                                context,
                                "카메라를 사용할 수 없습니다. 앱을 다시 실행해 주세요",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            active = false
            controller.unbind()
        }
    }

    LaunchedEffect(useFrontCamera) {
        val selector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        runCatching { controller.cameraSelector = selector }
            .onFailure {
                if (useFrontCamera) {
                    useFrontCamera = false
                    Toast.makeText(context, "전면 카메라를 사용할 수 없습니다", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    LaunchedEffect(flashMode) {
        runCatching { controller.imageCaptureFlashMode = flashMode }
    }

    val currentReference = references.getOrNull(selectedIndex)
    val lastCaptureUri = lastCaptureUriValue?.let(Uri::parse)
    val lastCaptureReference = lastCaptureReferenceId?.let { referenceId ->
        references.firstOrNull { it.id == referenceId }
    }

    fun capturePhoto() {
        if (captureRunning) return
        if (!cameraReady) {
            Toast.makeText(context, "카메라를 준비하고 있습니다", Toast.LENGTH_SHORT).show()
            return
        }
        captureRunning = true
        val captureRotationQuarterTurns = cameraRotationQuarterTurns
        val captureReference = currentReference
        val target = CaptureStore.createTarget(context)
        runCatching {
            controller.takePicture(
                target.options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = CaptureStore.resolveSavedUri(
                            context,
                            outputFileResults,
                            target,
                        )
                        scope.launch {
                            savedUri?.let { uri ->
                                withContext(Dispatchers.IO) {
                                    CaptureStore.applyAdditionalRotation(
                                        context,
                                        uri,
                                        captureRotationQuarterTurns,
                                    )
                                }
                            }
                            lastCaptureUriValue = savedUri?.toString()
                            lastCaptureReferenceId = captureReference?.id
                            lastCaptureReferenceRotationQuarterTurns =
                                captureRotationQuarterTurns
                            captureRunning = false
                            captureFlashVisible = true
                            delay(90)
                            captureFlashVisible = false
                            showLastCapture = savedUri != null && captureReference != null
                            Toast.makeText(context, "갤러리에 저장했습니다", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        captureRunning = false
                        Toast.makeText(
                            context,
                            "촬영하지 못했습니다. 다시 시도해 주세요",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        }.onFailure {
            captureRunning = false
            Toast.makeText(
                context,
                "카메라가 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    if (showLastCapture && lastCaptureUri != null) {
        BackHandler { showLastCapture = false }
        LastCapturePreview(
            uri = lastCaptureUri,
            reference = lastCaptureReference,
            referenceRotationQuarterTurns = lastCaptureReferenceRotationQuarterTurns,
            onClose = { showLastCapture = false },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        CameraStage(
            controller = controller,
            reference = currentReference,
            referenceNumber = selectedIndex + 1,
            ghostOpacity = ghostOpacity,
            ghostScaleMode = ghostScaleMode,
            mirrorGhost = useFrontCamera,
            rotationQuarterTurns = cameraRotationQuarterTurns,
            bodyGuideMode = bodyGuideMode,
            bodyGuidePositions = bodyGuidePositions,
            onBodyGuidePositionsChange = onBodyGuidePositionsChange,
            onBodyGuidePositionsChangeFinished = onBodyGuidePositionsChangeFinished,
            onStreamStateChanged = { streaming ->
                cameraStreaming = streaming
                if (streaming) cameraInitializationFailed = false
            },
            modifier = Modifier.fillMaxSize(),
        )

        CameraTopBar(
            referenceCount = references.size,
            selectedIndex = selectedIndex,
            flashMode = flashMode,
            cameraRotationDegrees = CameraRotation.degrees(cameraRotationQuarterTurns),
            bodyGuideMode = bodyGuideMode,
            onPreviousReference = { onSelectedIndexChange(selectedIndex - 1) },
            onNextReference = { onSelectedIndexChange(selectedIndex + 1) },
            onManageReferences = onManageReferences,
            onAddReferences = onAddReferences,
            onFlashModeChange = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            },
            onRotateCamera = onRotateCamera,
            onBodyGuideModeChange = onBodyGuideModeChange,
            onResetBodyGuidePositions = onResetBodyGuidePositions,
            onSwitchCamera = { useFrontCamera = !useFrontCamera },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        CameraBottomBar(
            hasReference = currentReference != null,
            ghostOpacity = ghostOpacity,
            ghostScaleMode = ghostScaleMode,
            cameraReady = cameraReady,
            cameraInitializationFailed = cameraInitializationFailed,
            captureRunning = captureRunning,
            lastCaptureUri = lastCaptureUri,
            onGhostOpacityChange = onGhostOpacityChange,
            onGhostOpacityChangeFinished = onGhostOpacityChangeFinished,
            onGhostScaleModeChange = onGhostScaleModeChange,
            onCapture = ::capturePhoto,
            onOpenLastCapture = { showLastCapture = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (captureFlashVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.42f)),
            )
        }

    }
}

@Composable
private fun CameraStage(
    controller: LifecycleCameraController,
    reference: ReferencePhoto?,
    referenceNumber: Int,
    ghostOpacity: Float,
    ghostScaleMode: GhostScaleMode,
    mirrorGhost: Boolean,
    rotationQuarterTurns: Int,
    bodyGuideMode: BodyGuideMode,
    bodyGuidePositions: BodyGuidePositions,
    onBodyGuidePositionsChange: (BodyGuidePositions) -> Unit,
    onBodyGuidePositionsChangeFinished: (BodyGuidePositions) -> Unit,
    onStreamStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedRotation = CameraRotation.normalize(rotationQuarterTurns)
    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val stageModifier = if (normalizedRotation % 2 == 1) {
            Modifier.requiredSize(width = maxHeight, height = maxWidth)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = stageModifier.graphicsLayer {
                rotationZ = CameraRotation.degrees(normalizedRotation).toFloat()
            },
        ) {
            CameraPreview(
                controller = controller,
                onStreamStateChanged = onStreamStateChanged,
                modifier = Modifier.fillMaxSize(),
            )
            reference?.let {
                AsyncImage(
                    model = it.file,
                    contentDescription = "$referenceNumber 번 고스트",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = ghostOpacity
                            scaleX = if (mirrorGhost) -1f else 1f
                        },
                    contentScale = ghostScaleMode.contentScale,
                )
            }
            BodyGuideOverlay(
                mode = bodyGuideMode,
                positions = bodyGuidePositions,
                onPositionsChange = onBodyGuidePositionsChange,
                onPositionsChangeFinished = onBodyGuidePositionsChangeFinished,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    onStreamStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context, controller) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            this.controller = controller
        }
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val observer = Observer<PreviewView.StreamState> { streamState ->
            onStreamStateChanged(streamState == PreviewView.StreamState.STREAMING)
        }
        previewView.previewStreamState.observe(lifecycleOwner, observer)
        onDispose {
            previewView.previewStreamState.removeObserver(observer)
            previewView.controller = null
            onStreamStateChanged(false)
        }
    }

    AndroidView(
        factory = { previewView },
        update = { it.controller = controller },
        modifier = modifier,
    )
}

@Composable
private fun CameraTopBar(
    referenceCount: Int,
    selectedIndex: Int,
    flashMode: Int,
    cameraRotationDegrees: Int,
    bodyGuideMode: BodyGuideMode,
    onPreviousReference: () -> Unit,
    onNextReference: () -> Unit,
    onManageReferences: () -> Unit,
    onAddReferences: () -> Unit,
    onFlashModeChange: () -> Unit,
    onRotateCamera: () -> Unit,
    onBodyGuideModeChange: (BodyGuideMode) -> Unit,
    onResetBodyGuidePositions: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compactControls = LocalConfiguration.current.screenWidthDp < 500

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
            .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CameraIconButton(
            icon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
            contentDescription = "기준 사진 관리",
            onClick = onManageReferences,
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (referenceCount == 0) {
                FilledTonalButton(
                    onClick = onAddReferences,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PanelBlack,
                        contentColor = SoftWhite,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("기준 사진 등록", fontSize = 14.sp)
                }
            } else {
                ReferenceNavigator(
                    selectedIndex = selectedIndex,
                    referenceCount = referenceCount,
                    onPrevious = onPreviousReference,
                    onNext = onNextReference,
                )
            }
        }

        if (!compactControls) {
            BodyGuideMenuButton(
                mode = bodyGuideMode,
                onModeChange = onBodyGuideModeChange,
                onResetPositions = onResetBodyGuidePositions,
            )
        }
        CameraRotationButton(
            rotationDegrees = cameraRotationDegrees,
            onClick = onRotateCamera,
        )
        if (compactControls) {
            CompactCameraActions(
                flashMode = flashMode,
                bodyGuideMode = bodyGuideMode,
                onFlashModeChange = onFlashModeChange,
                onBodyGuideModeChange = onBodyGuideModeChange,
                onResetBodyGuidePositions = onResetBodyGuidePositions,
                onSwitchCamera = onSwitchCamera,
            )
        } else {
            FlashButton(
                flashMode = flashMode,
                onClick = onFlashModeChange,
            )
            CameraIconButton(
                icon = { Icon(Icons.Outlined.Cameraswitch, contentDescription = null) },
                contentDescription = "카메라 전환",
                onClick = onSwitchCamera,
            )
        }
    }
}

@Composable
private fun CameraRotationButton(
    rotationDegrees: Int,
    onClick: () -> Unit,
) {
    CameraIconButton(
        icon = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Outlined.RotateRight,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    text = "$rotationDegrees°",
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                )
            }
        },
        contentDescription = "카메라 회전, 현재 ${rotationDegrees}도",
        onClick = onClick,
    )
}

@Composable
private fun BodyGuideMenuButton(
    mode: BodyGuideMode,
    onModeChange: (BodyGuideMode) -> Unit,
    onResetPositions: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        CameraIconButton(
            icon = {
                Icon(
                    imageVector = when (mode) {
                        BodyGuideMode.Off -> Icons.Outlined.GridOff
                        BodyGuideMode.Locked -> Icons.Outlined.GridOn
                        BodyGuideMode.Editing -> Icons.Outlined.Edit
                    },
                    contentDescription = null,
                    tint = when (mode) {
                        BodyGuideMode.Off -> SoftWhite
                        BodyGuideMode.Locked -> GhostTeal
                        BodyGuideMode.Editing -> CaptureAmber
                    },
                )
            },
            contentDescription = when (mode) {
                BodyGuideMode.Off -> "촬영 가이드 메뉴, 꺼짐"
                BodyGuideMode.Locked -> "촬영 가이드 메뉴, 켜짐"
                BodyGuideMode.Editing -> "촬영 가이드 메뉴, 위치 조정 중"
            },
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BodyGuideMenuItems(
                mode = mode,
                onModeChange = { selectedMode ->
                    expanded = false
                    onModeChange(selectedMode)
                },
                onResetPositions = {
                    expanded = false
                    onResetPositions()
                },
            )
        }
    }
}

@Composable
private fun BodyGuideMenuItems(
    mode: BodyGuideMode,
    onModeChange: (BodyGuideMode) -> Unit,
    onResetPositions: () -> Unit,
) {
    val guideEnabled = mode != BodyGuideMode.Off
    DropdownMenuItem(
        text = { Text(if (guideEnabled) "촬영 가이드 끄기" else "촬영 가이드 켜기") },
        leadingIcon = {
            Icon(
                imageVector = if (guideEnabled) Icons.Outlined.GridOff else Icons.Outlined.GridOn,
                contentDescription = null,
            )
        },
        trailingIcon = if (guideEnabled) {
            { Icon(Icons.Outlined.Check, contentDescription = null, tint = GhostTeal) }
        } else {
            null
        },
        onClick = {
            onModeChange(if (guideEnabled) BodyGuideMode.Off else BodyGuideMode.Locked)
        },
    )

    if (guideEnabled) {
        DropdownMenuItem(
            text = {
                Text(
                    if (mode == BodyGuideMode.Editing) "가이드 위치 고정" else "가이드 위치 조정",
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (mode == BodyGuideMode.Editing) {
                        Icons.Outlined.Lock
                    } else {
                        Icons.Outlined.Edit
                    },
                    contentDescription = null,
                )
            },
            onClick = {
                onModeChange(
                    if (mode == BodyGuideMode.Editing) {
                        BodyGuideMode.Locked
                    } else {
                        BodyGuideMode.Editing
                    },
                )
            },
        )
        DropdownMenuItem(
            text = { Text("가이드 기본 위치 복원") },
            leadingIcon = {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null)
            },
            onClick = onResetPositions,
        )
    }
}

@Composable
private fun FlashButton(
    flashMode: Int,
    onClick: () -> Unit,
) {
    CameraIconButton(
        icon = {
            Icon(
                imageVector = when (flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Outlined.FlashAuto
                    ImageCapture.FLASH_MODE_ON -> Icons.Outlined.FlashOn
                    else -> Icons.Outlined.FlashOff
                },
                contentDescription = null,
            )
        },
        contentDescription = flashModeLabel(flashMode),
        onClick = onClick,
    )
}

@Composable
private fun CompactCameraActions(
    flashMode: Int,
    bodyGuideMode: BodyGuideMode,
    onFlashModeChange: () -> Unit,
    onBodyGuideModeChange: (BodyGuideMode) -> Unit,
    onResetBodyGuidePositions: () -> Unit,
    onSwitchCamera: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        CameraIconButton(
            icon = { Icon(Icons.Outlined.MoreVert, contentDescription = null) },
            contentDescription = "카메라 메뉴",
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BodyGuideMenuItems(
                mode = bodyGuideMode,
                onModeChange = { mode ->
                    expanded = false
                    onBodyGuideModeChange(mode)
                },
                onResetPositions = {
                    expanded = false
                    onResetBodyGuidePositions()
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text(flashModeLabel(flashMode)) },
                leadingIcon = {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Outlined.FlashAuto
                            ImageCapture.FLASH_MODE_ON -> Icons.Outlined.FlashOn
                            else -> Icons.Outlined.FlashOff
                        },
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onFlashModeChange()
                },
            )
            DropdownMenuItem(
                text = { Text("카메라 전환") },
                leadingIcon = {
                    Icon(Icons.Outlined.Cameraswitch, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onSwitchCamera()
                },
            )
        }
    }
}

private fun flashModeLabel(flashMode: Int): String = when (flashMode) {
    ImageCapture.FLASH_MODE_AUTO -> "플래시 자동"
    ImageCapture.FLASH_MODE_ON -> "플래시 켬"
    else -> "플래시 끔"
}

@Composable
private fun ReferenceNavigator(
    selectedIndex: Int,
    referenceCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = PanelBlack,
        contentColor = SoftWhite,
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .pointerInput(selectedIndex, referenceCount) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            totalDrag += amount
                        },
                        onDragEnd = {
                            if (abs(totalDrag) >= 36f) {
                                if (totalDrag > 0f && selectedIndex > 0) onPrevious()
                                if (totalDrag < 0f && selectedIndex < referenceCount - 1) onNext()
                            }
                        },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = selectedIndex > 0,
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "이전 기준 사진")
            }
            Text(
                text = "${selectedIndex + 1} / $referenceCount",
                modifier = Modifier.width(58.dp),
                color = SoftWhite,
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(
                onClick = onNext,
                enabled = selectedIndex < referenceCount - 1,
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "다음 기준 사진")
            }
        }
    }
}

@Composable
private fun CameraBottomBar(
    hasReference: Boolean,
    ghostOpacity: Float,
    ghostScaleMode: GhostScaleMode,
    cameraReady: Boolean,
    cameraInitializationFailed: Boolean,
    captureRunning: Boolean,
    lastCaptureUri: Uri?,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostOpacityChangeFinished: () -> Unit,
    onGhostScaleModeChange: (GhostScaleMode) -> Unit,
    onCapture: () -> Unit,
    onOpenLastCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(visible = hasReference) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(6.dp),
                color = PanelBlack,
                contentColor = SoftWhite,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostScaleMenu(
                        selectedMode = ghostScaleMode,
                        onModeSelected = onGhostScaleModeChange,
                    )
                    Spacer(Modifier.width(4.dp))
                    Slider(
                        value = ghostOpacity,
                        onValueChange = onGhostOpacityChange,
                        onValueChangeFinished = onGhostOpacityChangeFinished,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(ghostOpacity * 100).roundToInt()}%",
                        modifier = Modifier.width(48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                lastCaptureUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "최근 촬영 사진 열기",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenLastCapture),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            ShutterButton(
                cameraReady = cameraReady,
                cameraInitializationFailed = cameraInitializationFailed,
                captureRunning = captureRunning,
                onClick = onCapture,
            )

            Spacer(Modifier.size(58.dp))
        }
    }
}

@Composable
private fun GhostScaleMenu(
    selectedMode: GhostScaleMode,
    onModeSelected: (GhostScaleMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .semantics {
                    contentDescription = "고스트 비율, 현재 ${selectedMode.label}"
                },
        ) {
            Icon(
                imageVector = ghostScaleIcon(selectedMode),
                contentDescription = null,
                tint = GhostTeal,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GhostScaleMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    leadingIcon = {
                        Icon(ghostScaleIcon(mode), contentDescription = null)
                    },
                    trailingIcon = if (mode == selectedMode) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onModeSelected(mode)
                    },
                )
            }
        }
    }
}

private fun ghostScaleIcon(mode: GhostScaleMode) = when (mode) {
    GhostScaleMode.Fill -> Icons.Outlined.Crop
    GhostScaleMode.Fit -> Icons.Outlined.FitScreen
    GhostScaleMode.Stretch -> Icons.Outlined.AspectRatio
}

@Composable
private fun ShutterButton(
    cameraReady: Boolean,
    cameraInitializationFailed: Boolean,
    captureRunning: Boolean,
    onClick: () -> Unit,
) {
    val shutterEnabled = cameraReady && !captureRunning
    Box(
        modifier = Modifier
            .size(82.dp)
            .semantics {
                contentDescription = when {
                    cameraInitializationFailed -> "카메라를 사용할 수 없음"
                    !cameraReady -> "카메라 준비 중"
                    else -> "사진 촬영"
                }
                role = Role.Button
            }
            .clickable(
                enabled = shutterEnabled,
                role = Role.Button,
                onClick = onClick,
            )
            .background(
                color = Color.White.copy(alpha = if (cameraReady) 1f else 0.58f),
                shape = CircleShape,
            )
            .padding(6.dp)
            .background(
                color = CaptureAmber.copy(alpha = if (cameraReady) 1f else 0.58f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraInitializationFailed) {
            Icon(
                imageVector = Icons.Outlined.NoPhotography,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = Color(0xFF2A1A00),
            )
        } else if (captureRunning || !cameraReady) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = Color(0xFF2A1A00),
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
private fun CameraIconButton(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = PanelBlack,
            contentColor = SoftWhite,
        ),
    ) { icon() }
}

@Composable
private fun LastCapturePreview(
    uri: Uri,
    reference: ReferencePhoto?,
    referenceRotationQuarterTurns: Int,
    onClose: () -> Unit,
) {
    if (reference != null) {
        BeforeAfterPreview(
            reference = reference,
            afterUri = uri,
            referenceRotationQuarterTurns = referenceRotationQuarterTurns,
            onClose = onClose,
        )
    } else {
        SingleCapturePreview(uri = uri, onClose = onClose)
    }
}

@Composable
private fun BeforeAfterPreview(
    reference: ReferencePhoto,
    afterUri: Uri,
    referenceRotationQuarterTurns: Int,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saveRunning by remember { mutableStateOf(false) }
    var comparisonSaved by rememberSaveable(afterUri.toString()) { mutableStateOf(false) }

    fun saveComparison() {
        if (saveRunning || comparisonSaved) return
        saveRunning = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    ComparisonStore.save(
                        context = context,
                        beforeFile = reference.file,
                        afterUri = afterUri,
                        beforeRotationQuarterTurns = referenceRotationQuarterTurns,
                    )
                }
            }
            saveRunning = false
            if (result.isSuccess) {
                comparisonSaved = true
                Toast.makeText(
                    context,
                    "B/A 콜라주를 갤러리에 저장했습니다",
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "B/A 콜라주를 저장하지 못했습니다",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111719))
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "카메라로 돌아가기",
                    tint = SoftWhite,
                )
            }
            Text(
                text = "촬영 비교",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = SoftWhite,
            )
            IconButton(
                onClick = ::saveComparison,
                enabled = !saveRunning && !comparisonSaved,
                modifier = Modifier.semantics {
                    contentDescription = if (comparisonSaved) {
                        "B/A 콜라주 저장됨"
                    } else {
                        "B/A 콜라주 저장"
                    }
                },
            ) {
                when {
                    saveRunning -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GhostTeal,
                        strokeWidth = 2.5.dp,
                    )
                    comparisonSaved -> Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = GhostTeal,
                    )
                    else -> Icon(
                        Icons.Outlined.SaveAlt,
                        contentDescription = null,
                        tint = SoftWhite,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                    ),
                ),
        ) {
            ComparisonPane(
                label = "BEFORE",
                model = reference.file,
                contentDescription = "기준 사진",
                rotationQuarterTurns = referenceRotationQuarterTurns,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color.White),
            )
            ComparisonPane(
                label = "AFTER",
                model = afterUri,
                contentDescription = "촬영 사진",
                rotationQuarterTurns = 0,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ComparisonPane(
    label: String,
    model: Any,
    contentDescription: String,
    rotationQuarterTurns: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        RotatedFitImage(
            model = model,
            contentDescription = contentDescription,
            rotationQuarterTurns = rotationQuarterTurns,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xCC000000),
            contentColor = Color.White,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun RotatedFitImage(
    model: Any,
    contentDescription: String,
    rotationQuarterTurns: Int,
    modifier: Modifier = Modifier,
) {
    val normalizedRotation = CameraRotation.normalize(rotationQuarterTurns)
    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val imageModifier = if (normalizedRotation % 2 == 1) {
            Modifier.requiredSize(width = maxHeight, height = maxWidth)
        } else {
            Modifier.fillMaxSize()
        }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = imageModifier.graphicsLayer {
                rotationZ = CameraRotation.degrees(normalizedRotation).toFloat()
            },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun SingleCapturePreview(
    uri: Uri,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "최근 촬영 사진",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(top = 8.dp, start = 8.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "카메라로 돌아가기",
                    tint = SoftWhite,
                )
            }
            Text(
                text = "최근 촬영",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = SoftWhite,
            )
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = GhostTeal,
            )
            Spacer(Modifier.width(8.dp))
            Text("갤러리에 저장됨", fontSize = 13.sp, color = SoftWhite)
        }
    }
}

@Composable
private fun CameraPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBlack)
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = GhostTeal,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "카메라 권한이 필요합니다",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequestPermission) {
            Text("카메라 허용")
        }
    }
}
