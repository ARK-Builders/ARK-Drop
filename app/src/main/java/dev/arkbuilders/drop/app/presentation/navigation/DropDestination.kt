package dev.arkbuilders.drop.app.presentation.navigation

sealed class DropDestination(val route: String) {
    object Home : DropDestination("home")

    object Send : DropDestination("send")

    object History : DropDestination("history")

    object EditProfile : DropDestination("edit_profile")

    object About : DropDestination("about")

    object Receive : DropDestination("receive?ticket={ticket}&confirmation={confirmation}") {
        const val DEEP_LINK_PATTERN = "drop://receive?ticket={ticket}&confirmation={confirmation}"

        fun createRoute(
            ticket: String = "",
            confirmation: UByte,
        ): String {
            return "receive?ticket=$ticket&confirmation=$confirmation"
        }
    }
}
