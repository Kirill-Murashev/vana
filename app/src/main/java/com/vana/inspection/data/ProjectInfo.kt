package com.vana.inspection.data

data class ProjectInfo(
    val projectName: String = "",
    val clientName: String = "",
    val valuerName: String = "",
    val companyName: String = ""
) {
    val isReady: Boolean
        get() = projectName.isNotBlank() && valuerName.isNotBlank()
}
