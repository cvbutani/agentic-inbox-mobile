package com.sonicstarsolutions.agentic.inbox.di

import android.content.Context
import androidx.room.Room
import com.sonicstarsolutions.agentic.inbox.data.local.AndroidAttachmentStore
import com.sonicstarsolutions.agentic.inbox.data.local.AppDatabase
import com.sonicstarsolutions.agentic.inbox.data.local.AttachmentStore
import com.sonicstarsolutions.agentic.inbox.data.local.DATABASE_NAME
import com.sonicstarsolutions.agentic.inbox.data.local.buildWithDefaults
import com.sonicstarsolutions.agentic.inbox.platform.AndroidAttachmentOpener
import com.sonicstarsolutions.agentic.inbox.platform.AttachmentOpener
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

fun platformModule(context: Context) = module {
    // Engine only — the HttpClient itself (defaultRequest URL/headers, ContentNegotiation) is
    // built in commonMain's networkModule. Providing a full HttpClient here would silently
    // override that configured client (platform modules load last), which is exactly the bug
    // that sent every request to localhost.
    single<HttpClientEngine> { OkHttp.create() }
    single { KSafe(context) }
    single<AttachmentStore> { AndroidAttachmentStore(context) }
    single<AttachmentOpener> { AndroidAttachmentOpener(context) }
    single<AppDatabase> {
        val appContext = context.applicationContext
        Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
        ).buildWithDefaults()
    }
}
