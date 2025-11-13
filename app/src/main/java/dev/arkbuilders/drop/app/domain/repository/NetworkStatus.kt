package dev.arkbuilders.drop.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface NetworkStatus {
    fun isOnline() = onlineStatus.value

    val onlineStatus: StateFlow<Boolean>
}
