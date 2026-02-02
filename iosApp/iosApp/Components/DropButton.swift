import SwiftUI

struct DropButton: View {
    let title: String
    let action: () -> Void
    var style: ButtonStyle = .primary
    var isEnabled: Bool = true
    var isLoading: Bool = false
    
    enum ButtonStyle {
        case primary
        case secondary
        case outline
        case destructive
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: Spacing.xs) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: textColor))
                        .scaleEffect(0.8)
                }
                
                Text(title)
                    .font(AppTypography.labelLarge)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(backgroundColor)
            .foregroundColor(textColor)
            .cornerRadius(CornerRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: CornerRadius.medium)
                    .stroke(borderColor, lineWidth: borderWidth)
            )
        }
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled ? 1.0 : 0.5)
    }
    
    private var backgroundColor: Color {
        switch style {
        case .primary:
            return .dropPrimary
        case .secondary:
            return .dropSecondary
        case .outline:
            return .clear
        case .destructive:
            return .dropError
        }
    }
    
    private var textColor: Color {
        switch style {
        case .primary, .destructive:
            return .white
        case .secondary:
            return .black
        case .outline:
            return .dropPrimary
        }
    }
    
    private var borderColor: Color {
        switch style {
        case .outline:
            return .dropPrimary
        default:
            return .clear
        }
    }
    
    private var borderWidth: CGFloat {
        style == .outline ? 1.5 : 0
    }
}

#Preview {
    VStack(spacing: Spacing.md) {
        DropButton(title: "Primary Button", action: {}, style: .primary)
        DropButton(title: "Secondary Button", action: {}, style: .secondary)
        DropButton(title: "Outline Button", action: {}, style: .outline)
        DropButton(title: "Loading...", action: {}, isLoading: true)
        DropButton(title: "Disabled", action: {}, isEnabled: false)
    }
    .padding()
}
