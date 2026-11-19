# Project Wiki — Multi-Source Collector

The collector has a Java 27 CLI and a local authenticated REST service. It supports Google News search RSS, public WordPress RSS/Atom, and public REST APIs for Mastodon, Lemmy, DEV.to, Hacker News, Stack Exchange, GitHub, TabNews, Wikipedia, GitLab, and Lobsters. Google Alerts collection and scheduling were removed; historical items and runs remain available. Reddit support was canceled by the owner and its adapter was removed. Issuu public discovery is unavailable, so that source remains disabled.

The private capability matrix and source evidence live in the canonical project context. This repository's README documents the public setup and runtime behavior.

- [Architecture overview](architecture-overview.md)
