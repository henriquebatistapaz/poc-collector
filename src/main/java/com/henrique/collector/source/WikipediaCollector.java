package com.henrique.collector.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.henrique.collector.feed.FeedItem;
import java.net.URI;
import java.util.List;
import java.util.Map;

public final class WikipediaCollector implements FeedCollector {
    private static final URI ENDPOINT = URI.create("https://en.wikipedia.org/w/api.php");
    private final RestJsonClient client;

    public WikipediaCollector() { this(ENDPOINT); }
    public WikipediaCollector(URI endpoint) { this.client = new RestJsonClient(endpoint); }

    @Override public List<FeedItem> collect(String input) {
        String term = RestJsonSupport.requiredInput(input, 200, "Wikipedia query");
        URI uri = RestJsonSupport.appendQuery(client.baseUri(), Map.of(
                "action", "query", "list", "search", "format", "json", "srsearch", term));
        JsonNode root = client.get(uri, Map.of());
        JsonNode query = root.get("query");
        if (query == null || !query.isObject()) throw new SourceAccessException("REST source returned an unexpected JSON shape.");
        return RestJsonSupport.items(query, "search", CollectorSource.WIKIPEDIA, node -> {
            String id = RestJsonSupport.value(node, "pageid");
            String title = RestJsonSupport.cleanTitle(RestJsonSupport.value(node, "title"));
            URI link = id == null ? null : RestJsonSupport.safeUri("https://en.wikipedia.org/?curid=" + id);
            return RestJsonSupport.item(CollectorSource.WIKIPEDIA, id, title, link,
                    RestJsonSupport.instant(RestJsonSupport.value(node, "timestamp")));
        });
    }
}
