import SwiftUI

struct DropCard<Content: View>: View {
    let content: Content
    var padding: CGFloat = Spacing.md
    
    init(padding: CGFloat = Spacing.md, @ViewBuilder content: () -> Content) {
        self.padding = padding
        self.content = content()
    }
    
    var body: some View {
        content
            .padding(padding)
            .background(Color.dropCard)
            .cornerRadius(CornerRadius.medium)
            .shadow(color: Color.black.opacity(0.08), radius: 8, x: 0, y: 2)
    }
}

#Preview {
    DropCard {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("Card Title")
                .font(AppTypography.titleLarge)
            Text("Card content goes here")
                .font(AppTypography.bodyMedium)
                .foregroundColor(.secondary)
        }
    }
    .padding()
}
