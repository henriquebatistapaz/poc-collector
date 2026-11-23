# TODO

## Specification phase

- [x] Define the active source set and record feasibility decisions, including Reddit cancellation.
- [x] Set privacy, terms-of-service, robots, rate-limit, and secret-handling constraints.
- [x] Review source capability for the first feed slice; keep undocumented or OAuth-only routes gated.
- [x] Add ten public REST JSON sources with local-fixture tests and bounded HTTP transport.
- [x] Remove Google Alerts collection and scheduling support.

## Before implementation

- [x] Implement Google News search RSS with an explicit conditional capability status; endpoint stability still needs monitoring.
- [x] Implement public WordPress RSS/Atom collection with URL safety checks.
- [x] Cancel Reddit integration at the owner's request; remove OAuth implementation and tests, keep historical items readable, and cancel persisted Reddit schedules.
- [x] Research an official permitted Issuu route; keep the source disabled because no public publication search/feed route is documented.
- [x] Approve and implement the REST/OpenAPI, persistence, scheduling, and retention requirements for the local service.

## Later implementation slices

- [x] Create a Java 27 / Maven command-line collector.
- [x] Add RSS/Atom parser tests, source-specific tests, bounded HTTP fetcher, and URL safety checks.
- [ ] Revisit the Google News RSS URL format if Google changes the undocumented endpoint.

- [x] Pin WordPress feed requests to checked public DNS results and reject unsafe re-resolution before connecting.
- [x] Add durable local storage with deduplication, encrypted scheduled inputs, and configurable item retention.
- [ ] Consider ActiveMQ Classic only if refresh jobs need asynchronous delivery. Consider Kafka only if replay or multiple independent consumers become a real requirement.
- [x] Add an authenticated local REST API and OpenAPI contract after threat-reviewing the network fetch path.
