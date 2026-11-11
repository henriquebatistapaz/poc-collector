# Multi-Source Collector Mini — Initial Specification

**Status:** Superseded by the current implementation and decisions in [SPEC.md](../../SPEC.md). Historical specification; do not use as the current source plan.
**Repository:** Private
**Scope:** Historical initial proposal; implementation status and remaining external gates are recorded in the current project specification.

## 1. Purpose

The initial request proposed a small, private service for public links and limited metadata. Subsequent decisions removed Mercado Livre and implementation proceeded in `poc-collector`:

1. Google News
2. Google Alerts
3. Reddit (official OAuth; live access gated by Reddit approval)
4. Issuu (unverified; unavailable)
5. WordPress

Mercado Livre was explicitly removed from scope.

The service must use only access methods that are currently documented or explicitly permitted. A requested source is not a promise that anonymous collection is technically available or allowed. If a source requires a token, written permission, or a platform-approved integration, its anonymous connector must remain disabled instead of bypassing the restriction.

## 2. Product boundary

### In scope

- A common source-connector contract and source capability status.
- Public links and the minimum metadata needed to identify and deduplicate a result.
- Per-source feasibility checks before implementation.
- Respect for source terms, robots rules, rate limits, privacy, and content removal requests.
- A future Java 21, Spring Boot, Maven implementation, following the existing private mini-project conventions.

### Out of scope for this specification-only slice

- Live requests, web scraping, browser automation, or undocumented endpoint calls.
- Implemented adapters, persistence, API endpoints, scheduling, or message brokers.
- Collecting full article/document bodies, personal profiles, or private content.
- Circumventing login, CAPTCHA, anti-bot controls, rate limits, or access restrictions.

## 3. Source feasibility matrix

| Source | Requested tokenless route | Current assessment | Implementation gate |
|---|---|---|---|
| WordPress | Public RSS 2.0 / RDF / Atom feeds exposed by a site | **Supported when the site publishes a public feed.** WordPress documents built-in RSS and Atom feed URLs. A site can still disable or restrict its feed. | Accept a feed URL supplied by the user; fetch only after safety and permission checks. |
| Google News | Public RSS search feed | **Candidate; not an officially documented API in the sources reviewed.** The commonly used RSS URL pattern must be re-verified against live behavior, terms, and stable response format. | A small manual feasibility spike is required before enabling the adapter. Do not scrape Google News HTML. |
| Google Alerts | RSS delivery for a user-configured alert | **Conditional and not established by current official help documentation.** Google help documents creating alerts and email settings; it does not document an RSS endpoint. If an account offers RSS delivery, its URL is account-provisioned and sensitive. | User supplies a feed URL only if the account actually exposes one. Treat it as a secret; never commit, return, or log it. Otherwise mark unavailable. |
| Reddit | Anonymous public feed or other no-token route | **Not cleared.** Reddit's current User Agreement prohibits collection except as permitted and prohibits scraping without prior written consent. Data API access uses the supplied access information (for example, OAuth tokens). | Do not implement anonymous scraping. Revisit only if an official permitted no-token feed is confirmed in writing/documentation; otherwise mark unavailable. |
| Issuu | Anonymous public feed/API | **Unverified.** No official, stable tokenless feed or API contract has been established for this project. | Research official documentation and terms first. No HTML scraping or hidden endpoints; mark unavailable if no permitted route exists. |

**Meaning of “tokenless”:** no API token or OAuth credential is sent by the collector. It does not waive a source's terms, account setup, explicit consent, or access restrictions. The Google Alerts feed URL is a special secret even if it is not an OAuth token.

## 4. Functional requirements

Future implementation must:

- Represent each source with `AVAILABLE`, `CONDITIONAL`, `UNVERIFIED`, or `UNAVAILABLE` capability status and an evidence/review date.
- Accept only sources whose current capability gate has passed.
- Collect only public item links and limited feed metadata: source, stable item ID when available, title, canonical link, publication time, and retrieval time.
- Normalize links and deduplicate using `(source, stable source item ID)`, falling back to a normalized canonical URL only when no stable ID exists.
- Preserve source attribution and the original item URL.
- Stop a connector after access denial, robots prohibition, repeated throttling, or a policy-related response; do not retry aggressively.
- Allow each connector to be disabled without affecting other permitted sources.
- Record enough status and error information to diagnose a feed without logging credentials, cookies, alert feed URLs, or full content.

## 5. Non-functional and safety requirements

- Enforce HTTPS by default; any HTTP exception must be explicit and justified.
- Prevent SSRF: reject loopback, private, link-local, multicast, reserved IP ranges, and unsafe DNS resolutions; repeat checks after redirects and DNS changes.
- Use strict connect/read timeouts, bounded response size, bounded redirects, per-host concurrency, and conservative rate limits.
- Do not forward authorization, cookies, or user-supplied headers across origins or redirects.
- Parse XML with DTDs and external entities disabled; impose depth, item-count, and document-size limits.
- Store only minimal metadata; define retention, deletion/takedown handling, and source-specific policy before production use.
- Keep credentials and feed URLs with bearer-like semantics in a secret store or local ignored environment, never in Git or logs.
- Keep the repository private until the owner explicitly chooses otherwise.

## 6. Proposed architecture (not implemented)

- **Source registry:** source name, capability state, evidence reference, and enablement flag.
- **Connector interface:** validates configuration, fetches within configured limits, and returns normalized candidates.
- **Feed parser:** RSS 2.0 and Atom 1.0 parsing for permitted feed sources.
- **Normalizer/deduplicator:** stable source identity and canonical URL normalization.
- **Collection service:** coordinates enabled adapters and reports partial failures without hiding source status.
- **Storage:** undecided; start in memory only if a first implementation slice is approved. Choose durable storage only after retention and deletion requirements are agreed.
- **API:** undecided; define endpoints and OpenAPI contract after the first permitted source slice is approved.

Do not add a message broker to the initial parser/collection slice. ActiveMQ Classic is a later option if refresh jobs require asynchronous delivery. Kafka is relevant only if replayable event history or multiple independent consumers become a demonstrated need.

## 7. Delivery plan

### Phase 0 — Source feasibility

Confirm each public access path, the applicable terms/robots rules, response format, rate limits, and a small set of allowed test URLs. No adapter code is written in this phase.

### Phase 1 — First permitted feed slice

Recommended starting point: user-supplied public WordPress RSS/Atom feeds. Add URL safety validation, RSS/Atom parsing, normalization, deduplication, and deterministic JUnit tests using local fixtures. No scheduler or persistence.

### Phase 2 — Additional permitted sources

This historical plan has been superseded: Google News, user-provided Google Alerts feeds, and WordPress feeds are implemented. Reddit OAuth and the official API reader are implemented but remain disabled until Reddit access approval and runtime credentials are available. Issuu remains unverified. Mercado Livre was removed from scope.

### Phase 3 — API and persistence

Define a small OpenAPI contract and persistence model only after collection behavior, data retention, deletion, and operational needs have been approved.

## 8. Acceptance criteria for this specification

- The original requested sources have a documented route assessment and implementation gate; Mercado Livre has since been removed from scope.
- The spec makes no promise of tokenless access where official evidence is absent or access is restricted.
- No implementation code, credentials, or real private feed URL is included.
- The repository remains private; `/docs/` and AI-Init generated files are ignored by Git.
- A first implementation slice cannot begin until source feasibility and scope are approved.

## 9. References

- [WordPress: Feeds](https://developer.wordpress.org/advanced-administration/wordpress/feeds/) — documents built-in RSS and Atom feed types and URLs.
- [Google Search Help: Create an alert](https://support.google.com/websearch/answer/4815696) — documents alert creation and notification settings; does not define an RSS API.
- [Reddit User Agreement](https://redditinc.com/policies/user-agreement) — collection is allowed only under applicable terms; scraping without prior written consent is prohibited.
- [Reddit Data API Terms](https://redditinc.com/policies/data-api-terms) — governs Data API access and required access information.
- [RFC 4287: The Atom Syndication Format](https://www.rfc-editor.org/rfc/rfc4287) — future parser format reference.
- [RSS 2.0 Specification](https://www.rssboard.org/rss-specification) — future parser format reference.

**Review note:** This historical matrix is not the current implementation plan. Re-check platform documentation and policies before enabling any gated connector.
