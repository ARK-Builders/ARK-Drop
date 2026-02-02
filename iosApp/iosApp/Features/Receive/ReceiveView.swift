import SwiftUI
import Shared
import AVFoundation
import Combine

struct ReceiveView: View {
    @StateObject private var viewModel = ReceiveViewModelWrapper()
    @EnvironmentObject private var coordinator: NavigationCoordinator
    
    var body: some View {
        ZStack {
            Color.dropBackground
                .ignoresSafeArea()
            
            Group {
                switch viewModel.state {
                case let state as ReceiveScreenState.Initial:
                    InitialReceiveView(
                        hasCameraPermission: state.cameraPermissionGranted,
                        onStartScanning: viewModel.onStartScanning,
                        onEnterManually: viewModel.onEnterManually,
                        onRequestPermission: viewModel.onRequestCameraPermission
                    )
                    
                case is ReceiveScreenState.RequestingPermission:
                    LoadingView(message: "Requesting camera permission...")
                    
                case is ReceiveScreenState.Scanning:
                    QRScannerView(
                        onCodeScanned: viewModel.onQrCodeScanned,
                        onCancel: viewModel.onStopScanning
                    )
                    
                case let state as ReceiveScreenState.ManualInput:
                    ManualInputView(
                        inputText: state.inputText,
                        inputError: state.inputError,
                        onInputChanged: viewModel.onManualInputChanged,
                        onSubmit: viewModel.handleManualInputSubmit,
                        onCancel: viewModel.onCancelManualInput,
                        onPaste: viewModel.onPasteFromClipboard
                    )
                    
                case let state as ReceiveScreenState.QRCodeScanned:
                    QRScannedView(
                        ticket: state.ticket,
                        confirmation: state.confirmation,
                        onAccept: viewModel.onAccept,
                        onCancel: viewModel.onScanAgain
                    )
                    
                case is ReceiveScreenState.Connecting:
                    LoadingView(message: "Connecting to sender...")
                    
                case let state as ReceiveScreenState.Receiving:
                    ReceivingView(
                        state: state,
                        onCancel: viewModel.onCancelReceiving
                    )
                    
                case let state as ReceiveScreenState.Success:
                    ReceiveSuccessView(
                        fileCount: state.receivedFiles.count,
                        onReceiveMore: viewModel.onReceiveMore,
                        onDone: viewModel.onDone
                    )
                    
                case let state as ReceiveScreenState.Error:
                    ReceiveErrorView(
                        error: state.error,
                        onRetry: viewModel.onErrorRetry,
                        onDismiss: viewModel.onErrorDismiss
                    )
                    
                default:
                    EmptyView()
                }
            }
        }
        .navigationTitle("Receive Files")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.effectPublisher) { effect in
            handleEffect(effect)
        }
    }
    
    private func handleEffect(_ effect: ReceiveScreenEffect) {
        switch effect {
        case is ReceiveScreenEffect.RequestCameraPermission:
            requestCameraPermission()
            
        case is ReceiveScreenEffect.NavigateBack:
            coordinator.navigateBack()
            
        case is ReceiveScreenEffect.HideKeyboard:
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            
        default:
            break
        }
    }
    
    private func requestCameraPermission() {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            Task { @MainActor in
                viewModel.onCameraPermissionGranted(granted)
            }
        }
    }
}

// MARK: - Initial View

struct InitialReceiveView: View {
    let hasCameraPermission: Bool
    let onStartScanning: () -> Void
    let onEnterManually: () -> Void
    let onRequestPermission: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Image(systemName: "qrcode.viewfinder")
                .font(.system(size: 100))
                .foregroundColor(.dropPrimary.opacity(0.6))
            
            VStack(spacing: Spacing.sm) {
                Text("Receive Files")
                    .font(AppTypography.titleLarge)
                
                Text("Scan a QR code from the sender to receive files")
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            VStack(spacing: Spacing.sm) {
                if hasCameraPermission {
                    DropButton(
                        title: "Scan QR Code",
                        action: onStartScanning,
                        style: .primary
                    )
                } else {
                    DropButton(
                        title: "Allow Camera Access",
                        action: onRequestPermission,
                        style: .primary
                    )
                }
                
                DropButton(
                    title: "Enter Code Manually",
                    action: onEnterManually,
                    style: .outline
                )
            }
            .frame(maxWidth: 280)
        }
        .padding(Spacing.xl)
    }
}

// MARK: - Manual Input

struct ManualInputView: View {
    let inputText: String
    let inputError: String?
    let onInputChanged: (String) -> Void
    let onSubmit: () -> Void
    let onCancel: () -> Void
    let onPaste: (String?) -> Void
    
    @FocusState private var isFocused: Bool
    
    var body: some View {
        VStack(spacing: Spacing.lg) {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text("Enter Transfer Code")
                    .font(AppTypography.titleMedium)
                
                Text("Format: ticket confirmation")
                    .font(AppTypography.bodySmall)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            VStack(alignment: .leading, spacing: Spacing.xs) {
                TextField("e.g. abc123 42", text: Binding(
                    get: { inputText },
                    set: { onInputChanged($0) }
                ))
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .font(AppTypography.bodyMedium.monospaced())
                .focused($isFocused)
                .onSubmit(onSubmit)
                
                if let error = inputError {
                    Text(error)
                        .font(AppTypography.bodySmall)
                        .foregroundColor(.dropError)
                }
            }
            
            Button(action: {
                onPaste(UIPasteboard.general.string)
            }) {
                HStack {
                    Image(systemName: "doc.on.clipboard")
                    Text("Paste from Clipboard")
                }
                .font(AppTypography.labelMedium)
                .foregroundColor(.dropPrimary)
            }
            
            Spacer()
            
            VStack(spacing: Spacing.sm) {
                DropButton(
                    title: "Submit",
                    action: onSubmit,
                    style: .primary,
                    isEnabled: !inputText.isEmpty
                )
                
                DropButton(
                    title: "Cancel",
                    action: onCancel,
                    style: .outline
                )
            }
            .frame(maxWidth: 280)
        }
        .padding(Spacing.lg)
        .onAppear {
            isFocused = true
        }
    }
}

// MARK: - QR Scanned

struct QRScannedView: View {
    let ticket: String
    let confirmation: UInt8
    let onAccept: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Spacer()
            
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.dropSuccess)
            
            VStack(spacing: Spacing.sm) {
                Text("Code Scanned")
                    .font(AppTypography.titleLarge)
                
                VStack(spacing: Spacing.xxs) {
                    Text("Ticket: \(ticket)")
                        .font(AppTypography.bodyMedium.monospaced())
                    Text("Confirmation: \(confirmation)")
                        .font(AppTypography.bodyMedium.monospaced())
                }
                .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(spacing: Spacing.sm) {
                DropButton(
                    title: "Accept Transfer",
                    action: onAccept,
                    style: .primary
                )
                
                DropButton(
                    title: "Scan Again",
                    action: onCancel,
                    style: .outline
                )
            }
            .frame(maxWidth: 280)
        }
        .padding(Spacing.lg)
    }
}

// MARK: - Receiving

struct ReceivingView: View {
    let state: ReceiveScreenState.Receiving
    let onCancel: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Spacer()
            
            // Sender Info
            if state.progress.isConnected {
                VStack(spacing: Spacing.md) {
                    AvatarView(
                        avatarBase64: state.progress.senderAvatar,
                        size: 80
                    )
                    
                    Text("Receiving from \(state.progress.senderName)")
                        .font(AppTypography.titleLarge)
                }
            }
            
            // Files Progress
            if !state.progress.files.isEmpty {
                VStack(spacing: Spacing.md) {
                    ForEach(state.progress.files, id: \.id) { file in
                        FileProgressRow(
                            fileName: file.name,
                            totalSize: Int64(file.size),
                            receivedBytes: state.progress.fileProgress[file.id]?.receivedBytes ?? 0,
                            isComplete: state.progress.fileProgress[file.id]?.isComplete ?? false
                        )
                    }
                }
                .padding(.horizontal, Spacing.lg)
            } else {
                ProgressView()
                    .scaleEffect(1.5)
                    .tint(.dropReceiving)
                
                Text("Waiting for files...")
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            DropButton(
                title: "Cancel",
                action: onCancel,
                style: .destructive
            )
            .frame(maxWidth: 280)
        }
        .padding(Spacing.lg)
    }
}

// MARK: - File Progress Row

struct FileProgressRow: View {
    let fileName: String
    let totalSize: Int64
    let receivedBytes: Int64
    let isComplete: Bool
    
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text(fileName)
                    .font(AppTypography.bodyMedium)
                    .lineLimit(1)
                
                Spacer()
                
                if isComplete {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.dropSuccess)
                }
            }
            
            DropProgressBar(
                progress: Double(receivedBytes) / Double(max(totalSize, 1)),
                foregroundColor: .dropReceiving
            )
            
            HStack {
                Text(formatBytes(receivedBytes))
                    .font(AppTypography.bodySmall)
                
                Spacer()
                
                Text(formatBytes(totalSize))
                    .font(AppTypography.bodySmall)
            }
            .foregroundColor(.secondary)
        }
        .padding(Spacing.sm)
        .background(Color.dropCard)
        .cornerRadius(CornerRadius.small)
    }
    
    private func formatBytes(_ bytes: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: bytes, countStyle: .file)
    }
}

// MARK: - Success View

struct ReceiveSuccessView: View {
    let fileCount: Int
    let onReceiveMore: () -> Void
    let onDone: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Spacer()
            
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.dropSuccess)
            
            VStack(spacing: Spacing.sm) {
                Text("Files Received!")
                    .font(AppTypography.titleLarge)
                
                Text("\(fileCount) file(s) saved successfully")
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(spacing: Spacing.sm) {
                DropButton(
                    title: "Receive More Files",
                    action: onReceiveMore,
                    style: .primary
                )
                
                DropButton(
                    title: "Done",
                    action: onDone,
                    style: .outline
                )
            }
            .frame(maxWidth: 280)
        }
        .padding(Spacing.lg)
    }
}

// MARK: - Error View

struct ReceiveErrorView: View {
    let error: ReceiveError
    let onRetry: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        ErrorView(
            title: errorTitle,
            message: errorMessage,
            onRetry: onRetry,
            onDismiss: onDismiss
        )
    }
    
    private var errorTitle: String {
        switch error {
        case .cameraPermissionDenied:
            return "Camera Permission Required"
        case .connectionFailed:
            return "Connection Failed"
        case .networkError:
            return "Network Error"
        case .storageError:
            return "Storage Error"
        default:
            return "Error"
        }
    }
    
    private var errorMessage: String {
        switch error {
        case .cameraPermissionDenied:
            return "Camera access is needed to scan QR codes. Please enable it in Settings."
        case .connectionFailed:
            return "Unable to connect to the sender. Please try again."
        case .networkError:
            return "Network connection lost. Please check your connection."
        case .storageError:
            return "Unable to save files. Please check your storage space."
        default:
            return "An error occurred. Please try again."
        }
    }
}

// MARK: - ViewModel Wrapper

@MainActor
class ReceiveViewModelWrapper: ObservableObject {
    @Published private(set) var state: ReceiveScreenState
    let effectPublisher = PassthroughSubject<ReceiveScreenEffect, Never>()
    
    private let viewModel: ReceiveViewModel
    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?
    
    init() {
        self.viewModel = DIContainer.shared.makeReceiveViewModel()
        self.state = ReceiveScreenState.Initial(cameraPermissionGranted: false)
        observeState()
        observeEffects()
    }
    
    private func observeState() {
        stateTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await newState in viewModel.container.stateFlow {
                    print("📡 ReceiveViewModel state changed: \(type(of: newState))")
                    self.state = newState as! ReceiveScreenState
                }
            } catch {
                print("❌ ReceiveViewModel state error: \(error)")
            }
        }
    }
    
    private func observeEffects() {
        effectTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await effect in viewModel.container.sideEffectFlow {
                    if let typedEffect = effect as? ReceiveScreenEffect {
                        self.effectPublisher.send(typedEffect)
                    }
                }
            } catch {
                print("ReceiveViewModel effect error: \(error)")
            }
        }
    }
    
    func onStartScanning() {
        viewModel.onStartScanning()
    }
    
    func onStopScanning() {
        viewModel.onStopScanning()
    }
    
    func onEnterManually() {
        viewModel.onEnterManually()
    }
    
    func onRequestCameraPermission() {
        viewModel.onRequestCameraPermission()
    }
    
    func onCameraPermissionGranted(_ granted: Bool) {
        viewModel.onCameraPermissionGranted(isGranted: granted)
    }
    
    func onQrCodeScanned(ticket: String, confirmation: UInt8) {
        print("🔄 ReceiveViewModelWrapper.onQrCodeScanned: ticket=\(ticket), confirmation=\(confirmation)")
        viewModel.onQrCodeScanned(ticket: ticket, confirmation: confirmation)
        print("✅ Called viewModel.onQrCodeScanned")
    }
    
    func onManualInputChanged(_ input: String) {
        viewModel.onManualInputChanged(input: input)
    }
    
    func handleManualInputSubmit() {
        viewModel.handleManualInputSubmit()
    }
    
    func onPasteFromClipboard(_ text: String?) {
        viewModel.onPasteFromClipboard(clipText: text)
    }
    
    func onAccept() {
        viewModel.onAccept()
    }
    
    func onCancelReceiving() {
        viewModel.onCancelReceiving()
    }
    
    func onCancelManualInput() {
        viewModel.onCancelManualInput()
    }
    
    func onScanAgain() {
        viewModel.onScanAgain()
    }
    
    func onReceiveMore() {
        viewModel.onReceiveMore()
    }
    
    func onDone() {
        viewModel.onDone()
    }
    
    func onErrorRetry() {
        viewModel.onErrorRetry()
    }
    
    func onErrorDismiss() {
        viewModel.onErrorDismiss()
    }
    
    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }
}

#Preview {
    NavigationStack {
        ReceiveView()
            .environmentObject(NavigationCoordinator())
    }
}
