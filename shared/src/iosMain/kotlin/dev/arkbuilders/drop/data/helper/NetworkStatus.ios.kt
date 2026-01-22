package dev.arkbuilders.drop.data.helper

import kotlinx.coroutines.flow.StateFlow

actual class NetworkStatus {
    actual fun isOnline(): Boolean {
        throw NotImplementedError()
    }

    actual val onlineStatus: StateFlow<Boolean>
        get() {
            throw NotImplementedError()
        }
}
