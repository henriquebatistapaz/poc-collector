# TODO

## Specification phase

- [x] Define the six requested sources and mark tokenless-access feasibility honestly.
- [x] Set privacy, terms-of-service, robots, rate-limit, and secret-handling constraints.
- [x] Keep AI-Ready generated files and `/docs/` out of Git.
- [ ] Review and approve the source feasibility matrix.

## Before implementation

- [ ] Run a source-specific feasibility spike for Google News RSS and confirm current availability, terms, and response format.
- [ ] Confirm whether Google Alerts exposes an RSS delivery option for this account; if yes, store its feed URL as a secret.
- [ ] Verify current authorized access paths for Reddit, Issuu, and Mercado Livre. Mark a source unavailable when anonymous access is not permitted.
- [ ] Select a first vertical slice, recommended: WordPress RSS/Atom feeds.
- [ ] Approve API, persistence, scheduling, and retention requirements.

## Later implementation slices

- [ ] Create a Java 21 / Spring Boot / Maven service after the source-access decisions are approved.
- [ ] Add feed URL safety checks, parser tests, and a bounded HTTP fetcher.
- [ ] Add one permitted adapter at a time with fixtures and source-specific tests.
- [ ] Add persistent storage only after retention and deletion behavior are agreed.
- [ ] Consider ActiveMQ Classic only if refresh jobs need asynchronous delivery. Consider Kafka only if replay or multiple independent consumers become a real requirement.
- [ ] Add REST/OpenAPI endpoints after the first parser and fetcher slice is accepted.
