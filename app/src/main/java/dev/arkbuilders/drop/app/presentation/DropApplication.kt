package dev.arkbuilders.drop.app.presentation

import android.app.Application
import dev.arkbuilders.drop.app.di.appModule
import dev.arkbuilders.drop.app.di.viewModelsModule
import org.koin.core.context.startKoin
import timber.log.Timber

class DropApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.Forest.plant(Timber.DebugTree())

        startKoin {
            modules(appModule, viewModelsModule)
        }
    }
}