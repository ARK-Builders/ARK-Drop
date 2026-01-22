package dev.arkbuilders.drop.presentation

import android.app.Application
import dev.arkbuilders.drop.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DropApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@DropApplication)
            modules(appModule)
        }
    }
}
