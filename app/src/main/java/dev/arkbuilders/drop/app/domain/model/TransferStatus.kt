package dev.arkbuilders.drop.app.domain.model

enum class TransferStatus {
    COMPLETED, FAILED, CANCELLED
}

enum class TransferType {
    SENT, RECEIVED
}