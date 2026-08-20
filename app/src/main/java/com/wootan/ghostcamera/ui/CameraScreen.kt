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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.wootan.ghostcamera.camera.CaptureStore
import com.wootan.ghostcamera.data.ReferencePhoto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    cameraPermissionGranted: Boolean,
    references: List<ReferencePhoto>,
    selectedIndex: Int,
    ghostOpacity: Float,
    onSelectedIndexChange: (Int) -> Unit,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostOpacityChangeFinished: () -> Unit,
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
        onSelectedIndexChange = onSelectedIndexChange,
        onGhostOpacityChange = onGhostOpacityChange,
        onGhostOpacityChangeFinished = onGhostOpacityChangeFinished,
        onManageReferences = onManageReferences,
        onAddReferences = onAddReferences,
    )
}

@Composable
private fun ActiveCameraScreen(
    references: List<ReferencePhoto>,
    selectedIndex: Int,
    ghostOpacity: Float,
    onSelectedIndexChange: (Int) -> Unit,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostOpacityChangeFinished: () -> Unit,
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
    var captureRunning by remember { mutableStateOf(false) }
    var captureFlashVisible by remember { mutableStateOf(false) }
    var lastCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var showLastCapture by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
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

    fun capturePhoto() {
        if (captureRunning) return
        captureRunning = true
        val target = CaptureStore.createTarget(context)
        controller.takePicture(
            target.options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    lastCaptureUri = CaptureStore.resolveSavedUri(
                        context,
                        outputFileResults,
                        target,
                    )
                    captureRunning = false
                    captureFlashVisible = true
                    scope.launch {
                        delay(90)
                        captureFlashVisible = false
                    }
                    Toast.makeText(context, "갤러리에 저장했습니다", Toast.LENGTH_SHORT).show()
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxSize(),
        )

        currentReference?.let { reference ->
            AsyncImage(
                model = reference.file,
                contentDescription = "${selectedIndex + 1} 번 고스트",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = ghostOpacity
                        scaleX = if (useFrontCamera) -1f else 1f
                    },
                contentScale = ContentScale.Crop,
            )
        }

        CameraTopBar(
            referenceCount = references.size,
            selectedIndex = selectedIndex,
            flashMode = flashMode,
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
            onSwitchCamera = { useFrontCamera = !useFrontCamera },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        CameraBottomBar(
            hasReference = currentReference != null,
            ghostOpacity = ghostOpacity,
            captureRunning = captureRunning,
            lastCaptureUri = lastCaptureUri,
            onGhostOpacityChange = onGhostOpacityChange,
            onGhostOpacityChangeFinished = onGhostOpacityChangeFinished,
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

        if (showLastCapture && lastCaptureUri != null) {
            BackHandler { showLastCapture = false }
            LastCapturePreview(
                uri = lastCaptureUri!!,
                onClose = { showLastCapture = false },
            )
        }
    }
}

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.controller = controller
            }
        },
        update = { previewView -> previewView.controller = controller },
        modifier = modifier,
    )
}

@Composable
private fun CameraTopBar(
    referenceCount: Int,
    selectedIndex: Int,
    flashMode: Int,
    onPreviousReference: () -> Unit,
    onNextReference: () -> Unit,
    onManageReferences: () -> Unit,
    onAddReferences: () -> Unit,
    onFlashModeChange: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .padding(top = 34.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
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
                Surface(
                    modifier = Modifier.clickable(onClick = onAddReferences),
                    shape = RoundedCornerShape(6.dp),
                    color = PanelBlack,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("기준 사진 등록", fontSize = 14.sp)
                    }
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
            contentDescription = when (flashMode) {
                ImageCapture.FLASH_MODE_AUTO -> "플래시 자동"
                ImageCapture.FLASH_MODE_ON -> "플래시 켬"
                else -> "플래시 끔"
            },
            onClick = onFlashModeChange,
        )
        CameraIconButton(
            icon = { Icon(Icons.Outlined.Cameraswitch, contentDescription = null) },
            contentDescription = "카메라 전환",
            onClick = onSwitchCamera,
        )
    }
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
    captureRunning: Boolean,
    lastCaptureUri: Uri?,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostOpacityChangeFinished: () -> Unit,
    onCapture: () -> Unit,
    onOpenLastCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(visible = hasReference) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(6.dp),
                color = PanelBlack,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = GhostTeal,
                    )
                    Spacer(Modifier.width(10.dp))
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
                captureRunning = captureRunning,
                onClick = onCapture,
            )

            Spacer(Modifier.size(58.dp))
        }
    }
}

@Composable
private fun ShutterButton(
    captureRunning: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .semantics {
                contentDescription = "사진 촬영"
                role = Role.Button
            }
            .clickable(
                enabled = !captureRunning,
                role = Role.Button,
                onClick = onClick,
            )
            .background(Color.White, CircleShape)
            .padding(6.dp)
            .background(CaptureAmber, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (captureRunning) {
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
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = PanelBlack,
            contentColor = SoftWhite,
        ),
    ) { icon() }
}

@Composable
private fun LastCapturePreview(
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
                .padding(top = 34.dp, start = 8.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "카메라로 돌아가기")
            }
            Text(
                text = "최근 촬영",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = GhostTeal,
            )
            Spacer(Modifier.width(8.dp))
            Text("갤러리에 저장됨", fontSize = 13.sp)
        }
    }
}

@Composable
private fun CameraPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBlack)
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
