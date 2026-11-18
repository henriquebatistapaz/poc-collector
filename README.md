# Multi-Source Collector Mini

Java 27 / Maven collector for public RSS/Atom feeds and seven public REST JSON sources. It reads Google News search RSS, public WordPress feeds, Mastodon, Lemmy, DEV.to, Hacker News, Stack Exchange, GitHub, and TabNews. The returned items contain source, stable ID, plain-text title, safe link, and timestamps.

## Build and tests

```bash
mvn -B -ntp clean verify
```

## Run

```bash
java -jar target/poc-collector-0.1.0-SNAPSHOT.jar google-news "open data Brazil"
java -jar target/poc-collector-0.1.0-SNAPSHOT.jar mastodon java
java -jar target/poc-collector-0.1.0-SNAPSHOT.jar dev-to spring
java -jar target/poc-collector-0.1.0-SNAPSHOT.jar tabnews relevant
```

Supported REST source commands are `mastodon`, `lemmy`, `dev-to`, `hacker-news`, `stack-exchange`, `github`, and `tabnews`. DEV.to and TabNews accept `@username`; TabNews also accepts `relevant`, `new`, `old`, or a title keyword. REST requests use fixed provider hosts, a 10-second timeout, a 2 MiB body limit, gzip support, no redirects, and a fixed User-Agent. Inputs never select a host.

For a public WordPress tag feed:

```bash
java -jar target/poc-collector-0.1.0-SNAPSHOT.jar wordpress java
```

The CLI prints normalized JSON. RSS requests have bounded timeouts and redirects disabled. WordPress URLs must use HTTPS and resolve only to public IP addresses. Google News uses its observed search RSS URL shape; Google does not publish an official API contract for that endpoint, so it may change.

## REST API, schedules, and persistence

The standalone API stores normalized items and encrypted scheduled inputs in a local JSON data file. It runs as a single instance; set `COLLECTOR_DATA_FILE` to a persistent mounted volume when running in a container. Item retention defaults to 30 days and can be changed with `COLLECTOR_RETENTION_DAYS` (1–3650). Scheduled intervals are limited to 5 minutes–24 hours.

Set separate random values for the bearer API key and AES-GCM encryption key. The API key must be at least 32 bytes; the encryption key must be 32 bytes encoded as 64 hexadecimal characters.

```bash
export COLLECTOR_API_KEY="$(openssl rand -hex 32)"
export COLLECTOR_SECRET_KEY="$(openssl rand -hex 32)"
export COLLECTOR_DATA_FILE="$PWD/collector-data.json"
export COLLECTOR_RETENTION_DAYS=30
./scripts/run-api.sh
```

The API binds to `127.0.0.1:8090` by default. Keep that default for local use. For remote deployment, use a TLS-terminating reverse proxy. `GET /health` and `GET /openapi.json` are public; `/v1/*` requires `Authorization: Bearer $COLLECTOR_API_KEY`.

- `GET /v1/sources` — capability status for each enabled or unavailable source.
- `POST /v1/collections` — collect once with `{"source":"GITHUB","input":"spring boot"}`.
- `GET /v1/items?limit=100&source=GITHUB` — list stored items (limit 1–500).
- `GET /v1/feeds` and `POST /v1/feeds` — list or schedule a collection.
- `DELETE /v1/feeds/{id}` — remove a schedule.

Scheduled source inputs are encrypted at rest. Losing or changing `COLLECTOR_SECRET_KEY` makes existing schedules unreadable; restore the same key with the data file. Existing collected items and run history remain readable. Google Alerts collection and scheduling have been removed; any remaining legacy records are not executed.

## Source status

- **Google News:** search RSS with free-text keywords; endpoint is undocumented and behavior may change.
- **WordPress:** public WordPress.com tag RSS feeds.
- **Mastodon, Lemmy, DEV.to, Hacker News, Stack Exchange, GitHub, TabNews:** public JSON APIs without API keys, using fixed official hosts and conservative request limits.
- **Reddit:** canceled by the owner; collection support is removed while historical items remain readable.
- **Issuu:** unavailable for public discovery; no scraping or hidden endpoints are used.

The canonical source evidence and capability matrix are maintained in the private ai-init-internal project context. This repository contains a versioned snapshot of managed AI-init files.
