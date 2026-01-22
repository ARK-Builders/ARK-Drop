package dev.arkbuilders.drop.data.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

actual class PermissionsHelper(
    private val ctx: Context,
) {
    actual fun isCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual fun isWritePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
