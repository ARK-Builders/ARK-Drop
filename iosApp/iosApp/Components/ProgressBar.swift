import SwiftUI

struct DropProgressBar: View {
    let progress: Double // 0.0 to 1.0
    var height: CGFloat = 8
    var backgroundColor: Color = Color.gray.opacity(0.2)
    var foregroundColor: Color = .dropPrimary
    
    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                // Background
                RoundedRectangle(cornerRadius: height / 2)
                    .fill(backgroundColor)
                    .frame(height: height)
                
                // Foreground
                RoundedRectangle(cornerRadius: height / 2)
                    .fill(foregroundColor)
                    .frame(
                        width: geometry.size.width * min(max(progress, 0), 1),
                        height: height
                    )
                    .animation(.easeInOut(duration: 0.3), value: progress)
            }
        }
        .frame(height: height)
    }
}

#Preview {
    VStack(spacing: Spacing.md) {
        DropProgressBar(progress: 0.3)
        DropProgressBar(progress: 0.7, foregroundColor: .dropSuccess)
        DropProgressBar(progress: 1.0)
    }
    .padding()
}
