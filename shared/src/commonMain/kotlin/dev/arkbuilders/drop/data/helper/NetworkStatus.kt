package dev.arkbuilders.drop.data.helper

import kotlinx.coroutines.flow.StateFlow

expect class NetworkStatus {
    fun isOnline(): Boolean

    val onlineStatus: StateFlow<Boolean>
}
