package dev.arkbuilders.drop.di

import dev.arkbuilders.drop.data.db.DropDatabase
import dev.arkbuilders.drop.data.db.dao.TransferSessionDao
import org.koin.dsl.module

val dbModule =
    module {
        factory<TransferSessionDao> {
            val db: DropDatabase = get()
            db.transferHistoryDao()
        }
    }
