package dev.arkbuilders.drop.app.domain.model

data class UserProfile(
    val name: String,
    val avatarB64: String,
    val avatarId: String,
) {
    companion object {
        fun empty() = UserProfile("", "", "")
    }
}