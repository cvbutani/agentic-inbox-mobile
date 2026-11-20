package com.sonicstarsolutions.agentic.inbox.domain.model

/**
 * The backend this app is built for. One source for every user-facing mention of it (onboarding,
 * and later the Settings screen), so the name, link and wording cannot drift apart.
 *
 * The app targets the Sonic Star fork of agentic-inbox only; the upstream cloudflare/agentic-inbox
 * lacks features the app expects (see docs/PUBLIC_BACKEND_MIGRATION.md).
 */
object BackendInfo {
    const val NAME = "agentic-inbox"

    const val REPO_URL = "https://github.com/SonicStar-Solutions/agentic-inbox"

    /** One line shown wherever the user is asked to connect. */
    const val REQUIREMENT = "Requires a deployed $NAME backend."

    /** Label for the link that opens [REPO_URL]. */
    const val REPO_LINK_LABEL = "Get $NAME on GitHub"
}
