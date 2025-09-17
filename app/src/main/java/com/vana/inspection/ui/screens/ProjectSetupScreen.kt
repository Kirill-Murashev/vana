package com.vana.inspection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vana.inspection.data.ProjectInfo

@Composable
fun ProjectSetupScreen(
    projectInfo: ProjectInfo,
    onProjectInfoChanged: (ProjectInfo) -> Unit,
    onProceedToCapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Project Details")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = projectInfo.projectName,
            onValueChange = { onProjectInfoChanged(projectInfo.copy(projectName = it)) },
            label = { Text("Project name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = projectInfo.clientName,
            onValueChange = { onProjectInfoChanged(projectInfo.copy(clientName = it)) },
            label = { Text("Client name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = projectInfo.valuerName,
            onValueChange = { onProjectInfoChanged(projectInfo.copy(valuerName = it)) },
            label = { Text("Appraiser / valuer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = projectInfo.companyName,
            onValueChange = { onProjectInfoChanged(projectInfo.copy(companyName = it)) },
            label = { Text("Appraisal company") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onProceedToCapture,
            enabled = projectInfo.isReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open camera")
        }
    }
}
