package dev.arkbuilders.drop.app.data.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dev.arkbuilders.drop.app.domain.PermissionsHelper

class PermissionsHelperImpl(
    private val ctx: Context,
) : PermissionsHelper {
    override fun isCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isWritePermissionGranted(): Boolean {
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
