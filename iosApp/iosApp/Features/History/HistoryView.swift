import SwiftUI
import Shared

struct HistoryView: View {
    @StateObject private var viewModel = HistoryViewModelWrapper()
    
    var body: some View {
        ZStack {
            Color.dropBackground
                .ignoresSafeArea()
            
            if viewModel.state.historyItems.isEmpty {
                EmptyStateView(
                    icon: "clock",
                    title: "No History",
                    message: "Your transfer history will appear here"
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: Spacing.md) {
                        ForEach(viewModel.state.historyItems, id: \.id) { item in
                            HistoryDetailCard(item: item)
                        }
                    }
                    .padding(Spacing.md)
                }
            }
        }
        .navigationTitle("Transfer History")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !viewModel.state.historyItems.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    Button("Clear All") {
                        viewModel.onClearHistory()
                    }
                    .foregroundColor(.dropError)
                }
            }
        }
    }
}

// MARK: - History Detail Card

struct HistoryDetailCard: View {
    let item: TransferSession
    
    var body: some View {
        DropCard {
            VStack(alignment: .leading, spacing: Spacing.md) {
                // Header
                HStack {
                    Image(systemName: item.type == .sent ? "arrow.up.circle.fill" : "arrow.down.circle.fill")
                        .font(.system(size: 40))
                        .foregroundColor(item.type == .sent ? .dropSending : .dropReceiving)
                    
                    VStack(alignment: .leading, spacing: Spacing.xxs) {
                        Text(item.type == .sent ? "Sent" : "Received")
                            .font(AppTypography.titleMedium)
                        
                        Text(formatDate(item.timestamp.toEpochMilliseconds()))
                            .font(AppTypography.bodySmall)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                }
                
                Divider()
                
                // Files
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    Text("Files (\(item.files.count))")
                        .font(AppTypography.labelMedium)
                        .foregroundColor(.secondary)
                    
                    ForEach(Array(item.files), id: \.name) { file in
                        HStack {
                            Image(systemName: "doc")
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                            
                            Text(file.name)
                                .font(AppTypography.bodySmall)
                                .lineLimit(1)
                        }
                    }
                }
            }
        }
    }
    
    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

// MARK: - ViewModel Wrapper

@MainActor
class HistoryViewModelWrapper: ObservableObject {
    @Published private(set) var state: HistoryScreenState
    
    private let viewModel: HistoryViewModel
    private var stateTask: Task<Void, Never>?
    
    init() {
        self.viewModel = DIContainer.shared.makeHistoryViewModel()
        self.state = HistoryScreenState(
            historyItems: [],
            showClearDialog: false,
            showDeleteDialog: false
        )
        observeState()
    }
    
    private func observeState() {
        stateTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await newState in viewModel.container.stateFlow {
                    self.state = newState as! HistoryScreenState
                }
            } catch {
                print("HistoryViewModel state error: \(error)")
            }
        }
    }
    
    func onClearHistory() {
        viewModel.onClear()
    }
    
    deinit {
        stateTask?.cancel()
    }
}

#Preview {
    NavigationStack {
        HistoryView()
    }
}
