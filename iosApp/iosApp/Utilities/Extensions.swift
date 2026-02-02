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
