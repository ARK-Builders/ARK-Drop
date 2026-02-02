# ARK Drop - iOS App

Production-ready SwiftUI implementation using Kotlin Multiplatform ViewModels via SKIE.

## Architecture

### MVVM with KMP ViewModels

```
┌─────────────────────────────────────┐
│         SwiftUI Views               │
│  (HomeView, SendView, ReceiveView)  │
└──────────────┬──────────────────────┘
               │
               │ @Published state
               ▼
┌─────────────────────────────────────┐
│    ViewModel Wrappers               │
│  (Observe KMP ViewModels)           │
└──────────────┬──────────────────────┘
               │
               │ SKIE Bridge
               ▼
┌─────────────────────────────────────┐
│    KMP ViewModels (Orbit MVI)       │
│  Flow<State> → AsyncSequence        │
└─────────────────────────────────────┘
```

### Key Components

#### 1. **Core Architecture**
- `ViewModelObserver.swift` - Generic KMP ViewModel observer
- `DIContainer.swift` - Dependency injection container
- `NavigationCoordinator.swift` - Centralized navigation

#### 2. **Theme System**
- `Colors.swift` - App color palette
- `Typography.swift` - Text styles
- `Spacing.swift` - Layout constants

#### 3. **Reusable Components**
- `DropButton.swift` - Styled button component
- `DropCard.swift` - Card container
- `AvatarView.swift` - Avatar display
- `ProgressBar.swift` - Progress indicator
- `EmptyStateView.swift` - Empty state placeholder
- `ErrorView.swift` - Error display
- `LoadingView.swift` - Loading indicator

#### 4. **Features**

##### Home
- `HomeView.swift` - Main screen with Send/Receive actions
- Shows recent transfer history
- Profile management

##### Send Files
- `SendView.swift` - Send flow coordinator
- `FileSelectionView` - File picker integration
- `WaitingForReceiverView` - QR code display
- `TransferringView` - Transfer progress
- `TransferCompleteView` - Success state

##### Receive Files
- `ReceiveView.swift` - Receive flow coordinator
- `QRScannerView.swift` - Camera-based QR scanner
- `ManualInputView` - Manual code entry
- `ReceivingView` - Receive progress
- `ReceiveSuccessView` - Success state

##### Profile
- `EditProfileView.swift` - Profile editing
- `AvatarPickerView` - Avatar selection
- `AboutView.swift` - App information

##### History
- `HistoryView.swift` - Transfer history list
- `HistoryDetailCard` - History item display

## SKIE Integration

SKIE automatically converts:
- `Flow<T>` → `AsyncSequence` for state/effect streams
- Kotlin coroutines → Swift async/await
- Sealed classes → Swift enums (with associated values)
- Data classes → Swift structs

### Example Usage

```swift
// Observe ViewModel state
for try await newState in viewModel.container.stateFlow {
    self.state = newState as! HomeScreenState
}

// Observe side effects
for try await effect in viewModel.container.sideEffectFlow {
    handleEffect(effect as! HomeScreenEffect)
}
```

## State Management

Each screen follows this pattern:

1. **ViewModel Wrapper** - Observes KMP ViewModel
2. **State** - Published state from KMP
3. **Effects** - Side effects (navigation, etc.)
4. **View** - SwiftUI view that reacts to state

```swift
@MainActor
class HomeViewModelWrapper: ObservableObject {
    @Published private(set) var state: HomeScreenState
    private let viewModel: HomeViewModel
    
    init() {
        self.viewModel = DIContainer.shared.makeHomeViewModel()
        // ... observe state/effects
    }
}
```

## File Structure

```
iosApp/
├── Core/
│   ├── ViewModelObserver.swift
│   ├── DIContainer.swift
│   └── NavigationCoordinator.swift
├── Theme/
│   ├── Colors.swift
│   ├── Typography.swift
│   └── Spacing.swift
├── Components/
│   ├── DropButton.swift
│   ├── DropCard.swift
│   ├── AvatarView.swift
│   ├── ProgressBar.swift
│   ├── EmptyStateView.swift
│   ├── ErrorView.swift
│   └── LoadingView.swift
├── Features/
│   ├── Home/
│   │   └── HomeView.swift
│   ├── Send/
│   │   └── SendView.swift
│   ├── Receive/
│   │   ├── ReceiveView.swift
│   │   └── QRScannerView.swift
│   ├── History/
│   │   └── HistoryView.swift
│   └── Profile/
│       ├── EditProfileView.swift
│       └── AboutView.swift
├── Utilities/
│   └── Extensions.swift
└── iOSApp.swift (main entry)
```

## Best Practices Implemented

### iOS Development
- ✅ MVVM architecture
- ✅ SwiftUI declarative UI
- ✅ Combine for reactive patterns
- ✅ @MainActor for thread safety
- ✅ Property wrappers (@Published, @StateObject, @EnvironmentObject)
- ✅ Proper memory management (weak self, deinit cleanup)

### Design
- ✅ iOS Human Interface Guidelines
- ✅ Adaptive layouts
- ✅ Native iOS components
- ✅ Proper spacing and typography
- ✅ Accessibility support ready

### Code Quality
- ✅ Clear separation of concerns
- ✅ Reusable components
- ✅ Type-safe navigation
- ✅ Error handling
- ✅ Preview support for SwiftUI

## Requirements

- iOS 16.0+
- Xcode 15.0+
- Swift 5.9+

## Permissions

The app requires:
- **Camera** - For QR code scanning (receive files)
- **Photo Library** - For file selection (send files)

These are declared in `Info.plist`.

## Running the App

1. Open `iosApp.xcodeproj` in Xcode
2. Select a simulator or device
3. Build and run (⌘R)

The Gradle build will automatically compile the Kotlin shared module.
