package dev.arkbuilders.drop.app.domain

import android.net.Uri

interface ResourcesHelper {
    fun getFileName(uri: Uri): String?
}