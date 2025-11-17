package dev.arkbuilders.drop.app.presentation

import android.app.Application
import dev.arkbuilders.drop.app.BuildConfig
import dev.arkbuilders.drop.app.di.appModule
import dev.arkbuilders.drop.app.di.viewModelsModule
import dev.arkbuilders.drop.app.domain.model.BuildConfigFields
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class DropApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.Forest.plant(Timber.DebugTree())

        val buildConfigFieldsModule =
            module {
                single {
                    BuildConfigFields(
                        BuildConfig.VERSION_CODE,
                        BuildConfig.VERSION_NAME,
                    )
                }
            }
        startKoin {
            androidContext(this@DropApplication)
            modules(appModule, viewModelsModule, buildConfigFieldsModule)
        }
    }
}
