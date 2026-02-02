import SwiftUI

struct ErrorView: View {
    let title: String
    let message: String
    var onRetry: (() -> Void)? = nil
    var onDismiss: (() -> Void)? = nil
    
    var body: some View {
        VStack(spacing: Spacing.lg) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 64))
                .foregroundColor(.dropError)
            
            VStack(spacing: Spacing.xs) {
                Text(title)
                    .font(AppTypography.titleLarge)
                    .foregroundColor(.primary)
                
                Text(message)
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            VStack(spacing: Spacing.sm) {
                if let onRetry = onRetry {
                    DropButton(
                        title: "Try Again",
                        action: onRetry,
                        style: .primary
                    )
                }
                
                if let onDismiss = onDismiss {
                    DropButton(
                        title: "Dismiss",
                        action: onDismiss,
                        style: .outline
                    )
                }
            }
            .frame(maxWidth: 280)
        }
        .padding(Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    ErrorView(
        title: "Connection Failed",
        message: "Unable to connect to the receiver. Please check your network and try again.",
        onRetry: {},
        onDismiss: {}
    )
}
