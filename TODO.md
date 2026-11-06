# TODO

## Specification phase

- [x] Define the six requested sources and mark tokenless-access feasibility honestly.
- [x] Set privacy, terms-of-service, robots, rate-limit, and secret-handling constraints.
- [x] Review source capability for the first feed slice; keep undocumented or OAuth-only routes gated.

## Before implementation

- [x] Implement Google News search RSS with an explicit conditional capability status; endpoint stability still needs monitoring.
- [x] Add Google Alerts RSS URL ingestion gated to user-provided URLs; determine separately whether the user's account exposes an RSS URL.
- [x] Implement public WordPress RSS/Atom collection with URL safety checks.
- [ ] Verify current authorized access paths for Reddit, Issuu, and Mercado Livre. Mark a source unavailable when anonymous access is not permitted.
- [x] Select and implement the initial feed vertical slice: Google News, conditional Google Alerts, and WordPress.
- [ ] Approve API, persistence, scheduling, and retention requirements.

## Later implementation slices

- [x] Create a Java 21 / Maven command-line collector.
- [x] Add RSS/Atom parser tests, source-specific tests, bounded HTTP fetcher, and URL safety checks.
- [ ] Revisit the Google News RSS URL format if Google changes the undocumented endpoint.
- [ ] Confirm whether this Google Alerts account exposes an RSS feed; keep the URL in a secret store/local environment only.
- [ ] Harden WordPress DNS rebinding protection before exposing collection as a hosted service.
- [ ] Add persistent storage only after retention and deletion behavior are agreed.
- [ ] Consider ActiveMQ Classic only if refresh jobs need asynchronous delivery. Consider Kafka only if replay or multiple independent consumers become a real requirement.
- [ ] Add REST/OpenAPI endpoints after the first parser and fetcher slice is accepted and network access is threat-reviewed.
