package dev.arkbuilders.drop.app.navigation

sealed class DropDestination(val route: String) {
    object Home : DropDestination("home")
    object Send : DropDestination("send")
    object History : DropDestination("history")
    object Settings : DropDestination("settings")
    object EditProfile : DropDestination("edit_profile")
    object Receive : DropDestination("receive?ticket={ticket}&confirmations={confirmations}") {
        const val deepLinkPattern = "drop://receive?ticket={ticket}&confirmations={confirmations}"
        
        fun createRoute(ticket: String = "", confirmations: List<UByte> = emptyList()): String {
            val confirmationsStr = confirmations.joinToString(",")
            return "receive?ticket=$ticket&confirmations=$confirmationsStr"
        }
    }
}
