package com.vana.inspection.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.vana.inspection.AppUiState
import com.vana.inspection.AppViewModel
import com.vana.inspection.data.PhotoRecord
import com.vana.inspection.data.UploadState
import com.vana.inspection.data.UploadTarget
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    uiState: AppUiState,
    onPrepareCapture: suspend () -> AppViewModel.CapturePreparation?,
    onFinalizeCapture: (AppViewModel.CapturePreparation) -> Unit,
    onCaptureFailed: (String?) -> Unit,
    onRetryUpload: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onClearError: () -> Unit,
    onRefreshGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    val requiredPermissions = remember {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap ->
        val allGranted = grantedMap.values.all { it }
        if (!allGranted) {
            onCaptureFailed("Camera and location permissions are required.")
        }
    }

    val hasPermissions = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    LaunchedEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    uiState.captureError?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (hasPermissions) {
                    CameraPreview(controller = cameraController)
                    CaptureOverlay(
                        projectInfoSummary = overlaySummary(uiState),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                } else {
                    PermissionExplanation(onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                    })
                }

                CaptureButton(
                    enabled = hasPermissions && !uiState.isCapturing,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    coroutineScope.launch {
                        val preparation = onPrepareCapture() ?: return@launch
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(preparation.file).build()
                        val executor = ContextCompat.getMainExecutor(context)
                        cameraController.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onError(exception: ImageCaptureException) {
                                    preparation.file.delete()
                                    onCaptureFailed(exception.message)
                                }

                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onFinalizeCapture(preparation)
                                    onRefreshGallery()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PhotoGallery(
                photos = uiState.photos,
                onRetryUpload = onRetryUpload,
                onRemovePhoto = onRemovePhoto
            )
        }
    }
}

@Composable
private fun CameraPreview(controller: LifecycleCameraController) {
    val context = LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        factory = {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CaptureOverlay(projectInfoSummary: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            )
        ) {
            Text(
                text = projectInfoSummary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun CaptureButton(enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.padding(bottom = 24.dp)
    ) {
        Icon(painter = painterResource(id = android.R.drawable.ic_menu_camera), contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = if (enabled) "Capture" else "Hold on...")
    }
}

@Composable
private fun PermissionExplanation(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Camera and precise location permissions are required to capture appraisal photos.")
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = onRequestPermissions) {
            Text(text = "Grant permissions")
        }
    }
}

@Composable
private fun PhotoGallery(
    photos: List<PhotoRecord>,
    onRetryUpload: (String) -> Unit,
    onRemovePhoto: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recent photos", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            items(photos, key = { it.id }) { record ->
                PhotoListItem(
                    record = record,
                    onRetryUpload = onRetryUpload,
                    onRemovePhoto = onRemovePhoto
                )
            }
        }
    }
}

@Composable
private fun PhotoListItem(
    record: PhotoRecord,
    onRetryUpload: (String) -> Unit,
    onRemovePhoto: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = record.filePath,
                contentDescription = record.fileName,
                modifier = Modifier
                    .height(64.dp)
                    .width(64.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = record.projectInfo.projectName.ifBlank { record.fileName })
                Text(
                    text = record.timestamp.toString(),
                    style = MaterialTheme.typography.bodySmall
                )
                val coords = if (record.latitude != null && record.longitude != null) {
                    "${"%.5f".format(record.latitude)}, ${"%.5f".format(record.longitude)}"
                } else {
                    "Coordinates pending"
                }
                Text(text = coords, style = MaterialTheme.typography.bodySmall)
                val statusText = when (record.uploadState) {
                    UploadState.UPLOADED -> "Uploaded"
                    UploadState.UPLOADING -> "Uploading..."
                    UploadState.FAILED -> "Upload failed"
                    UploadState.PENDING -> if (record.uploadTarget == UploadTarget.MANUAL) "Manual" else "Scheduled"
                }
                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (record.uploadState == UploadState.FAILED ||
                    (record.uploadState == UploadState.PENDING && record.uploadTarget != UploadTarget.MANUAL)
                ) {
                    FilledTonalButton(onClick = { onRetryUpload(record.id) }) {
                        Text("Retry")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = { onRemovePhoto(record.id) }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_delete),
                        contentDescription = "Remove"
                    )
                }
            }
        }
    }
}

private fun overlaySummary(uiState: AppUiState): String = buildString {
    appendLine(uiState.projectInfo.projectName.ifBlank { "Project not set" })
    if (uiState.projectInfo.clientName.isNotBlank()) appendLine("Client: ${uiState.projectInfo.clientName}")
    appendLine("Valuer: ${uiState.projectInfo.valuerName.ifBlank { "—" }}")
    if (uiState.preferences.autoUploadEnabled) {
        append("Auto upload → ${uiState.preferences.uploadTarget.name}")
        if (uiState.preferences.wifiOnlyUploads) append(" (Wi-Fi)")
    } else {
        append("Auto upload disabled")
    }
}
