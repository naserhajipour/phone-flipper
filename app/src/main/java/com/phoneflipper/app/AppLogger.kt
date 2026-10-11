package com.phoneflipper.app

import android.util.Log

// Utility class to help centralize logging and command logic
object AppLogger {
    private const val TAG = "PhoneFlipper"

    fun info(message: String) {
        Log.i(TAG, message)
    }
}
