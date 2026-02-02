import Foundation
import Shared

/// Dependency Injection Container
/// Provides access to KMP Koin dependencies
@MainActor
class DIContainer {
    static let shared = DIContainer()
    
    private init() {}
    
    // MARK: - ViewModels
    
    func makeHomeViewModel() -> HomeViewModel {
        return KoinHelper.shared.getHomeViewModel()
    }
    
    func makeSendViewModel() -> SendViewModel {
        return KoinHelper.shared.getSendViewModel()
    }
    
    func makeReceiveViewModel() -> ReceiveViewModel {
        return KoinHelper.shared.getReceiveViewModel()
    }
    
    func makeEditProfileViewModel() -> EditProfileViewModel {
        return KoinHelper.shared.getEditProfileViewModel()
    }
    
    func makeHistoryViewModel() -> HistoryViewModel {
        return KoinHelper.shared.getHistoryViewModel()
    }
}

// Note: We'll need to create a KoinHelper.kt in the shared module
// to expose the Koin dependencies to iOS
