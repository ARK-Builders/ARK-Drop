import SwiftUI
import AVFoundation
import Shared

struct QRScannerView: View {
    let onCodeScanned: (String, UInt8) -> Void
    let onCancel: () -> Void
    
    @StateObject private var scanner = QRScanner()
    
    var body: some View {
        ZStack {
            // Camera Preview
            CameraPreview(session: scanner.session)
                .ignoresSafeArea()
            
            // Overlay with scan area
            VStack {
                Spacer()
                
                // Scan area indicator
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color.white, lineWidth: 3)
                    .frame(width: 280, height: 280)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .fill(Color.white.opacity(0.1))
                    )
                
                Text("Point camera at QR code")
                    .font(AppTypography.bodyLarge)
                    .foregroundColor(.white)
                    .padding(.top, Spacing.lg)
                
                Spacer()
                
                // Cancel button
                DropButton(
                    title: "Cancel",
                    action: onCancel,
                    style: .outline
                )
                .frame(maxWidth: 280)
                .padding(.bottom, Spacing.xl)
            }
        }
        .onAppear {
            scanner.startScanning()
        }
        .onDisappear {
            scanner.stopScanning()
        }
        .onChange(of: scanner.scannedCode) { _, newValue in
            if let code = newValue {
                handleScannedCode(code)
            }
        }
    }
    
    private func handleScannedCode(_ code: String) {
        print("📷 QR Code scanned: \(code)")
        let reporter = KoinHelper.shared.getFirebaseReporter()
        reporter.log(message: "QRScanner: code scanned length=\(code.count)")
        
        // Parse URL format: drop://receive?ticket=ABC123&confirmation=1
        guard let url = URL(string: code),
              url.scheme == "drop",
              url.host == "receive",
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let queryItems = components.queryItems else {
            print("⚠️ Invalid QR code format: \(code)")
            reporter.log(message: "QRScanner: invalid QR code format")
            return
        }
        
        guard let ticketItem = queryItems.first(where: { $0.name == "ticket" }),
              let ticket = ticketItem.value,
              let confirmationItem = queryItems.first(where: { $0.name == "confirmation" }),
              let confirmationString = confirmationItem.value,
              let confirmation = UInt8(confirmationString) else {
            print("⚠️ Missing ticket or confirmation in QR code")
            reporter.recordError(message: "QRScanner: missing ticket or confirmation in QR code", throwable: nil)
            return
        }
        
        print("✅ Parsed QR: ticket=\(ticket), confirmation=\(confirmation)")
        reporter.log(message: "QRScanner: parsed ticket=\(ticket) confirmation=\(confirmation)")
        onCodeScanned(ticket, confirmation)
        scanner.stopScanning()
    }
}

// MARK: - Camera Preview

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .black
        
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)
        
        context.coordinator.previewLayer = previewLayer
        
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            context.coordinator.previewLayer?.frame = uiView.bounds
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    class Coordinator {
        var previewLayer: AVCaptureVideoPreviewLayer?
    }
}

// MARK: - QR Scanner

@MainActor
class QRScanner: NSObject, ObservableObject, AVCaptureMetadataOutputObjectsDelegate {
    @Published var scannedCode: String?
    
    let session = AVCaptureSession()
    private let metadataOutput = AVCaptureMetadataOutput()
    private let sessionQueue = DispatchQueue(label: "qr.scanner.session")
    
    override init() {
        super.init()
        setupCamera()
    }
    
    private func setupCamera() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            
            guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
                let reporter = KoinHelper.shared.getFirebaseReporter()
                reporter.recordError(message: "QRScanner: no video capture device available", throwable: nil)
                return
            }
            
            guard let videoInput = try? AVCaptureDeviceInput(device: videoCaptureDevice) else {
                let reporter = KoinHelper.shared.getFirebaseReporter()
                reporter.recordError(message: "QRScanner: failed to create video input", throwable: nil)
                return
            }
            
            if self.session.canAddInput(videoInput) {
                self.session.addInput(videoInput)
            }
            
            if self.session.canAddOutput(self.metadataOutput) {
                self.session.addOutput(self.metadataOutput)

                self.metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                self.metadataOutput.metadataObjectTypes = [.qr]
            }
        }
    }
    
    func startScanning() {
        sessionQueue.async { [weak self] in
            let reporter = KoinHelper.shared.getFirebaseReporter()
            reporter.log(message: "QRScanner: start scanning")
            self?.session.startRunning()
        }
    }
    
    func stopScanning() {
        sessionQueue.async { [weak self] in
            let reporter = KoinHelper.shared.getFirebaseReporter()
            reporter.log(message: "QRScanner: stop scanning")
            self?.session.stopRunning()
        }
    }
    
    nonisolated func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        if let metadataObject = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
           let stringValue = metadataObject.stringValue {
            Task { @MainActor in
                self.scannedCode = stringValue
            }
        }
    }
}

#Preview {
    QRScannerView(
        onCodeScanned: { ticket, conf in
            print("Scanned: \(ticket) \(conf)")
        },
        onCancel: {}
    )
}
