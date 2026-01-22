package dev.arkbuilders.drop.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.arkbuilders.drop.data.db.DropDatabase
import dev.arkbuilders.drop.data.helper.AvatarHelper
import dev.arkbuilders.drop.data.helper.NetworkStatus
import dev.arkbuilders.drop.data.helper.PermissionsHelper
import dev.arkbuilders.drop.data.helper.ResourcesHelper
import dev.arkbuilders.drop.data.settings.DATASTORE_FILENAME
import dev.arkbuilders.drop.data.settings.createDataStore
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single { AvatarHelper(androidContext()) }
        single { NetworkStatus(androidContext()) }
        single { PermissionsHelper(androidContext()) }
        single { ResourcesHelper(androidContext()) }

        single<DropDatabase> {
            val dbFile = androidApplication().getDatabasePath(DropDatabase.DB_NAME)
            Room.databaseBuilder<DropDatabase>(
                context = androidApplication(),
                name = dbFile.absolutePath,
            ).setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }

        single<DataStore<Preferences>> {
            createDataStore {
                androidContext().filesDir.resolve(DATASTORE_FILENAME).absolutePath
            }
        }
    }
