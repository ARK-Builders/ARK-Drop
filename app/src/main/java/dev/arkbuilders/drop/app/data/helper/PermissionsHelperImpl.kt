package dev.arkbuilders.drop.app.data.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dev.arkbuilders.drop.app.domain.PermissionsHelper

class PermissionsHelperImpl(
    private val ctx: Context,
): PermissionsHelper {
    override fun isCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}