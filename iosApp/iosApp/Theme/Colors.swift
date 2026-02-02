import SwiftUI

extension Color {
    // MARK: - Primary Colors
    static let dropPrimary = Color(hex: "6200EE")
    static let dropPrimaryVariant = Color(hex: "3700B3")
    static let dropSecondary = Color(hex: "03DAC6")
    static let dropSecondaryVariant = Color(hex: "018786")
    
    // MARK: - Background Colors
    static let dropBackground = Color(hex: "F5F5F5")
    static let dropSurface = Color.white
    static let dropCard = Color.white
    
    // MARK: - Status Colors
    static let dropError = Color(hex: "B00020")
    static let dropSuccess = Color(hex: "4CAF50")
    static let dropWarning = Color(hex: "FF9800")
    
    // MARK: - Text Colors
    static let dropOnPrimary = Color.white
    static let dropOnSecondary = Color.black
    static let dropOnBackground = Color(hex: "000000")
    static let dropOnSurface = Color(hex: "000000")
    static let dropOnError = Color.white
    
    // MARK: - Transfer Colors
    static let dropSending = Color(hex: "2196F3")
    static let dropReceiving = Color(hex: "4CAF50")
    
    // MARK: - Utility
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
