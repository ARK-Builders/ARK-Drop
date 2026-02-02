import SwiftUI

enum Route: Hashable {
    case home
    case send
    case receive
    case history
    case editProfile
    case about
}

@MainActor
class NavigationCoordinator: ObservableObject {
    @Published var path = NavigationPath()
    
    func navigate(to route: Route) {
        path.append(route)
    }
    
    func navigateBack() {
        path.removeLast()
    }
    
    func navigateToRoot() {
        path.removeLast(path.count)
    }
    
    func replace(with route: Route) {
        path.removeLast()
        path.append(route)
    }
}
