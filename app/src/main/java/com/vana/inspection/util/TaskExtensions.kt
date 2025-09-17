package com.vana.inspection.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitOrNull(): T? = try {
    await()
} catch (_: Exception) {
    null
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        when {
            task.isCanceled -> cont.cancel()
            task.isSuccessful -> cont.resume(task.result)
            else -> cont.resumeWithException(task.exception ?: CancellationException())
        }
    }
}
