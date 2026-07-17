package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal const val DATABASE_NAME = "agentic_inbox.db"

/** Shared driver/dispatcher config for both platform builders — only the [RoomDatabase.Builder]
 * construction itself (which needs an Android [android.content.Context] or an iOS file path)
 * differs per platform. */
internal fun RoomDatabase.Builder<AppDatabase>.buildWithDefaults(): AppDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2)
        .build()
