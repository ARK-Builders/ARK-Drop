@file:OptIn(ExperimentalForeignApi::class)

package dev.arkbuilders.drop.data.helper

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
actual class PermissionsHelper {

    actual fun isCameraGranted(): Boolean {
        return when (
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        ) {
            AVAuthorizationStatusAuthorized -> true
            else -> false
        }
    }

    actual fun isWritePermissionGranted(): Boolean {
        // iOS photo/library write access is managed via Photos framework
        // App sandbox writes (Documents/tmp) do not require permission
        return true
    }
}
