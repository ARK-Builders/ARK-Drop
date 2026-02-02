import SwiftUI

struct AvatarView: View {
    let avatarBase64: String?
    let size: CGFloat
    let fallbackIcon: String = "person.circle.fill"
    
    init(avatarBase64: String?, size: CGFloat = 48) {
        self.avatarBase64 = avatarBase64
        self.size = size
    }
    
    var body: some View {
        Group {
            if let base64 = avatarBase64,
               let imageData = Data(base64Encoded: base64),
               let uiImage = UIImage(data: imageData) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                Image(systemName: fallbackIcon)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .foregroundColor(.dropPrimary)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }
}

#Preview {
    HStack(spacing: Spacing.md) {
        AvatarView(avatarBase64: nil, size: 48)
        AvatarView(avatarBase64: nil, size: 64)
        AvatarView(avatarBase64: nil, size: 32)
    }
    .padding()
}
