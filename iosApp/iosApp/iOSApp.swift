import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Initialize app configuration and DI
        AppConfiguration.shared.initialize()
    }
    
    var body: some Scene {
        WindowGroup {
            AppRootView()
        }
    }
}

struct AppRootView: View {
    @StateObject private var coordinator = NavigationCoordinator()
    
    var body: some View {
        HomeView()
            .environmentObject(coordinator)
            .preferredColorScheme(.light) // Force light mode for consistency
    }
}
