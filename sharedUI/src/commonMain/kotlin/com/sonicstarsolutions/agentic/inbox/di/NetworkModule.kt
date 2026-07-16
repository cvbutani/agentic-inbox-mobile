package com.sonicstarsolutions.agentic.inbox.di

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.createAgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.header
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

val networkModule = module {
    factory<HttpClient> {
        val credentials: CredentialsRepository = get()
        val engine: HttpClientEngine = get()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
        HttpClient(engine) {
            // Non-2xx responses throw ResponseException instead of handing an HTML error page to
            // the JSON deserializer — ConnectionRepositoryImpl.describeFailure relies on this.
            expectSuccess = true
            // Never follow redirects: this client only talks JSON to the Worker. Cloudflare
            // Access answers unauthorized requests with a 302 to its sign-in page; failing fast
            // on the 302 (with its Location header) is diagnosable, silently fetching the login
            // HTML is not.
            followRedirects = false
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                val snapshot = credentials.state.value
                // normalizedBaseUrl() guarantees a scheme — takeFrom() of a scheme-less string
                // leaves host empty and Ktor would silently connect to localhost:80 instead.
                val base = snapshot.normalizedBaseUrl()
                if (base.isNotEmpty()) {
                    url.takeFrom(base)
                }
                if (snapshot.clientId.isNotBlank()) header("CF-Access-Client-Id", snapshot.clientId)
                if (snapshot.clientSecret.isNotBlank()) header("CF-Access-Client-Secret", snapshot.clientSecret)
            }
        }
    }
    factory<AgenticInboxApi> {

        Ktorfit.Builder()
            .baseUrl("", checkUrl = false)
            .httpClient(get<HttpClient>())
            .build()
            .createAgenticInboxApi()
    }
}
