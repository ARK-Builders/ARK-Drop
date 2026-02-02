import SwiftUI

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    var action: (() -> Void)? = nil
    var actionTitle: String? = nil
    
    var body: some View {
        VStack(spacing: Spacing.lg) {
            Image(systemName: icon)
                .font(.system(size: 72))
                .foregroundColor(.dropPrimary.opacity(0.6))
            
            VStack(spacing: Spacing.xs) {
                Text(title)
                    .font(AppTypography.titleLarge)
                    .foregroundColor(.primary)
                
                Text(message)
                    .font(AppTypography.bodyMedium)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            if let action = action, let actionTitle = actionTitle {
                DropButton(
                    title: actionTitle,
                    action: action,
                    style: .primary
                )
                .frame(maxWidth: 280)
            }
        }
        .padding(Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    EmptyStateView(
        icon: "tray",
        title: "No Files Yet",
        message: "Add files to get started with your first transfer",
        action: {},
        actionTitle: "Add Files"
    )
}
