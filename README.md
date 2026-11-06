# Multi-Source Collector Mini

Private, specification-first mini project for collecting public links and metadata from six sources: Google News, Google Alerts, Reddit, Issuu, WordPress, and Mercado Livre.

The project is intentionally not implemented yet. The initial deliverable is the scope and feasibility specification in [SPEC.md](SPEC.md). It distinguishes sources with a documented public feed from sources that need a policy or access feasibility check before code is written.

## Privacy and access


"No API token" does not always mean "no account setup": a Google Alerts RSS URL, if available in the account, may grant access to the configured alert and must be treated as a secret.

## Project status

- Specification: drafted.
- Source adapters: not implemented.
- Storage, scheduling, API, and messaging: not implemented.
- Next step: approve the feasibility spike and first source slice before implementation.

See [TODO.md](TODO.md) for the staged work.
