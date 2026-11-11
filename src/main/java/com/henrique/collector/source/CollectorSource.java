package com.henrique.collector.source;

public enum CollectorSource {
    GOOGLE_NEWS,
    GOOGLE_ALERTS,
    REDDIT, // Retained only to read historical local data; source collection is cancelled.
    ISSUU,
    WORDPRESS
}
