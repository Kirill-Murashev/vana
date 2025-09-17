package com.vana.inspection.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vana.inspection.data.AppPreferences
import com.vana.inspection.data.UploadTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onAutoUploadChanged: (Boolean) -> Unit,
    onUploadTargetChanged: (UploadTarget) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onKeepLocalCopyChanged: (Boolean) -> Unit,
    onIncludeCompassChanged: (Boolean) -> Unit
) {
    var targetMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Capture & storage", style = MaterialTheme.typography.titleLarge)

        SettingToggle(
            title = "Auto-upload photos",
            description = "Automatically stage captured photos for upload",
            checked = preferences.autoUploadEnabled,
            onCheckedChange = onAutoUploadChanged
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Upload destination", style = MaterialTheme.typography.titleMedium)

                ExposedDropdownMenuBox(
                    expanded = targetMenuExpanded,
                    onExpandedChange = { targetMenuExpanded = it }
                ) {
                    TextField(
                        value = preferences.uploadTarget.label,
                        onValueChange = {},
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Destination") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetMenuExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = targetMenuExpanded,
                        onDismissRequest = { targetMenuExpanded = false }
                    ) {
                        UploadTarget.values().forEach { option ->
                            ListItem(
                                headlineContent = { Text(option.label) },
                                supportingContent = { Text(option.description) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUploadTargetChanged(option)
                                        targetMenuExpanded = false
                                    }
                            )
                        }
                    }
                }

                SettingToggle(
                    title = "Wi-Fi only",
                    description = "Delay uploads until connected to Wi-Fi",
                    checked = preferences.wifiOnlyUploads,
                    onCheckedChange = onWifiOnlyChanged,
                    enabled = preferences.autoUploadEnabled
                )

                SettingToggle(
                    title = "Keep a local copy",
                    description = "Retain the photo in device storage after upload",
                    checked = preferences.keepLocalCopy,
                    onCheckedChange = onKeepLocalCopyChanged
                )
            }
        }

        SettingToggle(
            title = "Include compass direction",
            description = "Add the device azimuth to the watermark when available",
            checked = preferences.includeCompassDirection,
            onCheckedChange = onIncludeCompassChanged
        )
    }
}

private val UploadTarget.label: String
    get() = when (this) {
        UploadTarget.MANUAL -> "Manual archive"
        UploadTarget.GOOGLE_DRIVE -> "Google Drive"
        UploadTarget.CORPORATE_SERVER -> "Corporate server"
    }

private val UploadTarget.description: String
    get() = when (this) {
        UploadTarget.MANUAL -> "Leave transfer to a manual workflow"
        UploadTarget.GOOGLE_DRIVE -> "Copy photos into the Drive export staging folder"
        UploadTarget.CORPORATE_SERVER -> "Copy photos into the corporate staging folder"
    }

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
