import SwiftUI
import Shared
import Combine

struct EditProfileView: View {
    @StateObject private var viewModel = EditProfileViewModelWrapper()
    @EnvironmentObject private var coordinator: NavigationCoordinator
    @State private var showAvatarPicker = false
    
    var body: some View {
        ZStack {
            Color.dropBackground
                .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: Spacing.lg) {
                    // Avatar Selection
                    VStack(spacing: Spacing.md) {
                        AvatarView(
                            avatarBase64: viewModel.state.avatar.base64,
                            size: 120
                        )
                        
                        DropButton(
                            title: "Change Avatar",
                            action: { showAvatarPicker = true },
                            style: .outline
                        )
                        .frame(maxWidth: 200)
                    }
                    .padding(.vertical, Spacing.lg)
                    
                    // Name Input
                    VStack(alignment: .leading, spacing: Spacing.sm) {
                        Text("Name")
                            .font(AppTypography.labelLarge)
                            .foregroundColor(.secondary)
                        
                        TextField("Enter your name", text: Binding(
                            get: { viewModel.state.name },
                            set: { viewModel.onNameChanged($0) }
                        ))
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .font(AppTypography.bodyLarge)
                    }
                    
                    Spacer(minLength: Spacing.xl)
                    
                    // Actions
                    VStack(spacing: Spacing.sm) {
                        DropButton(
                            title: "Save Changes",
                            action: viewModel.onSave,
                            style: .primary,
                            isEnabled: viewModel.state.hasChanges
                        )
                        
                        Button("About") {
                            coordinator.navigate(to: .about)
                        }
                        .font(AppTypography.labelMedium)
                        .foregroundColor(.dropPrimary)
                    }
                }
                .padding(Spacing.lg)
            }
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showAvatarPicker) {
            AvatarPickerView(
                currentAvatar: viewModel.state.avatar.base64,
                onSelect: viewModel.onAvatarSelected
            )
        }
        .onReceive(viewModel.effectPublisher) { effect in
            if effect is EditProfileScreenEffect.NavigateBack {
                coordinator.navigateBack()
            }
        }
    }
}

// MARK: - Avatar Picker

struct AvatarPickerView: View {
    let currentAvatar: String?
    let onSelect: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    
    // Available avatar resources (matching Android drawables)
    private let avatarIds = ["avatar_00", "avatar_01", "avatar_02", "avatar_03", 
                             "avatar_04", "avatar_05", "avatar_06", "avatar_07", "avatar_08"]
    
    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: [
                    GridItem(.adaptive(minimum: 80), spacing: Spacing.md)
                ], spacing: Spacing.md) {
                    ForEach(avatarIds, id: \.self) { avatarId in
                        if let image = UIImage(named: avatarId) {
                            AvatarGridItem(
                                image: image,
                                isSelected: false,
                                onTap: {
                                    if let base64 = image.pngData()?.base64EncodedString() {
                                        onSelect(base64)
                                        dismiss()
                                    }
                                }
                            )
                        }
                    }
                }
                .padding(Spacing.md)
            }
            .navigationTitle("Choose Avatar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }
}

struct AvatarGridItem: View {
    let image: UIImage
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 80, height: 80)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(isSelected ? Color.dropPrimary : Color.clear, lineWidth: 3)
                )
        }
    }
}

// MARK: - ViewModel Wrapper

@MainActor
class EditProfileViewModelWrapper: ObservableObject {
    @Published private(set) var state: EditProfileScreenState
    let effectPublisher = PassthroughSubject<EditProfileScreenEffect, Never>()
    
    private let viewModel: EditProfileViewModel
    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?
    
    init() {
        self.viewModel = DIContainer.shared.makeEditProfileViewModel()
        self.state = EditProfileScreenState(
            currentProfile: UserProfile.companion.empty(),
            name: "",
            nameError: nil,
            avatar: UserAvatar(base64: "", predefinedId: nil),
            avatarImageLoadingFailed: false,
            hasChanges: false
        )
        observeState()
        observeEffects()
    }
    
    private func observeState() {
        stateTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await newState in viewModel.container.stateFlow {
                    self.state = newState as! EditProfileScreenState
                }
            } catch {
                print("EditProfileViewModel state error: \(error)")
            }
        }
    }
    
    private func observeEffects() {
        effectTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await effect in viewModel.container.sideEffectFlow {
                    if let typedEffect = effect as? EditProfileScreenEffect {
                        self.effectPublisher.send(typedEffect)
                    }
                }
            } catch {
                print("EditProfileViewModel effect error: \(error)")
            }
        }
    }
    
    func onNameChanged(_ name: String) {
        viewModel.onNameChanged(newName: name)
    }
    
    func onAvatarSelected(_ base64: String) {
        viewModel.onAvatarSelected(id: base64)
    }
    
    func onSave() {
        viewModel.onSave()
    }
    
    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }
}

#Preview {
    NavigationStack {
        EditProfileView()
            .environmentObject(NavigationCoordinator())
    }
}
