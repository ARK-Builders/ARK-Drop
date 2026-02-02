import SwiftUI

struct AboutView: View {
    var body: some View {
        ZStack {
            Color.dropBackground
                .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: Spacing.xl) {
                    // App Icon
                    Image(systemName: "arrow.down.circle.fill")
                        .font(.system(size: 100))
                        .foregroundColor(.dropPrimary)
                    
                    // App Info
                    VStack(spacing: Spacing.xs) {
                        Text("ARK Drop")
                            .font(AppTypography.headlineMedium)
                        
                        Text("Version 1.0.0")
                            .font(AppTypography.bodyMedium)
                            .foregroundColor(.secondary)
                    }
                    
                    // Description
                    DropCard {
                        VStack(alignment: .leading, spacing: Spacing.sm) {
                            Text("About")
                                .font(AppTypography.titleMedium)
                            
                            Text("ARK Drop is a secure, privacy-focused file transfer application. Send and receive files directly between devices without cloud storage.")
                                .font(AppTypography.bodyMedium)
                                .foregroundColor(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    
                    // Links
                    VStack(spacing: Spacing.sm) {
                        LinkRow(
                            icon: "link",
                            title: "Website",
                            url: "https://arkbuilders.github.io"
                        )
                        
                        LinkRow(
                            icon: "lock.shield",
                            title: "Privacy Policy",
                            url: "https://arkbuilders.github.io/privacy"
                        )
                        
                        LinkRow(
                            icon: "doc.text",
                            title: "Open Source Licenses",
                            url: "https://github.com/ARK-Builders/Drop-KMP"
                        )
                    }
                }
                .padding(Spacing.lg)
            }
        }
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct LinkRow: View {
    let icon: String
    let title: String
    let url: String
    
    var body: some View {
        Button(action: {
            if let url = URL(string: url) {
                UIApplication.shared.open(url)
            }
        }) {
            DropCard(padding: Spacing.md) {
                HStack {
                    Image(systemName: icon)
                        .font(.system(size: 20))
                        .foregroundColor(.dropPrimary)
                        .frame(width: 32)
                    
                    Text(title)
                        .font(AppTypography.bodyMedium)
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    NavigationStack {
        AboutView()
    }
}
