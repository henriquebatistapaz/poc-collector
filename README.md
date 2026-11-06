# Multi-Source Collector Mini

Private Java 21 / Maven command-line collector for public RSS and Atom links. The initial implementation reads Google News search RSS, a user-provided Google Alerts RSS URL, and public WordPress RSS/Atom feeds. Every returned item contains only source, stable ID, title, link, and timestamps.

## Build and tests

```bash
mvn test
mvn package
```

## Run

```bash
java -jar target/multi-source-collector-mini-0.1.0-SNAPSHOT.jar google-news "open data Brazil"
```

For Google Alerts, create and configure an alert in the Google account first, then provide its RSS feed URL through the environment. The URL is a secret and must not be stored in shell history, Git, logs, or screenshots.

```bash
read -s GOOGLE_ALERTS_FEED_URL
export GOOGLE_ALERTS_FEED_URL
java -jar target/multi-source-collector-mini-0.1.0-SNAPSHOT.jar google-alerts
unset GOOGLE_ALERTS_FEED_URL
```

For a public WordPress feed:

```bash
export WORDPRESS_FEED_URL='https://wordpress.org/news/feed/'
java -jar target/multi-source-collector-mini-0.1.0-SNAPSHOT.jar wordpress
unset WORDPRESS_FEED_URL
```

The program prints a JSON array of normalized feed items to stdout. Google Alerts fetch/parse errors omit the secret URL. Feed requests have timeouts, a 1 MB response cap, and redirects disabled. WordPress URLs must use HTTPS and resolve only to public IP addresses.

## Source status

- **Google News:** implemented using its observed search RSS URL shape. Google does not provide an official API contract for this endpoint, so behavior may change.
- **Google Alerts:** implemented only for a user-supplied feed URL if the user's account exposes one. Current Google help describes email alerts but does not document RSS delivery; no account feed URL is included in this repository.
- **WordPress:** implemented using public RSS or Atom feeds supplied by the user.
- **Reddit:** disabled. External API access requires Reddit-provided access information and approval; this project does not scrape Reddit pages.
- **Issuu:** unavailable until an official permitted tokenless feed/API contract is verified.
- **Mercado Livre:** disabled for the tokenless scope; the official API requires OAuth access tokens, and marketplace pages are not scraped.

See [SPEC.md](SPEC.md) for the source evidence, access limits, and implementation scope. AI-Ready files and `/docs/` remain local and ignored.
