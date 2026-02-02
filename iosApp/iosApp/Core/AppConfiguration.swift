import Foundation
import Shared

/// Handles app-wide configuration and initialization
class AppConfiguration {
    static let shared = AppConfiguration()
    
    private init() {}
    
    /// Initialize the app - call this on app launch
    func initialize() {
        initializeKoin()
        configureLogging()
    }
    
    private func initializeKoin() {
        // Initialize Koin dependency injection
        KoinInitializerKt.doInitKoin()
        print("✅ Koin initialized successfully")
    }
    
    private func configureLogging() {
        // Configure logging if needed
        print("✅ App configuration completed")
    }
}
