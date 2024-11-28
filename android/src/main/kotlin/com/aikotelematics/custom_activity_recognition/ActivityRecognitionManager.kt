package com.aikotelematics.custom_activity_recognition

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.EventChannel

class ActivityRecognitionManager(private val context: Context) : EventChannel.StreamHandler {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private var eventSinkInstance: EventChannel.EventSink? = null
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSinkInstance = events
        ActivityRecognitionReceiver.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSinkInstance = null
        ActivityRecognitionReceiver.eventSink = null
    }

    fun isAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR)
    }

    fun requestPermissions(activity: Activity, callback: (Boolean) -> Unit) {

        val permissionsToRequest = mutableListOf<String>()

        if (SDK_INT >= VERSION_CODES.Q) {
            if (!hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (SDK_INT >= VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            callback(false)
        } else {
            callback(true)
        }
    }

    fun startTracking(callback: (Boolean) -> Unit) {

        if (!hasRequiredPermissions()) {
            callback(false)
            return
        }

        eventSinkInstance?.let {
            ActivityRecognitionReceiver.eventSink = it
        }

        if (SDK_INT >= VERSION_CODES.O) {
            context.startForegroundService(Intent(context, ActivityRecognitionService::class.java))
        } else {
            context.startService(Intent(context, ActivityRecognitionService::class.java))
        }
        callback(true)
    }

    fun stopTracking(callback: (Boolean) -> Unit) {
        context.stopService(Intent(context, ActivityRecognitionService::class.java))
        callback(true)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasRequiredPermissions(): Boolean {

        var hasPermissions = true

        if (SDK_INT >= VERSION_CODES.Q) {
            hasPermissions = hasPermissions && hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (SDK_INT >= VERSION_CODES.TIRAMISU) {
            hasPermissions = hasPermissions && hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        }

        return hasPermissions
    }
}