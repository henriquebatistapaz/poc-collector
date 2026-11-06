package com.henrique.multisourcecollector.source;

import com.henrique.multisourcecollector.feed.FeedItem;
import java.util.List;

@FunctionalInterface
public interface FeedCollector {
    List<FeedItem> collect(String input);
}
