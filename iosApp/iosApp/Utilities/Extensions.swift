import Foundation
import Combine

// MARK: - PassthroughSubject Extension

extension PassthroughSubject where Output == Never {
    static func create() -> PassthroughSubject<Output, Failure> {
        return PassthroughSubject<Output, Failure>()
    }
}

// MARK: - String Extensions

extension String {
    var isNotEmpty: Bool {
        !isEmpty
    }
}

// MARK: - URL Extensions

extension URL {
    var fileSize: UInt64 {
        let attributes = try? FileManager.default.attributesOfItem(atPath: path)
        return attributes?[.size] as? UInt64 ?? 0
    }
}

// MARK: - View Extensions

import SwiftUI

extension View {
    func hideKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil,
            from: nil,
            for: nil
        )
    }
}
