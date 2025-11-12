package dev.arkbuilders.drop.app.domain.model

data class UserProfile(
    val name: String,
    val avatar: UserAvatar,
) {
    companion object {
        fun empty() = UserProfile("", UserAvatar("", null))
    }
}
