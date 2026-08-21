package com.wootan.ghostcamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.wootan.ghostcamera.data.ReferencePhoto
import com.wootan.ghostcamera.data.ReferenceStore
import com.wootan.ghostcamera.camera.CameraRotation
import com.wootan.ghostcamera.ui.BodyGuideMode
import com.wootan.ghostcamera.ui.BodyGuidePositions
import com.wootan.ghostcamera.ui.CameraScreen
import com.wootan.ghostcamera.ui.GhostScaleMode
import com.wootan.ghostcamera.ui.GhostCameraTheme
import com.wootan.ghostcamera.ui.ReferenceManagerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BODY_GUIDE_ENABLED_KEY = "body_guide_enabled"
private const val BODY_GUIDE_SHOULDER_KEY = "body_guide_shoulder"
private const val BODY_GUIDE_CHEST_KEY = "body_guide_chest"
private const val BODY_GUIDE_PELVIS_KEY = "body_guide_pelvis"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            GhostCameraTheme {
                GhostCameraApp()
            }
        }
    }
}

@Composable
private fun GhostCameraApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ReferenceStore(context.applicationContext) }
    val preferences = remember {
        context.getSharedPreferences("camera_preferences", Context.MODE_PRIVATE)
    }

    var references by remember { mutableStateOf(store.load()) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showReferenceManager by rememberSaveable { mutableStateOf(false) }
    var referenceOperationRunning by remember { mutableStateOf(false) }
    var ghostOpacity by rememberSaveable {
        mutableFloatStateOf(preferences.getFloat("ghost_opacity", 0.45f))
    }
    var ghostScaleMode by rememberSaveable {
        mutableStateOf(
            GhostScaleMode.fromPreference(preferences.getString("ghost_scale_mode", null)),
        )
    }
    var cameraRotationQuarterTurns by rememberSaveable {
        mutableIntStateOf(
            CameraRotation.normalize(preferences.getInt("camera_rotation_quarter_turns", 0)),
        )
    }
    val storedBodyGuidePositions = remember(preferences) {
        BodyGuidePositions.fromStored(
            shoulder = preferences.getFloat(
                BODY_GUIDE_SHOULDER_KEY,
                BodyGuidePositions.Default.shoulder,
            ),
            chest = preferences.getFloat(
                BODY_GUIDE_CHEST_KEY,
                BodyGuidePositions.Default.chest,
            ),
            pelvis = preferences.getFloat(
                BODY_GUIDE_PELVIS_KEY,
                BodyGuidePositions.Default.pelvis,
            ),
        )
    }
    var bodyGuideMode by rememberSaveable {
        mutableStateOf(
            if (preferences.getBoolean(BODY_GUIDE_ENABLED_KEY, false)) {
                BodyGuideMode.Locked
            } else {
                BodyGuideMode.Off
            },
        )
    }
    var bodyGuideShoulder by rememberSaveable {
        mutableFloatStateOf(storedBodyGuidePositions.shoulder)
    }
    var bodyGuideChest by rememberSaveable {
        mutableFloatStateOf(storedBodyGuidePositions.chest)
    }
    var bodyGuidePelvis by rememberSaveable {
        mutableFloatStateOf(storedBodyGuidePositions.pelvis)
    }
    val bodyGuidePositions = BodyGuidePositions.fromStored(
        shoulder = bodyGuideShoulder,
        chest = bodyGuideChest,
        pelvis = bodyGuidePelvis,
    )

    fun setBodyGuidePositions(positions: BodyGuidePositions) {
        bodyGuideShoulder = positions.shoulder
        bodyGuideChest = positions.chest
        bodyGuidePelvis = positions.pelvis
    }

    fun persistBodyGuidePositions(positions: BodyGuidePositions) {
        preferences.edit()
            .putFloat(BODY_GUIDE_SHOULDER_KEY, positions.shoulder)
            .putFloat(BODY_GUIDE_CHEST_KEY, positions.chest)
            .putFloat(BODY_GUIDE_PELVIS_KEY, positions.pelvis)
            .apply()
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
        permissionRequested = true
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            referenceOperationRunning = true
            references = withContext(Dispatchers.IO) { store.add(uris) }
            selectedIndex = selectedIndex.coerceIn(0, (references.lastIndex).coerceAtLeast(0))
            referenceOperationRunning = false
        }
    }

    fun openPhotoPicker() {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    fun requestCameraPermission() {
        permissionRequested = true
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!cameraPermissionGranted && !permissionRequested) requestCameraPermission()
    }

    if (showReferenceManager) {
        BackHandler { showReferenceManager = false }
        ReferenceManagerScreen(
            references = references,
            loading = referenceOperationRunning,
            onBack = { showReferenceManager = false },
            onAdd = ::openPhotoPicker,
            onSelect = { index ->
                selectedIndex = index
                showReferenceManager = false
            },
            onDelete = { photo ->
                scope.launch {
                    referenceOperationRunning = true
                    references = withContext(Dispatchers.IO) { store.delete(photo) }
                    selectedIndex = selectedIndex.coerceIn(
                        0,
                        references.lastIndex.coerceAtLeast(0),
                    )
                    referenceOperationRunning = false
                }
            },
            onMove = { from, to ->
                scope.launch {
                    references = withContext(Dispatchers.IO) { store.move(from, to) }
                    selectedIndex = when (selectedIndex) {
                        from -> to
                        to -> from
                        else -> selectedIndex
                    }
                }
            },
        )
    } else {
        CameraScreen(
            cameraPermissionGranted = cameraPermissionGranted,
            references = references,
            selectedIndex = selectedIndex.coerceIn(
                0,
                references.lastIndex.coerceAtLeast(0),
            ),
            ghostOpacity = ghostOpacity,
            ghostScaleMode = ghostScaleMode,
            cameraRotationQuarterTurns = cameraRotationQuarterTurns,
            bodyGuideMode = bodyGuideMode,
            bodyGuidePositions = bodyGuidePositions,
            onSelectedIndexChange = { selectedIndex = it },
            onGhostOpacityChange = { ghostOpacity = it },
            onGhostOpacityChangeFinished = {
                preferences.edit().putFloat("ghost_opacity", ghostOpacity).apply()
            },
            onGhostScaleModeChange = { mode ->
                ghostScaleMode = mode
                preferences.edit()
                    .putString("ghost_scale_mode", mode.preferenceValue)
                    .apply()
            },
            onRotateCamera = {
                cameraRotationQuarterTurns = CameraRotation.next(cameraRotationQuarterTurns)
                preferences.edit()
                    .putInt("camera_rotation_quarter_turns", cameraRotationQuarterTurns)
                    .apply()
            },
            onBodyGuideModeChange = { mode ->
                bodyGuideMode = mode
                preferences.edit()
                    .putBoolean(BODY_GUIDE_ENABLED_KEY, mode != BodyGuideMode.Off)
                    .apply()
            },
            onBodyGuidePositionsChange = ::setBodyGuidePositions,
            onBodyGuidePositionsChangeFinished = { positions ->
                setBodyGuidePositions(positions)
                persistBodyGuidePositions(positions)
            },
            onResetBodyGuidePositions = {
                setBodyGuidePositions(BodyGuidePositions.Default)
                persistBodyGuidePositions(BodyGuidePositions.Default)
            },
            onManageReferences = { showReferenceManager = true },
            onAddReferences = ::openPhotoPicker,
            onRequestCameraPermission = ::requestCameraPermission,
        )
    }
}
