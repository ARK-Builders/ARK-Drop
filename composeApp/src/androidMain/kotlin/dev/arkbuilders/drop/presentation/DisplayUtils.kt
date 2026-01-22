package dev.arkbuilders.drop.presentation

object DisplayUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "0 B"
        if (bytes < 1024) return "$bytes B"

        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)

        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)

        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }

    fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
