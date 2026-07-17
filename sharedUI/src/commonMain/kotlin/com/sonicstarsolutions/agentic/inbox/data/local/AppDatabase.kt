package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [MailboxEntity::class, FolderEntity::class, EmailEntity::class, DraftEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mailboxDao(): MailboxDao
    abstract fun folderDao(): FolderDao
    abstract fun emailDao(): EmailDao
    abstract fun draftDao(): DraftDao
}

/**
 * Adds the `drafts` table.
 *
 * Written out rather than left to a destructive fallback: every other table here is a cache that
 * can be refetched, but drafts only exist on the device — dropping them would throw away
 * something the user wrote.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `drafts` (" +
                "`id` TEXT NOT NULL, " +
                "`mailboxId` TEXT NOT NULL, " +
                "`to` TEXT NOT NULL, " +
                "`cc` TEXT NOT NULL, " +
                "`bcc` TEXT NOT NULL, " +
                "`subject` TEXT NOT NULL, " +
                "`body` TEXT NOT NULL, " +
                "`mode` TEXT NOT NULL, " +
                "`originalEmailId` TEXT, " +
                "`threadId` TEXT, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }
}

/** Room's KSP processor generates the `actual` implementation of this per target (Android/iOS) —
 * required boilerplate for Room's Kotlin Multiplatform constructor pattern. */
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
