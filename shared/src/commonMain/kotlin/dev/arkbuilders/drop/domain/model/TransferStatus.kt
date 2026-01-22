package dev.arkbuilders.drop.domain.model

enum class TransferStatus {
    COMPLETED,
    FAILED,
    CANCELLED,
}

enum class TransferType {
    SENT,
    RECEIVED,
}
