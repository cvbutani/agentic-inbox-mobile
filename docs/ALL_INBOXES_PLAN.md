# Plan: All Inboxes (per domain)

One merged inbox per domain: every mailbox on `example.com` in a single list, with pagination, search,
offline cache and a sensible From for compose. Based on reading `agentic-inbox-public` `main`
(the decided only-supported backend, see `PUBLIC_BACKEND_MIGRATION.md`) and Mobile `master`.
Nothing here has been built or run. Items marked **(verify)** are inferred from code.

## Decisions already made

- Merge on the **server** (new domain-scoped API), not in the app.
- Pagination and search are in scope from day one.
- Push notifications are out of scope.
- Offline: write to Room in the same call that fetches, never a second request just to fill the cache.
- Repo rules: failing test first, `DESIGN_SYSTEM.md` tokens, 48dp targets, no "PR #n" in Mobile commits.

## What the code looks like today (why this shape)

- Each mailbox is its own Durable Object (SQLite). Every email/search/folder endpoint is
  `/api/v1/mailboxes/:mailboxId/...`, so nothing today can read across mailboxes.
- `GET /api/v1/mailboxes` returns `{ id, email, name: id }` only: the display name (`settings.fromName`)
  lives in the mailbox's R2 JSON and is not sent. That is the cause of the "mailbox list shows no name" bug.
- Pagination is `page`/`limit` offset on a per-mailbox query, with a separate count call.
  `getThreadedEmails` is raw SQL grouping messages into conversation rows.
- Mobile's Room `emails` table is already keyed `(mailboxId, id)`, so merged rows fit it without a migration.
- Compose already has a From row with a dropdown (mailbox + aliases). Sending is per mailbox:
  `POST /mailboxes/:mailboxId/emails`, and the server requires From to be that mailbox or one of its aliases.

## Phase 0 - two small fixes first (own PR, before All Inboxes)

1. **`fromName` in the mailbox list.** Worker: `GET /mailboxes` reads each mailbox's R2 object in parallel and adds
   `fromName` (and keeps `name`). Mobile: `MailboxDto.fromName`, mapper uses it, and the existing
   `getMailboxes` upsert writes it to Room in that same call, so no extra requests. The "carry forward cached
   fromName" merge stays as the offline fallback.
   - Later optimisation if lists get slow: store `fromName` in R2 `customMetadata` on write and read it from
     `bucket.list({ include: ['customMetadata'] })` (needs a one-off backfill).
2. **Drawer counts on refresh.** `onRefresh` and `selectFolder` reload only the email page; folder/label counts
   reload only in the live-update path. Reload them there too, and adjust the unread count immediately on
   archive / delete / read toggle so the drawer never lags.

## Phase 1 - Worker: domain-scoped API (`agentic-inbox-public`)

New routes, behind the same auth as the mailbox routes. `:domain` must be in `DOMAINS`, else 404.

| Route | Returns |
|---|---|
| `GET /api/v1/domains/:domain/emails?folder&threaded&limit&cursor` | `{ emails: [{...summary, mailboxId}], totalCount, nextCursor }` |
| `GET /api/v1/domains/:domain/search?query&folder&from&to&subject&date_start&date_end&is_read&is_starred&has_attachment&limit&cursor` | same shape |
| `GET /api/v1/domains/:domain/folders` | system folders with unread counts summed across the domain's mailboxes |

How the merge works:
- List mailboxes in the domain (R2 prefix list, filter by email domain), fan out to each Durable Object in parallel.
- Each DO returns up to `limit` rows older than the cursor; the Worker k-way merges by `(date DESC, id DESC)`,
  keeps the top `limit`, and returns a cursor for the next page.
- **Cursor = keyset `(date, id)`, base64 opaque.** This needs a small DO change: `beforeDate`/`beforeId` options on
  `getEmails`, `getThreadedEmails` and `searchEmails`. Keyset (not offset) so archiving or deleting a row on page 1
  can't make page 2 skip rows, which today's page-number pagination can.
  - Alternative with no DO change: cursor carries a per-mailbox offset. Simpler, but it skips rows after deletes.
- `totalCount` = sum of each mailbox's existing count method (threaded count for threaded lists).
- A conversation row belongs to one mailbox. The same thread received in two mailboxes shows as two rows (v1).
- Every row carries `mailboxId`; all mutations stay on the existing per-mailbox routes.

Tests first (vitest, existing `test-utils`): merge order across 3 mailboxes, tie on equal dates, cursor round-trip
with no gaps or duplicates, limit cap (100), unknown domain 404, a mailbox in another domain excluded, search
filters applied per mailbox, summed counts, empty domain.

## Phase 2 - Mobile data layer

- `EmailSummary.mailboxId` (non-null; per-mailbox lists fill it from the request).
- API: `listDomainEmails`, `searchDomain`, `getDomainFolders`; repository methods take an `InboxScope`
  (`Mailbox(id)` or `Domain(name)`), so the view model doesn't branch on endpoints.
- Cursor pagination replaces `page` for the domain scope (store `nextCursor`; per-mailbox scope keeps `page`).
- **Room:** merged rows upsert into the existing `emails` table in the same call. Offline All Inboxes is a new DAO query:
  `WHERE mailboxId IN (:ids) AND folderId = :folder ORDER BY date DESC` (the ids come from the cached mailbox list).
  Domain folder counts are summed from cached per-mailbox folders when offline. Offline shows only what was fetched.

## Phase 3 - Mobile UI

- **Entry:** on the mailbox picker, each domain with 2+ mailboxes gets an "All inboxes" row at the top of its group
  (unread badge). New nav key `AllInboxes(domain)`.
- **Screen:** reuse `InboxScreen`/`InboxViewModel` with an `InboxScope`. No parallel screen.
- **Row:** a small mailbox label on each row (display name or local part) so you can tell which address received it.
- **Drawer:** system folders only, with domain-summed counts. Custom folders and labels are per-mailbox, so they
  are not shown in All Inboxes (v1).
- **Actions:** swipe, star, read, move, delete route to the row's own `mailboxId`. Selection mode groups by
  `mailboxId` and sends one bulk request per mailbox, reporting partial failures as today.
- **Search:** `SearchScreen` takes the scope; results show the mailbox label.
- **Settings gear:** not shown in All Inboxes (settings are per mailbox).

## Phase 4 - Compose: what is "From"?

Recommendation: **bottom sheet on phones, anchored popup on tablets**, using the existing From row as the trigger.
Reason: each choice needs two lines (display name + address) and, in All Inboxes, there are several mailboxes each
with aliases. A plain dropdown gets cramped and truncates addresses.

| Situation | From |
|---|---|
| New message from a single mailbox | that mailbox (as today); alias picker only if it has aliases |
| New message from All Inboxes | last-used mailbox for this domain (remembered on-device), else the first mailbox. Sheet lists every mailbox in the domain, grouped, aliases indented under their mailbox |
| Reply / reply-all / forward | **locked to the owning mailbox** (the row's `mailboxId`); defaults to the alias the message was addressed to (existing `ComposeFrom.defaultFor`). No cross-mailbox picker, because the sent copy and thread belong in that mailbox |

Mechanics: choosing a From in All Inboxes sets the target mailbox, so the send goes to
`/mailboxes/<chosen>/emails`, with no server change. Switching From swaps the signature
(and display name) for the chosen mailbox. Drafts save under the chosen mailbox.

## Phase 5 - Live updates

v1: open the existing per-mailbox socket for each mailbox in the domain (cap at ~8; above that, fall back to
refresh on resume + pull-to-refresh) and run the same debounced quiet refresh. A domain-scoped socket is a later
optimisation if mailbox counts grow.

## Order, and what ships separately

1. Phase 0 (Worker + Mobile, small PR).  2. Phase 1 (Worker).  3. Phase 2 + 3 (Mobile).  4. Search.  5. Compose From.  6. Live.
Phases 1 and 2 can be reviewed independently; Mobile depends on a deployed Worker with the new routes.

## Open questions

1. **Backend:** build the API in `agentic-inbox-public` `main` (recommended, matches the migration decision), or also back-port to `Worker` `develop`?
2. **Cursor:** keyset (recommended, needs a small DO change) or per-mailbox offset (no DO change, skips rows after deletes)?
3. **Threads across mailboxes:** same conversation in two mailboxes = two rows in v1. Acceptable?
4. **Domains with one mailbox:** hide All Inboxes (recommended) or always show it?
5. **Alias From on the deployed backend:** public supports sending from aliases; `Worker` `develop` does not (its sender check requires From == mailbox). **(verify)** whichever backend you deploy.
