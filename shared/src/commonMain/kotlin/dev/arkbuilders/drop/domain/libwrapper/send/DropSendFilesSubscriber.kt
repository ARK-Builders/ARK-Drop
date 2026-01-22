package dev.arkbuilders.drop.domain.libwrapper.send

import kotlinx.coroutines.flow.StateFlow

interface DropSendFilesSubscriber {
    val progress: StateFlow<DropSendingProgress>
}
