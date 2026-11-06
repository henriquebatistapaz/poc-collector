package com.henrique.multisourcecollector;

import com.henrique.multisourcecollector.feed.FeedItem;
import com.henrique.multisourcecollector.feed.JdkHttpFeedFetcher;
import com.henrique.multisourcecollector.feed.RssFeedParser;
import com.henrique.multisourcecollector.source.*;
import java.util.List;

public final class CollectorCli {
    private CollectorCli() { }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar multi-source-collector-mini.jar <google-news|google-alerts|wordpress> [query]");
            System.exit(2);
        }
        var fetcher = new JdkHttpFeedFetcher();
        var parser = new RssFeedParser();
        var service = new CollectorService(new SourceRegistry(),
                new GoogleNewsCollector(fetcher, parser),
                new GoogleAlertsCollector(fetcher, parser, new GoogleAlertsUrlPolicy()),
                new WordPressFeedCollector(fetcher, parser, new PublicHttpsFeedUrlPolicy()));
        try {
            CollectorSource source = switch (args[0].toLowerCase()) {
                case "google-news" -> CollectorSource.GOOGLE_NEWS;
                case "google-alerts" -> CollectorSource.GOOGLE_ALERTS;
                case "wordpress" -> CollectorSource.WORDPRESS;
                case "reddit" -> CollectorSource.REDDIT;
                case "issuu" -> CollectorSource.ISSUU;
                case "mercado-livre" -> CollectorSource.MERCADO_LIVRE;
                default -> throw new IllegalArgumentException("Unknown source. Use google-news, google-alerts, wordpress, reddit, issuu, or mercado-livre.");
            };
            String input = switch (source) {
                case GOOGLE_NEWS -> String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                case GOOGLE_ALERTS -> requiredEnvironment("GOOGLE_ALERTS_FEED_URL");
                case WORDPRESS -> requiredEnvironment("WORDPRESS_FEED_URL");
                default -> "";
            };
            List<FeedItem> items = service.collect(source, input);
            System.out.println(FeedItemJson.encode(items));
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " environment variable is required.");
        return value;
    }
}
