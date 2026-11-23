package dev.henrique.collector.source;

import dev.henrique.collector.feed.FeedItem;
import java.net.URI;
import java.util.List;

public final class LobstersCollector implements FeedCollector {
    private static final URI ENDPOINT = URI.create("https://lobste.rs");
    private final RestJsonClient client;

    public LobstersCollector() { this(ENDPOINT); }
    public LobstersCollector(URI endpoint) { this.client = new RestJsonClient(endpoint); }

    @Override public List<FeedItem> collect(String input) {
        String value = input == null || input.isBlank() ? "hottest"
                : RestJsonSupport.requiredInput(input, 100, "Lobsters tag").toLowerCase(java.util.Locale.ROOT);
        String path;
        if (value.equals("hottest")) path = "/hottest.json";
        else if (value.matches("[a-z0-9]{1,100}")) path = "/t/" + value + ".json";
        else throw new IllegalArgumentException("Invalid Lobsters tag.");
        String base = client.baseUri().toASCIIString();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return RestJsonSupport.items(client.get(URI.create(base + path), java.util.Map.of()), null,
                CollectorSource.LOBSTERS, node -> {
                    String id = RestJsonSupport.value(node, "short_id");
                    String title = RestJsonSupport.cleanTitle(RestJsonSupport.value(node, "title"));
                    URI link = RestJsonSupport.safeUri(RestJsonSupport.first(node, "url", "comments_url"));
                    return RestJsonSupport.item(CollectorSource.LOBSTERS, id, title, link,
                            RestJsonSupport.instant(RestJsonSupport.value(node, "created_at")));
                });
    }
}
