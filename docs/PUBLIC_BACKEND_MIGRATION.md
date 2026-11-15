# Plan: switch Mobile from Worker to agentic-inbox-public

Based on a code comparison of Mobile `feature/upstream-fixes`, Worker `develop` and
agentic-inbox-public `main`. Nothing here has been run against a live public deployment; items marked
**(verify)** are inferred from code and must be confirmed in Phase 0.

## Goal and rules

- Mobile talks to a public deployment with no broken buttons, then gains public's everyday features.
- **Decided: public is the only supported backend.** Worker-only code is deleted, not gated. Mobile documents a minimum public version (`main` including the inbox-polish merge) instead of probing for features.
- Repo conventions: failing test first (TDD), `docs/DESIGN_SYSTEM.md` tokens, 48dp targets, no "PR #n" in Mobile commit messages.

## Phase 0 - Verify against a real public deployment (0.5 day)

Run public locally (`npm run dev`) or on a dev Worker, point Mobile at it, and check:

1. `POST …/emails/:id/spam` status code (expected 405 from React Router's `*` route, **verify**).
2. Send with an attachment (Mobile's `encodeDefaults = false` drops `disposition`; the schema requires it, **verify**; affects Worker too).
3. Send a message the provider rejects: confirm 502 body shape and what Mobile shows.
4. Create a mailbox when `EMAIL_ADDRESSES` is restricted (403) and when the address is an alias (409).
5. `GET /api/v1/config` returns only `domains` and `emailAddresses`: confirm onboarding and the mailbox picker work with it.
6. `GET …/folders` includes the `spam` folder (needed for the Phase 1.1 replacement).

Exit: a short list of confirmed bugs feeding Phase 1.

## Phase 1 - Make Mobile correct on public (1-2 days)

| # | Change | Test first |
|---|---|---|
| 1.1 | Replace "Mark as spam" (which calls the Worker-only `/spam` endpoint) with a plain move to the Spam folder via `moveEmail`; public seeds a `spam` folder in its first migration (**verify** it is returned by `GET …/folders`). Or drop the button if you don't want it. | `ThreadViewModelTest` / `EmailRepositoryImplTest` |
| 1.2 | Fix attachment `disposition` encoding (`@EncodeDefault` or send it explicitly). | `EmailRepositoryImplTest` asserts request body contains `disposition` |
| 1.3 | Map 502 (and 403/409 on mailbox create) to `ApiException` with a presentable message. | `ApiErrorMappingTest` |
| 1.4 | **Delete the Worker-only code** (full list in Phase 4 step 4). Do this in Phase 1 so nothing Worker-specific ships on public. | existing suites still green |
| 1.5 | Update README ("backend used as-is") and settings copy to name public as the supported backend, with the minimum version. | n/a |

Exit: Mobile passes its test suite and a manual smoke test on public: onboarding, list, read, send, reply, forward, move, search, folders, attachments, settings.

## Phase 2 - Capability discovery: dropped

With public as the only backend there is no need for a `features` object in public's `/config`. Mobile assumes
a recent public version. If you later need to support older public deployments, add a `features` object to
`GET /api/v1/config` in public then (small change plus a route test).

## Phase 3 - Close the feature gap, in value order (each is its own PR)

1. **Bulk endpoint** (`POST …/emails/bulk`): replace the per-email loops in `InboxViewModel.batchArchive/batchDelete/batchMark*` with one call. Smallest change, immediate win. Needs the public bulk request schema (`workers/lib/bulk.ts`).
2. **Labels**: API ×4, label chips on list and thread, label filter (`?label=`), apply/remove in selection toolbar. Room schema migration for cached labels.
3. **Aliases + From picker**: read `settings.aliases` from the mailbox, From picker in compose, alias editor in settings. Preserves the existing merge-on-save behaviour.
4. **Send later**: schedule from compose, Scheduled screen (list and cancel). Note public does not allow attachments on scheduled sends.
5. **Recipient autocomplete** (`GET …/contacts?q=`): To/CC/BCC suggestions.
6. **Live updates** (WebSocket `…/events`): refresh list on events; fall back to pull-to-refresh when the socket fails. Needs a Ktor WebSocket client on both platforms.
7. **Search grouped by conversation** (`group=thread`) and server-side threaded list (`threaded=true`; also gives `thread_count`).
8. **Settings parity**: auto-draft toggle, AI model override, agent system prompt (only if a mobile user would set them; otherwise keep preserving them on save).
9. **Optional**: undo send (client-side hold), server drafts via `POST …/drafts`, rich signature.

Out of scope unless wanted: AI agent panel, MCP, keyboard shortcuts.

## Phase 4 - Cut-over and cleanup

1. Deploy public to a **new** Worker name first, point a test build of Mobile at it, and migrate by re-importing or leaving the old Worker read-only.
2. If deploying public **over** the existing Worker: Durable Object migrations are tracked by name, so existing data is safe (the `is_spam` column stays, harmless). But Worker's `wrangler.jsonc` has an `ImportJobDO` class (migration v4) that public lacks, so add a `deleted_classes` migration or the deploy is rejected.
3. Existing agent keys and `/agent/api` stop working; tell any automation using them first.
4. (Done in Phase 1.4.) The Mobile code to delete, all of it unreachable on public:
   - Agent access keys: `AgentKeyDtos`, `AgentKeyRepositoryImpl`, `AgentKey`, `AgentKeyRepository`, Create/List/RevokeAgentKey use cases, `ui/mailbox/agentaccess/*`, their tests and `FakeAgentKeyRepository`, the three `agent-keys` methods in `AgenticInboxApi`, and the navigation entry and settings link.
   - AI compose rewrite: `RewriteDraftUseCase`, `composeRewrite` and its DTOs, the rewrite button and `aiRewriteAvailable` in `ComposeScreen`/`ComposeViewModel`.
   - Spam endpoint: `markEmailSpam`, `SpamEmailDto`/`SpamEmailResponseDto`, `MarkSpamOutcome`, `MarkEmailSpamUseCase` (replaced by a folder move).
   - Feature discovery: `FeatureFlagsDto`, `GetServerFeaturesUseCase`, `FeatureUnavailableException`, `ServerFeaturesTest`, and the `signaturesAvailable`/`autoReplyAvailable`/`agentAccessAvailable` state in the settings view model; `ConfigDto` keeps only `domains` and `emailAddresses`.
   - Auto-reply settings: the three form fields and the auto-reply parsing in `MailboxSettingsMapping`/`MailboxRepositoryImpl`. Keep merge-on-save so an existing `autoReply` key in a mailbox's settings is preserved.
   - Demo data (`DemoDataModule`) that references the removed flags.
5. Tag a release and update store listings and docs (the README currently says the app works with the upstream Worker "as-is").

## Rough size

Phase 0-1: about 2-3 days (Phase 1 includes the deletions). Phase 3 items 1, 3, 5 are roughly a day each; labels, send later and live updates are 2-3 days each.

## Phase 0 results (run against agentic-inbox-public `main` locally, simulated bindings)

| Check | Result |
|---|---|
| `POST …/emails/:id/spam` | **405**, HTML body (React Router `*` route). Not 404/501, so Mobile's fallback never triggers. |
| `POST …/ai/compose-rewrite` | 405, HTML body. |
| `GET …/agent-keys` | **200 with the SPA's HTML** (not JSON). |
| Send with an attachment that has no `disposition` | **400** `{"error":"Invalid request body","details":{"fieldErrors":{"attachments":["Required"]}}}`. Confirmed Mobile bug (`encodeDefaults = false` drops the default). |
| Send with `disposition` set | 202 `{"id", "status":"sent"}`. |
| Send with `from` object missing `name` | 400. Mobile always sends a name string, so no change needed. |
| Send rejected by the provider | **502** `{"error":"Failed to send email: …"}`. Mobile maps only 400/429. |
| Create a mailbox that exists | 409 `{"error":"Mailbox already exists"}`. |
| `GET …/folders` | Includes `spam` (6 folders: archive, draft, inbox, sent, spam, trash). |
| `GET /config` | `{"domains":[…],"emailAddresses":[]}`, no `features`. |
| `POST …/emails/bulk` | Zod 400 on `{}` (`action` required). |
| Mailbox create with restricted `EMAIL_ADDRESSES` (403) | Not run (needs config); read from code only. |

## Progress

| Phase | Status | Commit |
|---|---|---|
| 0 Verify against public | done | docs: add public backend migration plan and phase 0 verification results |
| 1 Fixes (disposition, 409/502/403) | done | fix(network): send attachment disposition and present 409/502/403 errors |
| 1 Remove Worker-only code, spam becomes a move to Spam | done | refactor: target agentic-inbox-public and remove Worker-only features |
| 3.1 Bulk endpoint | done | feat(inbox): apply batch actions with one bulk request |
| 3.2 Labels | done | feat(labels): create, edit, filter and apply labels |
| 3.3 Aliases and From picker | not started | |
| 3.4 Send later | not started | |
| 3.5 Recipient autocomplete | not started | |
| 3.6 Live updates | not started | |
| 3.7 Search grouped by conversation, threaded list | not started | |

Verified live against a local public instance: `POST …/emails/bulk` accepts Mobile's body, rejects more than 200 ids with a 400,
returns 400 `Folder not found` for an unknown move target, and star/delete changed real messages.
