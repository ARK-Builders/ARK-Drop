package dev.arkbuilders.drop.app.navigation

sealed class DropDestination(val route: String) {
    object Home : DropDestination("home")
    object Send : DropDestination("send?uris={uris}")
    object History : DropDestination("history")
    object Settings : DropDestination("settings")
    object EditProfile : DropDestination("edit_profile")
    object Receive : DropDestination("receive?ticket={ticket}&confirmations={confirmations}") {
        const val deepLinkPattern = "drop://receive?ticket={ticket}&confirmations={confirmations}"
    }
}