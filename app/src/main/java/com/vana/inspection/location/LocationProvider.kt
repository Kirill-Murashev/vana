package com.vana.inspection.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vana.inspection.util.awaitOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = withContext(Dispatchers.IO) {
        val lastKnown = fusedClient.lastLocation.awaitOrNull()
        if (lastKnown != null) return@withContext lastKnown

        val tokenSource = CancellationTokenSource()
        return@withContext fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).awaitOrNull()
    }
}
