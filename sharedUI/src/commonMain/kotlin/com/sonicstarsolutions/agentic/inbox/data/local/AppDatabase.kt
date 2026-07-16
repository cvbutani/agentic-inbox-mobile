package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [MailboxEntity::class, FolderEntity::class, EmailEntity::class], version = 1, exportSchema = true)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mailboxDao(): MailboxDao
    abstract fun folderDao(): FolderDao
    abstract fun emailDao(): EmailDao
}

/** Room's KSP processor generates the `actual` implementation of this per target (Android/iOS) —
 * required boilerplate for Room's Kotlin Multiplatform constructor pattern. */
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
