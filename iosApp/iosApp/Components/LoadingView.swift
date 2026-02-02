import SwiftUI

struct LoadingView: View {
    let message: String
    var canCancel: Bool = false
    var onCancel: (() -> Void)? = nil
    
    var body: some View {
        VStack(spacing: Spacing.lg) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.dropPrimary)
            
            Text(message)
                .font(AppTypography.bodyLarge)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            if canCancel, let onCancel = onCancel {
                DropButton(
                    title: "Cancel",
                    action: onCancel,
                    style: .outline
                )
                .frame(maxWidth: 200)
                .padding(.top, Spacing.md)
            }
        }
        .padding(Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    LoadingView(message: "Generating QR Code...", canCancel: true, onCancel: {})
}
