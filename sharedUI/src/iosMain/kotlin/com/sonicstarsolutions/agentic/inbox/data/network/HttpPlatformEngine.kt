package com.sonicstarsolutions.agentic.inbox.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun httpPlatformEngine(): HttpClientEngine = Darwin.create()
