package com.example.util

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck

object FirebaseAppCheckLogger {
    private const val TAG = "FirebaseAppCheckLogger"

    fun logStatus(source: String) {
        try {
            FirebaseAppCheck.getInstance()
                .getAppCheckToken(false)
                .addOnSuccessListener { tokenResult ->
                    val hasToken = tokenResult.token.isNotBlank()
                    Log.d(
                        TAG,
                        "[$source] App Check token status: valid=$hasToken, expireTimeMillis=${tokenResult.expireTimeMillis}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "[$source] App Check token fetch failed: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "[$source] App Check initialization check failed: ${e.message}", e)
        }
    }
}
