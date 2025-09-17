package com.vana.inspection

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vana.inspection.data.ProjectInfo
import com.vana.inspection.data.UploadTarget
import com.vana.inspection.ui.screens.CameraScreen
import com.vana.inspection.ui.screens.ProjectSetupScreen
import com.vana.inspection.ui.screens.SettingsScreen
import com.vana.inspection.ui.theme.VanaTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            VanaTheme {
                VanaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun VanaApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navigationItems = listOf(
        BottomDestination.Project,
        BottomDestination.Capture,
        BottomDestination.Settings
    )

    Scaffold(
        bottomBar = {
            VanaBottomBar(navController, navigationItems)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomDestination.Project.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomDestination.Project.route) {
                ProjectSetupScreen(
                    projectInfo = uiState.projectInfo,
                    onProjectInfoChanged = { updated: ProjectInfo -> viewModel.setProjectInfo(updated) },
                    onProceedToCapture = {
                        navController.navigate(BottomDestination.Capture.route)
                    }
                )
            }
            composable(BottomDestination.Capture.route) {
                CameraScreen(
                    uiState = uiState,
                    onPrepareCapture = viewModel::prepareCapture,
                    onFinalizeCapture = viewModel::finalizeCapture,
                    onCaptureFailed = viewModel::onCaptureFailed,
                    onRetryUpload = viewModel::retryUpload,
                    onRemovePhoto = viewModel::removePhoto,
                    onClearError = viewModel::clearCaptureError,
                    onRefreshGallery = viewModel::refreshGallery
                )
            }
            composable(BottomDestination.Settings.route) {
                SettingsScreen(
                    preferences = uiState.preferences,
                    onAutoUploadChanged = viewModel::toggleAutoUpload,
                    onUploadTargetChanged = viewModel::setUploadTarget,
                    onWifiOnlyChanged = viewModel::setWifiOnlyUploads,
                    onKeepLocalCopyChanged = viewModel::setKeepLocalCopy,
                    onIncludeCompassChanged = viewModel::setIncludeCompass
                )
            }
        }
    }
}

private enum class BottomDestination(val route: String, val label: String, @DrawableRes val icon: Int) {
    Project("project", "Project", android.R.drawable.ic_menu_edit),
    Capture("capture", "Capture", android.R.drawable.ic_menu_camera),
    Settings("settings", "Settings", android.R.drawable.ic_menu_manage)
}

@Composable
private fun VanaBottomBar(navController: NavHostController, destinations: List<BottomDestination>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = destination.icon),
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
