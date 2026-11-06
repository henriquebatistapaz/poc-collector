package com.henrique.multisourcecollector.feed;

public class FeedFetchException extends RuntimeException {
    public FeedFetchException(String message) { super(message); }
    public FeedFetchException(String message, Throwable cause) { super(message, cause); }
}
