import SwiftUI
import Shared
import Combine

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModelWrapper
    @EnvironmentObject private var coordinator: NavigationCoordinator
    
    init() {
        _viewModel = StateObject(wrappedValue: HomeViewModelWrapper())
    }
    
    var body: some View {
        NavigationStack(path: $coordinator.path) {
            ZStack {
                Color.dropBackground
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Header
                    headerView
                        .padding(.horizontal, Spacing.md)
                        .padding(.vertical, Spacing.md)
                    
                    // Main Content
                    ScrollView {
                        VStack(spacing: Spacing.lg) {
                            // Transfer Actions
                            actionCardsView
                                .padding(.horizontal, Spacing.md)
                            
                            // Recent History
                            if !viewModel.state.historyItems.isEmpty {
                                historySection
                            }
                        }
                        .padding(.vertical, Spacing.md)
                    }
                }
            }
            .navigationTitle("ARK Drop")
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(for: Route.self) { route in
                destinationView(for: route)
            }
            .onReceive(viewModel.effectPublisher) { effect in
                handleEffect(effect)
            }
        }
    }
    
    private func handleEffect(_ effect: HomeScreenEffect) {
        switch effect {
        case is HomeScreenEffect.NavigateToReceiveScreen:
            coordinator.navigate(to: .receive)
            
        case is HomeScreenEffect.AskWritePermission:
            // TODO: Request storage permission if needed on iOS
            break
            
        default:
            break
        }
    }
    
    // MARK: - Header
    
    private var headerView: some View {
        HStack {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text("Hello, \(viewModel.state.profile.name.isEmpty ? "Anonymous" : viewModel.state.profile.name)")
                    .font(AppTypography.titleLarge)
                
                Text("Transfer files securely")
                    .font(AppTypography.bodySmall)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Button(action: { coordinator.navigate(to: .editProfile) }) {
                AvatarView(
                    avatarBase64: viewModel.state.profile.avatar.base64,
                    size: 48
                )
            }
        }
    }
    
    // MARK: - Action Cards
    
    private var actionCardsView: some View {
        HStack(spacing: Spacing.md) {
            ActionCard(
                icon: "arrow.up.circle.fill",
                title: "Send",
                description: "Share files",
                color: .dropSending
            ) {
                coordinator.navigate(to: .send)
            }
            
            ActionCard(
                icon: "arrow.down.circle.fill",
                title: "Receive",
                description: "Get files",
                color: .dropReceiving
            ) {
                viewModel.onReceiveClick()
            }
        }
    }
    
    // MARK: - History
    
    private var historySection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                Text("Recent Transfers")
                    .font(AppTypography.titleMedium)
                    .foregroundColor(.primary)
                
                Spacer()
                
                Button("See All") {
                    coordinator.navigate(to: .history)
                }
                .font(AppTypography.labelMedium)
                .foregroundColor(.dropPrimary)
            }
            .padding(.horizontal, Spacing.md)
            
            ForEach(Array(viewModel.state.historyItems.prefix(3)), id: \.id) { item in
                HistoryItemRow(item: item)
                    .padding(.horizontal, Spacing.md)
            }
        }
    }
    
    // MARK: - Navigation
    
    @ViewBuilder
    private func destinationView(for route: Route) -> some View {
        switch route {
        case .send:
            SendView()
        case .receive:
            ReceiveView()
        case .history:
            HistoryView()
        case .editProfile:
            EditProfileView()
        case .about:
            AboutView()
        case .home:
            EmptyView()
        }
    }
}

// MARK: - Action Card

struct ActionCard: View {
    let icon: String
    let title: String
    let description: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            DropCard(padding: Spacing.lg) {
                VStack(spacing: Spacing.sm) {
                    Image(systemName: icon)
                        .font(.system(size: 48))
                        .foregroundColor(color)
                    
                    Text(title)
                        .font(AppTypography.titleMedium)
                        .foregroundColor(.primary)
                    
                    Text(description)
                        .font(AppTypography.bodySmall)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - History Item Row

struct HistoryItemRow: View {
    let item: TransferSession
    
    var body: some View {
        DropCard(padding: Spacing.md) {
            HStack(spacing: Spacing.md) {
                Image(systemName: item.type == .sent ? "arrow.up.circle.fill" : "arrow.down.circle.fill")
                    .font(.system(size: 32))
                    .foregroundColor(item.type == .sent ? .dropSending : .dropReceiving)
                
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(item.type == .sent ? "Sent" : "Received")
                        .font(AppTypography.labelMedium)
                        .foregroundColor(.secondary)
                    
                    Text("\(item.files.count) file(s)")
                        .font(AppTypography.titleSmall)
                        .foregroundColor(.primary)
                    
                    Text(formatDate(item.timestamp.toEpochMilliseconds()))
                        .font(AppTypography.bodySmall)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
        }
    }
    
    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000)
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - ViewModel Wrapper

@MainActor
class HomeViewModelWrapper: ObservableObject {
    @Published private(set) var state: HomeScreenState
    private let viewModel: HomeViewModel
    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?
    
    let effectPublisher = PassthroughSubject<HomeScreenEffect, Never>()
    
    init() {
        self.viewModel = DIContainer.shared.makeHomeViewModel()
        self.state = HomeScreenState(
            historyItems: [],
            profile: UserProfile.companion.empty()
        )
        observeState()
        observeEffects()
    }
    
    private func observeState() {
        stateTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await newState in viewModel.container.stateFlow {
                    self.state = newState as! HomeScreenState
                }
            } catch {
                print("HomeViewModel state error: \(error)")
            }
        }
    }
    
    private func observeEffects() {
        effectTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await effect in viewModel.container.sideEffectFlow {
                    self.effectPublisher.send(effect as! HomeScreenEffect)
                }
            } catch {
                print("HomeViewModel effect error: \(error)")
            }
        }
    }
    
    func onReceiveClick() {
        viewModel.onReceiveClick()
    }
    
    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }
}

#Preview {
    HomeView()
        .environmentObject(NavigationCoordinator())
}
