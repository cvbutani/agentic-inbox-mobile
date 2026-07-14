package com.sonicstarsolutions.agentic.inbox.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpPlatformEngine(): HttpClientEngine = OkHttp.create()
