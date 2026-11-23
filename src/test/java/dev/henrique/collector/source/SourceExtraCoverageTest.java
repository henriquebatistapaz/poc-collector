package dev.henrique.collector.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.henrique.collector.CollectorApiServer;
import dev.henrique.collector.feed.FeedFetcher;
import dev.henrique.collector.feed.FeedParseException;
import dev.henrique.collector.feed.RssFeedParser;
import dev.henrique.collector.service.CollectorStateStore;
import dev.henrique.collector.service.SecretBox;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SourceExtraCoverageTest {

    @TempDir
    Path tempDir;

    static class StubHttpClient extends HttpClient {
        int status = 200;
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        Map<String, List<String>> hdrs = Map.of();
        IOException ioErr;
        InterruptedException intErr;

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException, InterruptedException {
            if (intErr != null) throw intErr;
            if (ioErr != null) throw ioErr;
            return new HttpResponse<>() {
                @Override public int statusCode() { return status; }
                @Override public HttpRequest request() { return request; }
                @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
                @Override public HttpHeaders headers() { return HttpHeaders.of(hdrs, (a, b) -> true); }
                @Override public T body() { return (T) new ByteArrayInputStream(body); }
                @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
                @Override public URI uri() { return request.uri(); }
                @Override public Version version() { return Version.HTTP_1_1; }
            };
        }

        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { return null; }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { return null; }
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
    }

    @Test
    void restJsonSupportAndClientCoverAllBranches() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // RestJsonSupport entity decoding, safeUri, cleanTitle, item, items, appendQuery
        assertEquals("", RestJsonSupport.text(null));
        assertNotNull(RestJsonSupport.text("&amp; &lt; &gt; &quot; &apos; &#39; &nbsp; &#65; &#x42; &#xd800; &#99999999; &#xZZ;"));
        assertNull(RestJsonSupport.value(null, "a"));
        assertNull(RestJsonSupport.value(mapper.readTree("{\"a\":null}"), "a"));
        assertNull(RestJsonSupport.first(mapper.readTree("{\"a\":\"   \"}"), "a", "b"));
        assertNull(RestJsonSupport.instant(null));
        assertNull(RestJsonSupport.instant("   "));
        assertNull(RestJsonSupport.instant("not-a-date"));
        assertNull(RestJsonSupport.safeUri(null, "   ", "https://e.com/" + "a".repeat(2005), "ftp://e.com", "https://u:p@e.com", "/nohost", "http://[::1"));
        assertNotNull(RestJsonSupport.safeUri("http://example.com"));
        assertNull(RestJsonSupport.cleanTitle("   "));
        assertEquals(1000, RestJsonSupport.cleanTitle("x".repeat(1200)).length());
        assertNull(RestJsonSupport.item(CollectorSource.GITHUB, null, "t", URI.create("https://e.com"), null));
        assertNull(RestJsonSupport.item(CollectorSource.GITHUB, "  ", "t", URI.create("https://e.com"), null));
        assertNull(RestJsonSupport.item(CollectorSource.GITHUB, "x".repeat(2005), "t", URI.create("https://e.com"), null));
        assertNull(RestJsonSupport.item(CollectorSource.GITHUB, "1", null, URI.create("https://e.com"), null));
        assertNull(RestJsonSupport.item(CollectorSource.GITHUB, "1", "t", null, null));
        assertThrows(SourceAccessException.class, () -> RestJsonSupport.items(mapper.readTree("{}"), "missing", CollectorSource.GITHUB, n -> null));
        assertEquals("https://e.com?a=1&b=2", RestJsonSupport.appendQuery(URI.create("https://e.com?a=1"), Map.of("b", "2")).toString());

        // RestJsonClient constructor & get branches
        StubHttpClient http = new StubHttpClient();
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(null, http, mapper));
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(URI.create("/nohost"), http, mapper));
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(URI.create("https://u:p@api.github.com"), http, mapper));
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(URI.create("https://api.github.com:8443"), http, mapper));
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(URI.create("http://localhost:0"), http, mapper));
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(URI.create("http://evil.com:8080"), http, mapper));
        assertNotNull(new RestJsonClient(URI.create("https://api.github.com:443"), http, mapper));
        assertNotNull(new RestJsonClient(URI.create("http://localhost:8080"), http, mapper));
        assertThrows(IllegalArgumentException.class, () -> new RestJsonClient(URI.create("http://[::1]:8080"), http, mapper));

        RestJsonClient client = new RestJsonClient(URI.create("http://127.0.0.1:8080"), http, mapper);
        http.status = 199;
        assertThrows(SourceAccessException.class, () -> client.get(URI.create("http://127.0.0.1:8080"), Map.of("X-Test", "1")));

        http.status = 200;
        http.body = "null".getBytes(StandardCharsets.UTF_8);
        assertThrows(SourceAccessException.class, () -> client.get(URI.create("http://127.0.0.1:8080"), Map.of()));

        http.body = "123".getBytes(StandardCharsets.UTF_8);
        assertThrows(SourceAccessException.class, () -> client.get(URI.create("http://127.0.0.1:8080"), Map.of()));

        http.body = "".getBytes(StandardCharsets.UTF_8);
        assertThrows(SourceAccessException.class, () -> client.get(URI.create("http://127.0.0.1:8080"), Map.of()));

        http.intErr = new InterruptedException("int");
        assertThrows(SourceAccessException.class, () -> client.get(URI.create("http://127.0.0.1:8080"), Map.of()));
        http.intErr = null;
        Thread.interrupted();
    }

    @Test
    void collectorsParserServiceAndStoreCoverRemainingBranches() throws Exception {
        FeedFetcher stubFetcher = uri -> "<rss version='2.0'><channel><item><title>T</title><link>https://e.com/1</link></item></channel></rss>".getBytes(StandardCharsets.UTF_8);
        RssFeedParser parser = new RssFeedParser();

        // GoogleNewsCollector branches
        GoogleNewsCollector gn = new GoogleNewsCollector(stubFetcher, parser);
        assertThrows(IllegalArgumentException.class, () -> new GoogleNewsCollector(stubFetcher, parser, null, "US", "US:en"));
        assertThrows(IllegalArgumentException.class, () -> new GoogleNewsCollector(stubFetcher, parser, "invalid!", "US", "US:en"));
        assertThrows(IllegalArgumentException.class, () -> gn.collect(null));
        assertThrows(IllegalArgumentException.class, () -> gn.collect("   "));
        assertThrows(IllegalArgumentException.class, () -> gn.collect("x".repeat(205)));
        assertThrows(IllegalArgumentException.class, () -> gn.feedUri(null));
        assertThrows(IllegalArgumentException.class, () -> gn.feedUri("   "));
        assertThrows(IllegalArgumentException.class, () -> gn.feedUri("x".repeat(205)));
        assertNotNull(gn.feedUri("java"));

        // CollectorService 10-arg constructor
        CollectorService svc10 = new CollectorService(new SourceRegistry(), gn, new WordPressFeedCollector(stubFetcher, parser),
                new MastodonCollector(), new LemmyCollector(), new DevToCollector(), new HackerNewsCollector(),
                new StackExchangeCollector(), new GitHubCollector(), new TabNewsCollector());
        assertNotNull(svc10);

        // RssFeedParser branches
        String atomAndRdf = """
                <RDF>
                  <item><title>   </title><link>https://e.com/1</link></item>
                  <item><title>NoLink</title></item>
                  <entry>
                    <title>ValidEntry</title>
                    <id>   </id>
                    <link rel="enclosure" href="https://e.com/enc"/>
                    <link href="https://e.com/alt"/>
                    <published>   </published>
                  </entry>
                  <entry>
                    <title>OffsetDateEntry</title>
                    <link rel="alternate" href="http://e.com/2"/>
                    <updated>2024-01-01T10:00:00+02:00</updated>
                  </entry>
                </RDF>
                """;
        assertEquals(2, parser.parse(atomAndRdf.getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS).size());
        assertThrows(FeedParseException.class, () -> parser.parse("<other><item><title>T</title><link>https://e.com</link></item></other>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS));
        assertThrows(FeedParseException.class, () -> parser.parse("<rss><item><title>T</title><link>ftp://e.com</link></item></rss>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS));
        assertThrows(FeedParseException.class, () -> parser.parse("<rss><item><title>T</title><link>https:///nohost</link></item></rss>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS));
        assertThrows(FeedParseException.class, () -> parser.parse("<rss><item><title>T</title><link>https://u:p@e.com</link></item></rss>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS));

        StringBuilder over100 = new StringBuilder("<rss>");
        for (int i = 0; i < 102; i++) {
            over100.append("<item><title>T</title><link>https://e.com/").append(i).append("</link></item>");
        }
        over100.append("</rss>");
        assertThrows(FeedParseException.class, () -> parser.parse(over100.toString().getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS));

        // TabNews, GitLab, GitHub, StackExchange, HackerNews, DevTo, Lobsters, Wikipedia edge cases via local HttpServer
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = srv.getAddress().getPort();
        URI base = URI.create("http://127.0.0.1:" + port);
        srv.createContext("/tabnews", ex -> reply(ex, "[{\"id\":\"1\",\"title\":\"Java News\",\"owner_username\":\"user_1\",\"slug\":\"post-1\"},{\"id\":\"2\",\"title\":\"T\",\"owner_username\":null,\"slug\":\"s\"},{\"id\":\"3\",\"title\":\"T\",\"owner_username\":\"u\",\"slug\":null},{\"id\":\"4\",\"title\":\"T\",\"owner_username\":\"bad!\",\"slug\":\"s\"},{\"id\":\"5\",\"title\":\"T\",\"owner_username\":\"u\",\"slug\":\"bad!\"}]"));
        srv.createContext("/gitlab", ex -> reply(ex, "[{\"id\":\"1\",\"name\":\"proj\",\"description\":\"desc\",\"web_url\":\"https://gitlab.com/p\"},{\"id\":\"2\",\"name\":\"proj2\",\"web_url\":\"https://gitlab.com/p2\"},{\"id\":\"3\",\"web_url\":\"https://gitlab.com/p3\"}]"));
        srv.createContext("/github", ex -> reply(ex, "{\"items\":[{\"id\":\"1\",\"full_name\":\"o/r\",\"html_url\":\"https://github.com/o/r\"},{\"id\":\"2\",\"html_url\":\"https://github.com/o/r2\"}]}"));
        srv.createContext("/stack", ex -> reply(ex, "{\"items\":[{\"question_id\":\"1\",\"title\":\"Q\",\"link\":\"https://stackoverflow.com/q/1\",\"creation_date\":\"not-a-long\"}]}"));
        srv.createContext("/hn", ex -> reply(ex, "{\"hits\":[{\"title\":\"NoId\",\"url\":\"https://e.com\"}]}"));
        srv.createContext("/lobsters/", ex -> reply(ex, "[]"));
        srv.createContext("/wiki", ex -> reply(ex, "{\"query\":{\"search\":[{\"pageid\":1,\"title\":\"   \"},{\"pageid\":2,\"title\":\"Java\",\"snippet\":\"<b>lang</b>\"},{\"pageid\":3,\"title\":\"OnlyTitle\"}]}}"));
        srv.start();
        try {
            TabNewsCollector tn = new TabNewsCollector(URI.create(base + "/tabnews"));
            assertThrows(IllegalArgumentException.class, () -> tn.collect("@bad!user"));
            assertEquals(1, tn.collect("@valid_user").size());
            assertEquals(1, tn.collect("relevant").size());
            assertEquals(1, tn.collect("new").size());
            assertEquals(1, tn.collect("old").size());

            GitLabCollector gl = new GitLabCollector(URI.create(base + "/gitlab"));
            assertEquals(2, gl.collect("java").size());

            GitHubCollector gh = new GitHubCollector(URI.create(base + "/github"));
            assertEquals(1, gh.collect("java").size());

            StackExchangeCollector se = new StackExchangeCollector(URI.create(base + "/stack"));
            assertEquals(1, se.collect("java").size());

            HackerNewsCollector hn = new HackerNewsCollector(URI.create(base + "/hn"));
            assertTrue(hn.collect("java").isEmpty());

            LobstersCollector lob = new LobstersCollector(URI.create(base + "/lobsters/"));
            assertTrue(lob.collect(null).isEmpty());
            assertTrue(lob.collect("   ").isEmpty());

            WikipediaCollector wk = new WikipediaCollector(URI.create(base + "/wiki"));
            assertEquals(2, wk.collect("java").size());
        } finally {
            srv.stop(0);
        }

        // CollectorStateStore invalid file & branches
        Path badFile = tempDir.resolve("bad.json");
        Files.writeString(badFile, "not valid json");
        assertThrows(IllegalStateException.class, () -> new CollectorStateStore(badFile));

        Path goodFile = tempDir.resolve("good.json");
        CollectorStateStore store = new CollectorStateStore(goodFile);
        var feed = store.createFeed(CollectorSource.GOOGLE_NEWS, "enc", 300, Instant.now().plusSeconds(300));
        assertTrue(store.dueFeeds(Instant.now()).isEmpty());
        store.completeFeed("non-existent-id", Instant.now(), "FAILED");

        // CollectorApiServer POST /v1/collections and POST /v1/feeds success
        SecretBox box = SecretBox.fromEnvironment("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        String apiKey = "k".repeat(32);
        try (CollectorApiServer api = new CollectorApiServer(new InetSocketAddress("127.0.0.1", 0), svc10, store, box, apiKey, 30)) {
            api.start();
            String apiBase = "http://127.0.0.1:" + api.address().getPort();
            HttpClient client = HttpClient.newHttpClient();
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(apiBase + "/v1/collections"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"GOOGLE_NEWS\",\"input\":\"java\"}"))
                    .header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(201, client.send(HttpRequest.newBuilder(URI.create(apiBase + "/v1/feeds"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"GOOGLE_NEWS\",\"input\":\"java\",\"intervalSeconds\":600}"))
                    .header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}
