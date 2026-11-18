package com.henrique.collector.source;

import com.henrique.collector.feed.FeedItem;
import java.net.URI;
import java.util.List;
import java.util.Map;

public final class GitLabCollector implements FeedCollector {
    private static final URI ENDPOINT = URI.create("https://gitlab.com/api/v4/projects");
    private final RestJsonClient client;

    public GitLabCollector() { this(ENDPOINT); }
    public GitLabCollector(URI endpoint) { this.client = new RestJsonClient(endpoint); }

    @Override public List<FeedItem> collect(String input) {
        String term = RestJsonSupport.requiredInput(input, 200, "GitLab query");
        URI uri = RestJsonSupport.appendQuery(client.baseUri(), Map.of("search", term, "per_page", "30"));
        return RestJsonSupport.items(client.get(uri, Map.of()), null, CollectorSource.GITLAB, node -> {
            String id = RestJsonSupport.value(node, "id");
            String name = RestJsonSupport.cleanTitle(RestJsonSupport.first(node, "name_with_namespace", "name"));
            String description = RestJsonSupport.cleanTitle(RestJsonSupport.value(node, "description"));
            String title = name == null ? null : RestJsonSupport.cleanTitle(description == null ? name : name + " — " + description);
            URI link = RestJsonSupport.safeUri(RestJsonSupport.value(node, "web_url"));
            return RestJsonSupport.item(CollectorSource.GITLAB, id, title, link,
                    RestJsonSupport.instant(RestJsonSupport.value(node, "last_activity_at")));
        });
    }
}
