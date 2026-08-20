package com.wootan.ghostcamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.wootan.ghostcamera.ui.CameraScreen
import com.wootan.ghostcamera.ui.GhostCameraTheme
import com.wootan.ghostcamera.ui.ReferenceManagerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            onSelectedIndexChange = { selectedIndex = it },
            onGhostOpacityChange = { ghostOpacity = it },
            onGhostOpacityChangeFinished = {
                preferences.edit().putFloat("ghost_opacity", ghostOpacity).apply()
            },
            onManageReferences = { showReferenceManager = true },
            onAddReferences = ::openPhotoPicker,
            onRequestCameraPermission = ::requestCameraPermission,
        )
    }
}
