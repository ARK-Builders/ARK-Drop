import SwiftUI
import Shared
import UniformTypeIdentifiers
import Combine

struct SendView: View {
    @StateObject private var viewModel = SendViewModelWrapper()
    @EnvironmentObject private var coordinator: NavigationCoordinator
    @State private var showFilePicker = false
    
    var body: some View {
        ZStack {
            Color.dropBackground
                .ignoresSafeArea()
            
            Group {
                switch viewModel.state {
                case let state as SendScreenState.FileSelection:
                    FileSelectionView(
                        state: state,
                        onAddFiles: { showFilePicker = true },
                        onRemoveFile: viewModel.onFileRemove,
                        onStartTransfer: viewModel.onStartTransfer
                    )
                    
                case let state as SendScreenState.GeneratingQR:
                    LoadingView(
                        message: "Generating QR Code...",
                        canCancel: true,
                        onCancel: viewModel.onCancelQrGeneration
                    )
                    
                case let state as SendScreenState.WaitingForReceiver:
                    WaitingForReceiverView(
                        state: state,
                        onCancel: viewModel.onCancelTransfer
                    )
                    
                case let state as SendScreenState.Transfer:
                    TransferringView(
                        state: state,
                        onCancel: viewModel.onCancelTransfer
                    )
                    
                case let state as SendScreenState.Complete:
                    TransferCompleteView(
                        fileCount: state.files.count,
                        onSendMore: viewModel.onSendMore,
                        onDone: viewModel.onDone
                    )
                    
                case let state as SendScreenState.Error:
                    SendErrorView(
                        error: state.error,
                        onRetry: viewModel.onErrorRetry,
                        onDismiss: viewModel.onErrorDismiss
                    )
                    
                default:
                    EmptyView()
                }
            }
        }
        .navigationTitle("Send Files")
        .navigationBarTitleDisplayMode(.inline)
        .fileImporter(
            isPresented: $showFilePicker,
            allowedContentTypes: [.item],
            allowsMultipleSelection: true
        ) { result in
            switch result {
            case .success(let urls):
                // Start accessing security-scoped resources and use file paths
                var accessiblePaths: [String] = []
                for url in urls {
                    if url.startAccessingSecurityScopedResource() {
                        accessiblePaths.append(url.path)
                        viewModel.trackAccessedURL(url)
                    }
                }
                viewModel.onFilesAdded(accessiblePaths)
            case .failure(let error):
                print("File picker error: \(error)")
            }
        }
        .onReceive(viewModel.effectPublisher) { effect in
            handleEffect(effect)
        }
    }
    
    private func handleEffect(_ effect: SendScreenEffect) {
        switch effect {
        case is SendScreenEffect.LaunchFilePicker:
            showFilePicker = true
            
        case is SendScreenEffect.NavigateBack:
            coordinator.navigateBack()
            
        default:
            break
        }
    }
}

// MARK: - File Selection View

struct FileSelectionView: View {
    let state: SendScreenState.FileSelection
    let onAddFiles: () -> Void
    let onRemoveFile: (String) -> Void
    let onStartTransfer: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.lg) {
            if state.files.isEmpty {
                EmptyStateView(
                    icon: "doc.badge.plus",
                    title: "No Files Selected",
                    message: "Choose files to send to another device",
                    action: onAddFiles,
                    actionTitle: "Add Files"
                )
            } else {
                ScrollView {
                    VStack(spacing: Spacing.md) {
                        // File List
                        ForEach(state.files, id: \.self) { file in
                            FileItemRow(
                                fileName: extractFileName(from: file),
                                fileSize: getFileSize(for: file),
                                onRemove: { onRemoveFile(file) }
                            )
                        }
                        .padding(.horizontal, Spacing.md)
                        
                        // Actions
                        VStack(spacing: Spacing.sm) {
                            DropButton(
                                title: "Add More Files",
                                action: onAddFiles,
                                style: .outline
                            )
                            
                            DropButton(
                                title: "Start Transfer",
                                action: onStartTransfer,
                                style: .primary,
                                isEnabled: state.canStartTransfer
                            )
                        }
                        .padding(.horizontal, Spacing.md)
                        .padding(.top, Spacing.md)
                    }
                }
            }
        }
        .padding(.vertical, Spacing.md)
    }
    
    private func extractFileName(from path: String) -> String {
        URL(fileURLWithPath: path).lastPathComponent
    }
    
    private func getFileSize(for path: String) -> Int64 {
        let url = URL(fileURLWithPath: path)
        do {
            let resources = try url.resourceValues(forKeys: [.fileSizeKey])
            return Int64(resources.fileSize ?? 0)
        } catch {
            return 0
        }
    }
}

// MARK: - File Item Row

struct FileItemRow: View {
    let fileName: String
    let fileSize: Int64
    let onRemove: () -> Void
    
    var body: some View {
        DropCard(padding: Spacing.md) {
            HStack(spacing: Spacing.md) {
                Image(systemName: fileIcon)
                    .font(.system(size: 32))
                    .foregroundColor(.dropPrimary)
                    .frame(width: 40)
                
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(fileName)
                        .font(AppTypography.bodyMedium)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                    
                    if fileSize > 0 {
                        Text(formatFileSize(fileSize))
                            .font(AppTypography.bodySmall)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                Button(action: onRemove) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.secondary)
                }
            }
        }
    }
    
    private var fileIcon: String {
        let ext = (fileName as NSString).pathExtension.lowercased()
        switch ext {
        case "jpg", "jpeg", "png", "gif", "heic":
            return "photo"
        case "mp4", "mov", "avi":
            return "video"
        case "mp3", "wav", "m4a":
            return "music.note"
        case "pdf":
            return "doc.text"
        case "zip", "rar":
            return "doc.zipper"
        default:
            return "doc"
        }
    }
    
    private func formatFileSize(_ size: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: size, countStyle: .file)
    }
}

// MARK: - Waiting For Receiver

struct WaitingForReceiverView: View {
    let state: SendScreenState.WaitingForReceiver
    let onCancel: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Spacer()
            
            // QR Code
            if let qrImage = imageFromBytes(state.qrBitmap) {
                Image(uiImage: qrImage)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 280, height: 280)
                    .background(Color.white)
                    .cornerRadius(CornerRadius.large)
                    .shadow(color: Color.black.opacity(0.1), radius: 12, x: 0, y: 4)
            }
            
            VStack(spacing: Spacing.sm) {
                Text("Scan this QR code")
                    .font(AppTypography.titleLarge)
                
                Text("Waiting for receiver to connect...")
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
                
                // Copy ticket/confirmation
                HStack {
                    Text(state.copyString)
                        .font(AppTypography.bodySmall.monospaced())
                        .foregroundColor(.secondary)
                    
                    Button(action: {
                        UIPasteboard.general.string = state.copyString
                    }) {
                        Image(systemName: "doc.on.doc")
                            .foregroundColor(.dropPrimary)
                    }
                }
                .padding(.horizontal, Spacing.md)
                .padding(.vertical, Spacing.sm)
                .background(Color.gray.opacity(0.1))
                .cornerRadius(CornerRadius.small)
            }
            
            Spacer()
            
            DropButton(
                title: "Cancel",
                action: onCancel,
                style: .outline
            )
            .frame(maxWidth: 280)
        }
        .padding(Spacing.lg)
    }
    
    private func imageFromBytes(_ bytes: KotlinByteArray) -> UIImage? {
        let count = Int(bytes.size)
        guard count > 0 else {
            print("⚠️ QR code data is empty")
            return nil
        }
        
        var byteArray = [UInt8](repeating: 0, count: count)
        for i in 0..<count {
            // Kotlin Byte = Swift Int8, convert to UInt8
            byteArray[i] = UInt8(bitPattern: bytes.get(index: Int32(i)))
        }
        
        let data = Data(byteArray)
        guard let image = UIImage(data: data) else {
            print("⚠️ Failed to decode QR image from \(count) bytes")
            return nil
        }
        
        print("✅ Successfully decoded QR code (\(count) bytes)")
        return image
    }
}

// MARK: - Transferring View

struct TransferringView: View {
    let state: SendScreenState.Transfer
    let onCancel: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Spacer()
            
            // Receiver Info
            VStack(spacing: Spacing.md) {
                AvatarView(
                    avatarBase64: state.receiverAvatar,
                    size: 80
                )
                
                Text("Sending to \(state.receiverName)")
                    .font(AppTypography.titleLarge)
                
                Text(state.currentFileName)
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            
            // Progress
            VStack(spacing: Spacing.md) {
                DropProgressBar(
                    progress: Double(state.bytesTransferred) / Double(max(state.totalBytes, 1)),
                    height: 12,
                    foregroundColor: .dropSending
                )
                
                HStack {
                    Text(formatBytes(state.bytesTransferred))
                        .font(AppTypography.bodySmall)
                    
                    Spacer()
                    
                    Text(formatBytes(state.totalBytes))
                        .font(AppTypography.bodySmall)
                }
                .foregroundColor(.secondary)
            }
            .padding(.horizontal, Spacing.xl)
            
            Spacer()
            
            DropButton(
                title: "Cancel Transfer",
                action: onCancel,
                style: .destructive
            )
            .frame(maxWidth: 280)
        }
        .padding(Spacing.lg)
    }
    
    private func formatBytes(_ bytes: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: bytes, countStyle: .file)
    }
}

// MARK: - Transfer Complete

struct TransferCompleteView: View {
    let fileCount: Int
    let onSendMore: () -> Void
    let onDone: () -> Void
    
    var body: some View {
        VStack(spacing: Spacing.xl) {
            Spacer()
            
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.dropSuccess)
            
            VStack(spacing: Spacing.sm) {
                Text("Transfer Complete!")
                    .font(AppTypography.titleLarge)
                
                Text("\(fileCount) file(s) sent successfully")
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(spacing: Spacing.sm) {
                DropButton(
                    title: "Send More Files",
                    action: onSendMore,
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

struct SendErrorView: View {
    let error: SendException
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
        case .transferInitializationFailed:
            return "Initialization Failed"
        case .qrgenerationFailed:
            return "QR Code Error"
        case .transferInterrupted:
            return "Transfer Interrupted"
        default:
            return "Error"
        }
    }
    
    private var errorMessage: String {
        switch error {
        case .transferInitializationFailed:
            return "Unable to start the transfer. Please check your network and try again."
        case .qrgenerationFailed:
            return "Failed to generate QR code. Please try again."
        case .transferInterrupted:
            return "The transfer was interrupted. Please try again."
        default:
            return "An unknown error occurred."
        }
    }
}

// MARK: - ViewModel Wrapper

@MainActor
class SendViewModelWrapper: ObservableObject {
    @Published private(set) var state: SendScreenState
    @Published var effectPublisher = PassthroughSubject<SendScreenEffect, Never>()
    
    private let viewModel: SendViewModel
    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?
    private var accessedURLs: [URL] = []
    
    init() {
        self.viewModel = DIContainer.shared.makeSendViewModel()
        self.state = SendScreenState.FileSelection(files: [], size: 0, canStartTransfer: false)
        observeState()
        observeEffects()
    }
    
    func trackAccessedURL(_ url: URL) {
        accessedURLs.append(url)
    }
    
    private func observeState() {
        stateTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await newState in viewModel.container.stateFlow {
                    self.state = newState as! SendScreenState
                }
            } catch {
                print("SendViewModel state error: \(error)")
            }
        }
    }
    
    private func observeEffects() {
        effectTask = Task { [weak self] in
            guard let self = self else { return }
            
            do {
                for try await effect in viewModel.container.sideEffectFlow {
                    if let typedEffect = effect as? SendScreenEffect {
                        self.effectPublisher.send(typedEffect)
                    }
                }
            } catch {
                print("SendViewModel effect error: \(error)")
            }
        }
    }
    
    func onFilesAdded(_ files: [String]) {
        viewModel.onFilesAdded(newFiles: files)
    }
    
    func onFileRemove(_ file: String) {
        viewModel.onFileRemove(file: file)
    }
    
    func onStartTransfer() {
        viewModel.onStartTransfer()
    }
    
    func onCancelTransfer() {
        viewModel.onCancelTransfer()
    }
    
    func onCancelQrGeneration() {
        viewModel.onCancelQrGeneration()
    }
    
    func onSendMore() {
        viewModel.onSendMore()
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
        // Release security-scoped resource access
        accessedURLs.forEach { $0.stopAccessingSecurityScopedResource() }
        accessedURLs.removeAll()
        
        stateTask?.cancel()
        effectTask?.cancel()
    }
}

#Preview {
    NavigationStack {
        SendView()
            .environmentObject(NavigationCoordinator())
    }
}
