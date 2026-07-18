package com.sonicstarsolutions.agentic.inbox.di

import androidx.room.Room
import com.sonicstarsolutions.agentic.inbox.data.local.AppDatabase
import com.sonicstarsolutions.agentic.inbox.data.local.AttachmentStore
import com.sonicstarsolutions.agentic.inbox.data.local.DATABASE_NAME
import com.sonicstarsolutions.agentic.inbox.data.local.IosAttachmentStore
import com.sonicstarsolutions.agentic.inbox.data.local.buildWithDefaults
import com.sonicstarsolutions.agentic.inbox.platform.AttachmentOpener
import com.sonicstarsolutions.agentic.inbox.platform.IosAttachmentOpener
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

val platformSettingsDataStoreModule = module {
    // Engine only — the HttpClient itself (defaultRequest URL/headers, ContentNegotiation) is
    // built in commonMain's networkModule. Providing a full HttpClient here would silently
    // override that configured client (platform modules load last), which is exactly the bug
    // that sent every request to localhost.
    single<HttpClientEngine> { Darwin.create() }
    single { KSafe() }
    single<AttachmentStore> { IosAttachmentStore() }
    single<AttachmentOpener> { IosAttachmentOpener() }
    single<AppDatabase> {
        Room.databaseBuilder<AppDatabase>(name = "${documentDirectory()}/$DATABASE_NAME").buildWithDefaults()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Could not resolve the iOS documents directory" }
}
