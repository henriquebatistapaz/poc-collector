# AGENTS.md — Multi-Source Collector

Operational guide for Copilot, Claude Code, Codex, Cursor, and other coding agents.
Detailed project context: @README.md @TODO.md @docs/sdd/INDEX.md.
The canonical private project context, including SPEC.md, is maintained separately from this public repository. This repository contains a synced, versioned snapshot of managed AI-init files.

## Project summary

Standalone Java 27 / Maven command-line collector and local REST service. The runtime uses JDK HttpServer, Jackson, and JUnit 5; it does not use Spring Boot. Source adapters, RSS/Atom parsing, deduplication, local JSON persistence, encrypted schedules, retention, and an authenticated REST/OpenAPI API are implemented.

Enabled sources: Google News search RSS, public WordPress.com tag feeds, and public REST APIs for Mastodon, Lemmy, DEV.to, Hacker News, Stack Exchange, GitHub, TabNews, Wikipedia, GitLab and Lobsters. Google Alerts collection and scheduling were removed; preserve historical items and runs. Reddit support was canceled by the owner on 2026-11-11 and its adapter was removed. Issuu public discovery remains unavailable; do not use scraping or undocumented endpoints.

## Build, test, and run

Run the complete verification suite before shipping changes:

```bash
mvn -B -ntp clean verify
```

Run the CLI from the packaged JAR:

```bash
java -jar target/poc-collector-0.1.0-SNAPSHOT.jar google-news "open data Brazil"
```

Start the local API:

```bash
./scripts/run-api.sh
```

The API binds to `127.0.0.1:8090` by default. `/health` and `/openapi.json` are public; `/v1/*` requires a bearer API key. Keep the API loopback-bound for local use. For remote use, put it behind a TLS-terminating reverse proxy before binding it to a network interface.

## Runtime and data safety

- Keep source adapters small and registered through `SourceRegistry`.
- WordPress requests must use HTTPS and public DNS addresses; preserve the DNS rebinding checks and their tests.
- Treat API keys and encryption keys as secrets. Never store them in Git, shell history, logs, or screenshots. REST source hosts must remain fixed; preserve timeout, response-size, gzip, and redirect restrictions.
- Preserve read access to previously collected Reddit items in existing data files; never add Reddit collection back without a new approved spec.
- The local JSON store supports one process. Mount a persistent volume for container use; do not share the same data file across processes.
- Scheduled feed inputs are encrypted with AES-GCM. Preserve the same `COLLECTOR_SECRET_KEY` with its data file; changing it makes stored schedules unreadable.
- Keep retention configurable and preserve deletion/removal behavior for collected content.

## Project conventions

Use Java 27, Maven, the JDK HTTP server, and JUnit 5. The project is a single Maven module. Avoid new dependencies unless the SDD justifies them. Follow the existing package boundaries under `com.henrique.collector`.

AI-init managed files are mirrored from the private canonical project context. Update that canonical context first, then sync the mirror and manifest hashes. Do not add private credentials or private-only context to public project documentation.

## Spec-driven workflow

Create non-trivial work under `docs/sdd/specs/<YYYYMMDD>-<slug>/spec.md`, add its test plan, and register it in `docs/sdd/INDEX.md`. Follow `docs/sdd/TEMPLATE.sdd.md` and `docs/sdd/constitution.md`.

Workflow: QA review and human approval → test plan → implementation → validation against acceptance criteria → PR → CI and human review → merge. Only a human sets a spec to `Approved`. `/next` may perform the next available step and stops at human gates.

## Do not

- Commit secrets, credentials, real feed URLs, or connection strings.
- Enable a source unless its access route and use are documented and permitted.
- Bypass DNS/public-address checks, request timeouts, response-size caps, or redirect restrictions.
- Finish work with failing tests or a broken build.
- Leave this guide or `docs/wiki/` inaccurate after a change.
