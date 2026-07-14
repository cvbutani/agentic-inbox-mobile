# Agentic Inbox — Compose Multiplatform App Plan

**Goal:** A native Android + iOS email client for the existing `cloudflare/agentic-inbox` Worker backend. Same app name, full feature parity with the web client except the AI agent panel (excluded for v1). The backend is used as-is — zero server changes required except optionally one auth decision (see Section 3).

---

## 1. Scope

**In scope (v1):**
- Mailbox management: list, create, switch, edit settings, delete
- Email list per folder, threaded conversation view, pagination
- Read email (HTML rendering), mark read, star, move, delete
- Compose, reply, reply-all, forward — with attachments
- Drafts (save, resume, discard)
- Custom folders (create, rename, delete) alongside system folders (Inbox, Sent, Draft, Archive, Trash, Spam)
- Full search with filters (query, from, to, subject, date range, read/starred/has-attachment)
- Attachment download, preview, and share
- Per-mailbox settings: display name, signature, auto-reply, forwarding
- Offline read cache + optimistic UI for common actions

**Out of scope (v1):**
- Agent chat panel (WebSocket / Agents SDK protocol)
- Auto-draft notifications from the agent
- MCP integration
- Push notifications (no server-side push exists; see Section 9 for a polling alternative)

---

## 2. Architecture

Built on the Terrakok CMP scaffold. Two-module layout: a thin Android application module and a shared KMP library that holds all Compose UI, domain, and data. iOS is consumed as a static framework from `iosApp`.

```
androidApp/                          # Thin Android shell (com.android.application)
└── src/main/kotlin/.../androidApp/
    └── AppActivity.kt               # setContent { App(...) }; status bar theming

sharedUI/                            # KMP library (com.android.kotlin.multiplatform.library)
├── src/
│   ├── commonMain/
│   │   ├── kotlin/.../inbox/
│   │   │   ├── App.kt               # Root composable (Terrakok demo replaced by feature tree)
│   │   │   ├── theme/               # MaterialKolor-generated light/dark schemes (in place)
│   │   │   ├── data/
│   │   │   │   ├── network/         # Ktor client, AgenticInboxApi, DTOs, auth interceptor
│   │   │   │   ├── local/           # Room (KMP): entities, DAOs, AgenticInboxDatabase
│   │   │   │   └── repository/      # Single source of truth, offline-first
│   │   │   ├── domain/
│   │   │   │   ├── model/            # Email, Thread, Folder, Mailbox, Attachment
│   │   │   │   └── usecase/         # SendEmail, MoveEmail, SearchEmails, ...
│   │   │   ├── ui/
│   │   │   │   ├── mailboxlist/     # Mailbox picker / onboarding
│   │   │   │   ├── inbox/           # Folder + email list (threaded)
│   │   │   │   ├── thread/          # Conversation reader
│   │   │   │   ├── compose/         # Composer (new / reply / forward)
│   │   │   │   ├── search/          # Search + filters
│   │   │   │   ├── settings/        # Mailbox settings
│   │   │   │   └── components/      # Shared: EmailListItem, AvatarChip, FolderRow...
│   │   │   └── di/                  # Koin modules
│   │   └── composeResources/        # Drawables, strings, fonts (CMP resources)
│   ├── androidMain/
│   │   └── kotlin/.../inbox/
│   │       ├── Html WebView renderer (JS off, no remote loads by default)
│   │       ├── File picker, share sheet
│   │       ├── Ktor OkHttp engine + EncryptedSharedPreferences
│   │       └── MainViewModel factories (using navigation3 ViewModelScenario)
│   └── iosMain/
│       └── kotlin/.../inbox/
│           ├── MainViewController(): UIViewController (ComposeUIViewController)
│           ├── WKWebView HTML renderer
│           ├── PHPicker / document picker, share sheet
│           ├── Ktor Darwin engine + Keychain token storage
│           └── ksp iosArm64 / iosSimulatorArm64

iosApp/                              # Xcode project (UIKit App)
├── iosApp.xcodeproj/
└── iosApp/
    ├── iosApp.swift                 # UIApplicationMain -> embeds SharedUI framework
    ├── Info.plist
    └── Assets.xcassets
```

**Stack** (matches `gradle/libs.versions.toml`):

| Concern | Choice | Version | Notes |
|---|---|---|---|
| UI | Compose Multiplatform + Material 3 | CMP 1.11.1 / M3 1.11.0-alpha07 | Static MaterialKolor palette (in place) |
| DI | Koin | 4.x | Not yet in catalog — add before M0 |
| Networking | Ktor Client + kotlinx.serialization | Ktor 3.5.0, kx-serialization 1.11.0 | OkHttp engine (androidMain), Darwin engine (iosMain) |
| Local cache | Room KMP (SQLite) | 2.8.4 | `room { schemaDirectory(...) }`; KSP for all 3 targets |
| State | lifecycle-viewmodel-compose KMP | 2.11.0-beta01 | StateFlow SSOT |
| Navigation | Navigation 3 (KMP) | 1.1.1 | `navigation3-ui` + `lifecycle-viewmodel-navigation3`. Replaces the earlier Jetpack Navigation Compose plan |
| Images/HTML | Platform WebView wrappers | — | See Section 6 |
| Avatars | Coil 3 + coil-network-ktor3 | 3.4.0 | In catalog |
| Date/time | kotlinx-datetime | 0.8.0 | In catalog |
| Logging | Kermit | 2.1.0 | In catalog |
| Settings/tokens | multiplatform-settings + encrypted storage | TBD | **Not yet in catalog — add**; EncryptedSharedPreferences (Android) / Keychain (iOS) |
| Paging | Manual page/limit with `LoadState` wrapper | — | Server already paginates with `page`/`limit`; Paging 3 KMP iOS edges not worth the cost |
| Build config | gmazzo/gradle-buildconfig-plugin | 5.6.5 | In catalog (unused so far) |
| Kotlin / AGP / KSP | Kotlin 2.4.0, AGP 9.0.0, KSP 2.3.9 | — | KMP+AGP9 migration patterns understood |

---

## 3. Auth — decide this first

The Worker requires a `cf-access-jwt-assertion` header in production, validated against Cloudflare Access JWKS. Anyone passing the Access policy sees **all mailboxes** (by design upstream). Two viable paths:

**Option A — Access Service Token (recommended for personal/team use):**
Create a Cloudflare Access service token; the app sends `CF-Access-Client-Id` and `CF-Access-Client-Secret` headers on every request, and Access injects the JWT. App stores the pair in EncryptedSharedPreferences (Android) / Keychain (iOS), entered once on a login screen. Simplest by far; suits the self-hosted, single-operator nature of this project.

**Option B — Interactive Access login:**
Open the Access login page in a Custom Tab / ASWebAuthenticationSession, capture the `CF_Authorization` cookie, replay it as the JWT header. More "real-user" but fragile (cookie lifetime, IdP variations). Defer unless multi-user matters.

Auth layer design: Koin-provided `AuthInterceptor` lives in `sharedUI/commonMain`, attaches credentials on every call, and surfaces 403s as a `SessionExpired` event → Nav3 routes the user back to the credentials screen. Credentials are stored via `multiplatform-settings`+encrypted backend: `EncryptedSharedPreferences` (androidMain) and Keychain with `kSecAttrAccessibleAfterFirstUnlock` (iosMain, so the v1.5 background poll can read the token without unlock). Add `androidx.security:security-crypto` to the Android catalog.

---

## 4. API layer (exact contract from the repo)

Base: `https://<your-worker-domain>`. All JSON.

```kotlin
interface AgenticInboxApi {
    // Config
    suspend fun getConfig(): ConfigDto                                    // GET /api/v1/config

    // Mailboxes
    suspend fun listMailboxes(): List<MailboxDto>                         // GET /api/v1/mailboxes
    suspend fun createMailbox(body: CreateMailboxDto): MailboxDto        // POST /api/v1/mailboxes
    suspend fun getMailbox(id: String): MailboxDto                        // GET /api/v1/mailboxes/{id}
    suspend fun updateMailbox(id: String, settings: JsonObject)          // PUT /api/v1/mailboxes/{id}
    suspend fun deleteMailbox(id: String)                                 // DELETE /api/v1/mailboxes/{id}

    // Emails
    suspend fun getEmails(mailboxId: String, folder: String?, threaded: Boolean?,
                          threadId: String?, page: Int?, limit: Int?,
                          sortColumn: String?, sortDirection: String?): EmailPageDto
    suspend fun sendEmail(mailboxId: String, body: SendEmailRequestDto): SendEmailResponseDto
    suspend fun deleteEmail(mailboxId: String, emailId: String)
    suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String)
    suspend fun replyEmail(mailboxId: String, emailId: String, body: ReplyDto)
    suspend fun forwardEmail(mailboxId: String, emailId: String, body: ForwardDto)

    // Threads
    suspend fun getThread(mailboxId: String, threadId: String): List<EmailFullDto>
    suspend fun markThreadRead(mailboxId: String, threadId: String)

    // Folders
    suspend fun getFolders(mailboxId: String): List<FolderDto>
    suspend fun createFolder(mailboxId: String, name: String): FolderDto
    suspend fun renameFolder(mailboxId: String, folderId: String, name: String): FolderDto
    suspend fun deleteFolder(mailboxId: String, folderId: String)

    // Search
    suspend fun search(mailboxId: String, query: String, filters: SearchFilters,
                       page: Int?, limit: Int?): EmailPageDto

    // Attachments (returns bytes, Content-Disposition set by server)
    suspend fun downloadAttachment(mailboxId: String, emailId: String, attachmentId: String): ByteArray
}
```

**Key DTOs** (mirroring `workers/lib/schemas.ts`):

```kotlin
@Serializable
data class EmailMetadataDto(
    val id: String, val subject: String, val sender: String, val recipient: String,
    val cc: String? = null, val bcc: String? = null, val date: String,
    val read: Boolean, val starred: Boolean, val in_reply_to: String? = null,
    val thread_id: String? = null, val folder_id: String? = null, val snippet: String? = null,
)

@Serializable
data class EmailFullDto( /* metadata + */ val body: String? = null,
    val message_id: String? = null, val attachments: List<AttachmentDto> = emptyList())

@Serializable
data class AttachmentDto(val id: String, val filename: String, val mimetype: String,
    val size: Long, val content_id: String? = null, val disposition: String? = null)

@Serializable
data class SendEmailRequestDto(
    val to: List<String>, val cc: List<String>? = null, val bcc: List<String>? = null,
    val from: FromDto,                     // { email, name }
    val subject: String,
    val html: String? = null, val text: String? = null,   // at least one required
    val attachments: List<OutboundAttachmentDto>? = null, // base64 content
    val in_reply_to: String? = null, val references: List<String>? = null,
    val thread_id: String? = null,
)
```

System folder IDs (from `shared/folders.ts`): `inbox`, `sent`, `draft`, `archive`, `trash`, `spam`.

**Drafts note:** Draft rows live in the `draft` folder; the composer saves drafts by writing an email into that folder and passing `draft_id` on update. The mobile composer should autosave to the local Room cache every few seconds and sync to the server draft folder on pause/exit — matching web behavior without chattiness.

---

## 5. Offline & repository strategy

Offline-first read path, online-required write path (this is an email client — sends must hit the server):

- **Room entities:** `MailboxEntity`, `FolderEntity`, `EmailEntity` (metadata + body + `folderId` + `threadId` + `syncState`), `AttachmentEntity` (metadata only; files cached in app storage on demand).
- **Repository pattern:** each folder screen collects a Flow from Room; a refresh triggers a network page fetch that upserts into Room. Server timestamps (`date`) drive ordering. `totalCount` from the API drives pagination end detection.
- **Optimistic actions:** mark-read, star, move, delete apply to Room immediately, fire the API call, and roll back on failure with a snackbar. (Star is local + server field — API exposes `is_starred` in search, and the DO stores `starred`; verify the update endpoint during M1 since the route file truncates there — flagged in Risks.)
- **Send queue:** compose "Send" enqueues locally with `syncState = PENDING`, sends via API, moves the local copy to `sent` on success; on failure keeps it visible in an "Outbox" pseudo-folder with retry.

---

## 6. UI design

Material 3, dynamic color on Android, matching static palette on iOS. Adaptive: single-pane on phones, list–detail two-pane on tablets/landscape (like the web `MailboxSplitView`).

**Screens:**

1. **Onboarding / Server setup** — Worker URL + Access credentials (Option A token pair). Validates via `GET /api/v1/config`.
2. **Mailbox picker** — list of mailboxes with unread badge; FAB to create (address restricted to configured domains from config). Long-press → settings/delete.
3. **Inbox (main screen)** — top app bar with mailbox name + search icon; modal navigation drawer listing system folders, custom folders (+ manage), and mailbox switcher. Email list is threaded when folder = inbox: sender avatar chip (initials/color hash), sender, snippet, time, star toggle, attachment icon, unread emphasis. Swipe right = archive, swipe left = trash (configurable later). Pull-to-refresh. Multi-select action mode: move / delete / mark read.
4. **Thread view** — collapsed message cards, latest expanded; each expands to full HTML body rendered in a sandboxed platform WebView (JS disabled, remote images blocked behind a "Load images" tap — same privacy posture as the web `EmailIframe`). Attachment chips → download/preview/share. Bottom actions: Reply / Reply all / Forward.
5. **Composer** — full-screen; To/Cc/Bcc chip fields with validation, subject, body. v1 body is **plain text** with signature auto-append (the web uses TipTap rich text; a Compose rich text editor is a v1.5 stretch — see Milestones). Attachment picker (Photo Picker / file picker on Android, PHPicker/DocumentPicker on iOS) with base64 encoding and a size warning near the 25 MB email cap. Autosave to Drafts.
6. **Search** — dedicated screen with query field + filter bottom sheet (from, to, subject, date range pickers, read/starred/attachment toggles), paged results reusing the email list item.
7. **Mailbox settings** — from name, signature (enabled + text), auto-reply (enabled + subject + message), forwarding (enabled + address). Maps 1:1 to the settings JSON blob.
8. **Folder management** — create/rename/delete custom folders; system folders locked.

**Design details worth getting right:**
- Relative timestamps ("2m", "Yesterday", "Jul 3") with absolute time in thread view.
- Sender identity: color-hashed avatar from address, display name parsed from `"Name <addr>"` format.
- Empty states per folder with distinct illustrations/copy.
- Skeleton loaders for list + thread; error states with retry.
- Large-screen: `ListDetailPaneScaffold` (Nav3 + Material3-adaptive) so tablet users get the web-like split view.

---

## 7. Module & dependency setup

Two-module layout inherited from the Terrakok scaffold:

- `:androidApp` — `com.android.application`, Kotlin, depends on `:sharedUI`. 32-bit minSdk 23, target/compile SDK 36, JVM 17. Owns `AppActivity` + Android-specific platform hooks; no business logic here.
- `:sharedUI` — `com.android.kotlin.multiplatform.library`. KMP targets: `android`, `iosArm64`, `iosSimulatorArm64`. Static framework `SharedUI` exported to the existing Xcode project. Holds the whole app: theme, data, domain, UI, DI, navigation. Add `androidMain` as a real (currently missing) source set so `actual` declarations for the Ktor engine, encrypted storage, and WebView renderer have a home.
- iOS — the existing `iosApp` Xcode project calls `MainViewController()` from `iosMain`. No new framework plumbing needed; just keep `isStatic = true` and the `SharedUI` baseName.

Do **not** split into `:core:network`, `:core:database`, `:feature:*` for v1; the single `:sharedUI` module keeps refactor cost near-zero while the surface is small. Split only if compile times or team size force it.

Version catalog (`gradle/libs.versions.toml`) — already present for most of the stack (Kotlin 2.4.0, CMP 1.11.1, Ktor 3.5.0, Room 2.8.4, Coil 3.4.0, kx-datetime 0.8.0, Kermit 2.1.0, Nav3 1.1.1). **Add to catalog before M0:**

| Group | Library | Why |
|---|---|---|
| Koin 4.x | `koin-core`, `koin-compose`, `koin-compose-viewmodel` | DI |
| multiplatform-settings | `multiplatform-settings`, `multiplatform-settings-coroutines`, `multiplatform-settings-no-arg` | Credentials storage abstraction |
| Android security | `androidx.security:security-crypto` | `EncryptedSharedPreferences` |
| Ktor test | `ktor-client-mock` | MockEngine suite for Section 8 |

CI: GitHub Actions — `:androidApp:assembleDebug` + `:sharedUI:allTests` on PR; iOS framework compile check (no UI tests in v1 — explicit tradeoff) on a macOS runner; `detekt` + `ktlint`. Make the iOS-test omission a deliberate, documented decision in the workflow file.

---

## 8. Testing

- **Unit:** repository logic (paging merge, optimistic rollback), DTO serialization against captured real JSON from a deployed instance, date/thread grouping.
- **Integration:** Ktor MockEngine suite covering every endpoint contract; one live smoke test profile against a dev deployment (`npm run dev` locally exposes the API without Access).
- **UI:** Compose UI tests for list interactions, swipe actions, composer validation; screenshot tests for list item states.

---

## 9. Milestones

| Milestone | Deliverable | Est. |
|---|---|---|
| **M0 — Foundations** | Strip the Terrakok demo `App.kt`; add `androidMain` source set; extend `libs.versions.toml` with Koin, multiplatform-settings, security-crypto, ktor-client-mock; Koin modules + DI graph; Ktor client + auth interceptor; config/mailbox endpoints wired; onboarding + mailbox picker screens (state-driven routing). **Status:** Koin DI ✓, Ktor client ✓, config+mailbox endpoints ✓, Onboarding + MailboxPicker screens ✓; remaining: Nav3 routes skeleton, EncryptedSharedPreferences/Keychain upgrade, ktor-client-mock test suite | 1 wk |
| **M1 — Read path** | Room cache, folder drawer, threaded inbox list with paging, thread view with HTML rendering, mark read, pull-to-refresh. Verify star/read update endpoints against live server | 2 wks |
| **M2 — Write path** | Composer (plain text), send/reply/forward, drafts + outbox queue, move/delete/archive with swipe + multi-select, folder management | 2 wks |
| **M3 — Search & settings** | Search screen + filters, attachments (download/preview/share), mailbox settings, empty/error polish | 1.5 wks |
| **M4 — Ship** | Tablet split view, iOS polish pass, screenshot tests, Play internal track + TestFlight | 1.5 wks |
| **v1.5 (later)** | Rich text composer, background polling + local notifications for new mail (no push infra server-side), inline image (cid:) rendering, agent chat via WebView or native WS client | — |

Total v1: ~8 weeks part-time; considerably less if driven with Copilot/Claude Code agents like Foodie was.

---

## 10. Risks & open items

1. **Star/read single-email endpoints** — the routes file shows thread-level mark-read clearly; the per-email update (star toggle, read toggle) sits in a section to verify against the live code before M1. If absent, star can be client-local-only for v1 or a tiny upstream PR (repo is Apache 2.0).
2. **HTML email rendering** — the messiest part of any email client. Sandbox strictly: WebView with JS off, no remote loads by default, viewport-fit CSS injection for mobile widths.
3. **Access auth in production** — Option A service tokens must be tested early; if the deployed Access policy doesn't allow service tokens, that's a dashboard config change, not code.
4. **No push notifications** — Email Routing → DO has no push hook. v1 ships without notifications; v1.5 adds foreground polling / WorkManager–BGTaskScheduler background refresh.
5. **25 MB email cap** — enforce client-side on attachment total before send.
6. **All-mailboxes trust model** — inherited from upstream; the app should not pretend to have per-user privacy it doesn't.
