package dev.arkbuilders.drop.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initialize Koin for iOS
 * This should be called from Swift on app startup
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModule)
    }
}

// Empty function for SwiftUI to call
fun doInitKoin() = initKoin()
