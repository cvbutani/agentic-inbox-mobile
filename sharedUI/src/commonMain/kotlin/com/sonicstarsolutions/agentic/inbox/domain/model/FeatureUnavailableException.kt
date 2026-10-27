package com.sonicstarsolutions.agentic.inbox.domain.model

/** An optional Worker feature answered 404/501 — the server is older than this client, or has the
 * feature switched off. [message] is already phrased for the user. */
class FeatureUnavailableException(message: String) : Exception(message)
