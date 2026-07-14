package com.sonicstarsolutions.agentic.inbox.data.network

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform-specific Ktor engine factory. Provide a real engine per host;
 * the network module cannot build a client without one.
 *
 * Android  -> OkHttp engine (com.sonicstarsolutions.agentic.inbox)
 * iOS      -> Darwin engine
 */
expect fun httpPlatformEngine(): HttpClientEngine
