package dev.henrique.collector;

import dev.henrique.collector.feed.FeedFetchException;
import dev.henrique.collector.feed.FeedItem;
import dev.henrique.collector.feed.JdkHttpFeedFetcher;
import dev.henrique.collector.feed.RssFeedParser;
import dev.henrique.collector.service.CollectorScheduler;
import dev.henrique.collector.service.CollectorStateStore;
import dev.henrique.collector.service.ScheduledFeed;
import dev.henrique.collector.service.SecretBox;
import dev.henrique.collector.source.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CollectorExtraCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void jdkHttpFeedFetcherAndPolicyCoverAllBranches() throws Exception {
        JdkHttpFeedFetcher defaultFetcher = new JdkHttpFeedFetcher();
        assertNotNull(new JdkHttpFeedFetcher());
        assertThrows(FeedFetchException.class, () -> new JdkHttpFeedFetcher().fetch(URI.create("http://example.com/rss")));
        assertThrows(FeedFetchException.class, () -> new JdkHttpFeedFetcher().fetch(URI.create("https:///nohost")));
        assertThrows(FeedFetchException.class, () -> new JdkHttpFeedFetcher().fetch(URI.create("https://example.com:8443/rss")));
        JdkHttpFeedFetcher port443Unresolvable = new JdkHttpFeedFetcher(h -> new InetAddress[0]);
        assertThrows(FeedFetchException.class, () -> port443Unresolvable.fetch(URI.create("https://example.com:443/rss")));

        // JdkHttpFeedFetcher fetch(URI) with resolved public IP that fails socket connect -> covers for-loop and lastError!=null branch
        JdkHttpFeedFetcher failingConnect = new JdkHttpFeedFetcher(h -> new InetAddress[]{InetAddress.getByAddress(h, new byte[]{127, 0, 0, 1})});
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"))) {
            int p = ss.getLocalPort();
            Thread acceptor = new Thread(() -> {
                try (java.net.Socket s = ss.accept()) {
                    s.close();
                } catch (Exception ignored) {}
            });
            acceptor.setDaemon(true);
            acceptor.start();
            Method fetchAddr = JdkHttpFeedFetcher.class.getDeclaredMethod("fetch", URI.class, InetAddress.class);
            fetchAddr.setAccessible(true);
            assertThrows(InvocationTargetException.class,
                    () -> fetchAddr.invoke(failingConnect, URI.create("https://example.com:" + p + "/"), InetAddress.getByName("127.0.0.1")));
        }

        // RssFeedParser offset date, RFC1123 date, invalid date, blank date, empty guid, non-alternate atom link
        RssFeedParser rfp = new RssFeedParser();
        String atomXml = "<feed><entry><title>A1</title><id>   </id><link href='https://e.com/rel' rel='self'/><link href='https://e.com/a1' rel='alternate'/><published>2026-01-01T10:00:00+02:00</published></entry>"
                + "<entry><title>A2</title><link href='https://e.com/a2'/><updated>   </updated></entry>"
                + "<entry><title>   </title><link href='https://e.com/skip'/></entry>"
                + "<entry><title>NoLink</title></entry></feed>";
        assertEquals(2, rfp.parse(atomXml.getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS).size());

        JdkHttpFeedFetcher unresolvable = new JdkHttpFeedFetcher(h -> new InetAddress[0]);
        assertThrows(FeedFetchException.class, () -> unresolvable.fetch(URI.create("https://example.com/rss")));

        // Reflection on private static HTTP parsing methods in JdkHttpFeedFetcher
        Method readHeaders = JdkHttpFeedFetcher.class.getDeclaredMethod("readHeaders", InputStream.class);
        readHeaders.setAccessible(true);
        Method readLengthBody = JdkHttpFeedFetcher.class.getDeclaredMethod("readLengthBody", InputStream.class, String.class);
        readLengthBody.setAccessible(true);
        Method readChunkedBody = JdkHttpFeedFetcher.class.getDeclaredMethod("readChunkedBody", InputStream.class);
        readChunkedBody.setAccessible(true);
        Method readUntilEof = JdkHttpFeedFetcher.class.getDeclaredMethod("readUntilEof", InputStream.class);
        readUntilEof.setAccessible(true);
        Method readLine = JdkHttpFeedFetcher.class.getDeclaredMethod("readLine", InputStream.class, int.class);
        readLine.setAccessible(true);
        Method isIp = JdkHttpFeedFetcher.class.getDeclaredMethod("isIpAddress", String.class);
        isIp.setAccessible(true);

        assertTrue((Boolean) isIp.invoke(null, "8.8.8.8"));
        assertTrue((Boolean) isIp.invoke(null, "2606:4700::1"));
        assertFalse((Boolean) isIp.invoke(null, "example.com"));

        // readHeaders valid and invalid cases
        Method buildRequest = JdkHttpFeedFetcher.class.getDeclaredMethod("buildRequest", URI.class, int.class);
        buildRequest.setAccessible(true);
        assertTrue(((String) buildRequest.invoke(null, URI.create("https://e.com"), 443)).contains("GET / HTTP/1.1\r\nHost: e.com\r\n"));
        assertTrue(((String) buildRequest.invoke(null, URI.create("https://e.com:8443/rss?q=1"), 8443)).contains("GET /rss?q=1 HTTP/1.1\r\nHost: e.com:8443\r\n"));
        assertTrue(((String) buildRequest.invoke(null, new URI("https", "e.com", null, null), 443)).contains("GET / HTTP/1.1"));

        // JdkHttpFeedFetcher.fetch(URI) with resolved public IP that fails to connect (e.g. 203.0.114.1 on closed port via reflection or unreachable public IP with short timeout)
        // PublicHttpsFeedUrlPolicy IPv4 & IPv6 additional branches
        for (byte[] pubIp : new byte[][]{
                {(byte) 192, 0, 1, 1}, {(byte) 192, 1, 0, 1}, {(byte) 192, 88, 98, 1}, {(byte) 192, 89, 99, 1},
                {(byte) 198, 17, 0, 1}, {(byte) 198, 50, 0, 1}, {(byte) 203, 1, 113, 1}, {(byte) 203, 0, 112, 1},
                {0x20, 0x01, 0x02, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0d, (byte) 0xb7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0e, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}
        }) {
            assertTrue(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", pubIp)));
        }
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{100, 65, 0, 1})));
        // RssFeedParser RFC1123 date, invalid date, non-alternate atom link with another attribute
        assertEquals(1, rfp.parse("<rss version='2.0'><channel><item><title>T</title><link>https://e.com/1</link><pubDate>Tue, 03 Jun 2008 11:05:30 GMT</pubDate></item></channel></rss>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS).size());
        assertEquals(1, rfp.parse("<rss version='2.0'><channel><item><title>T</title><link>https://e.com/1</link><pubDate>not-a-date</pubDate></item></channel></rss>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS).size());
        assertEquals(1, rfp.parse("<feed><entry><title>T</title><link type='text/html' href='https://e.com/1'/></entry></feed>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS).size());

        // CollectorService with UNAVAILABLE & unimplemented collector
        SourceRegistry customReg = new SourceRegistry();
        CollectorService svcWithNull = new CollectorService(customReg, null, null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(SourceAccessException.class, () -> svcWithNull.collect(CollectorSource.GOOGLE_NEWS, "java"));

        // RestJsonSupport safeUri with userInfo & http scheme
        Method safeUri2 = Class.forName("dev.henrique.collector.source.RestJsonSupport").getDeclaredMethod("safeUri", String[].class);
        safeUri2.setAccessible(true);
        assertNull(safeUri2.invoke(null, (Object) new String[]{"https://u:p@e.com/1"}));
        assertNotNull(safeUri2.invoke(null, (Object) new String[]{"http://e.com/1"}));
        assertNotNull(new DevToCollector(URI.create("http://localhost:12345/api")));
        assertThrows(IllegalArgumentException.class, () -> new DevToCollector(URI.create("http://[::1]:12345/api")));

        // DevToCollector without @ prefix
        assertThrows(IllegalArgumentException.class, () -> new DevToCollector().collect("bad tag!"));
        assertThrows(IllegalArgumentException.class, () -> new DevToCollector().collect("@bad user!"));
        assertTrue(rfp.parse("<feed><entry><title>T</title><link rel='self' href='https://e.com/1'/></entry></feed>".getBytes(StandardCharsets.UTF_8), CollectorSource.WORDPRESS).isEmpty());

        // HttpHeaders.first with empty list
        java.lang.reflect.Constructor<?> hhC = Class.forName("dev.henrique.collector.feed.JdkHttpFeedFetcher$HttpHeaders").getDeclaredConstructor(int.class, Map.class);
        hhC.setAccessible(true);
        Object hhEmpty = hhC.newInstance(200, Map.of("k", List.of()));
        Method hhFirst = hhEmpty.getClass().getDeclaredMethod("first", String.class);
        hhFirst.setAccessible(true);
        assertNull(hhFirst.invoke(hhEmpty, "k"));

        // readLine with \r not followed by \n
        assertEquals("a\rb", readLine.invoke(null, in("a\rb\r\n"), 100));

        Method readResponseBody = JdkHttpFeedFetcher.class.getDeclaredMethod("readResponseBody", InputStream.class);
        readResponseBody.setAccessible(true);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
                (byte[]) readResponseBody.invoke(null, in("HTTP/1.1 200 OK\r\nContent-Encoding: identity\r\nContent-Length: 5\r\n\r\nhello")));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
                (byte[]) readResponseBody.invoke(null, in("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n0\r\n\r\n")));
        assertArrayEquals("eof".getBytes(StandardCharsets.UTF_8),
                (byte[]) readResponseBody.invoke(null, in("HTTP/1.1 200 OK\r\n\r\neof")));
        assertThrows(InvocationTargetException.class, () -> readResponseBody.invoke(null, in("HTTP/1.1 100 Continue\r\n\r\n")));
        assertThrows(InvocationTargetException.class, () -> readResponseBody.invoke(null, in("HTTP/1.1 404 Not Found\r\n\r\n")));
        assertThrows(InvocationTargetException.class, () -> readResponseBody.invoke(null, in("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\n\r\n")));
        assertThrows(InvocationTargetException.class, () -> readResponseBody.invoke(null, in("HTTP/1.1 200 OK\r\nTransfer-Encoding: gzip\r\n\r\n")));

        Object hdrs = readHeaders.invoke(null, in("HTTP/1.1 200 OK\r\nContent-Type: text/xml\r\nX-Custom: a\r\nX-Custom: b\r\n\r\n"));
        Method firstMethod = hdrs.getClass().getDeclaredMethod("first", String.class);
        firstMethod.setAccessible(true);
        assertEquals("text/xml", firstMethod.invoke(hdrs, "content-type"));
        assertNull(firstMethod.invoke(hdrs, "missing"));

        String[] badHeaders = new String[]{
                "INVALID\r\n\r\n",
                "FTP/1.1 200 OK\r\n\r\n",
                "HTTP/1.1 abc OK\r\n\r\n",
                "HTTP/1.1 200 OK\r\n folded: val\r\n\r\n",
                "HTTP/1.1 200 OK\r\n\tfolded: val\r\n\r\n",
                "HTTP/1.1 200 OK\r\nNoColon\r\n\r\n",
                "HTTP/1.1 200 OK\r\n:EmptyName\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Length: 5\r\nContent-Length: 5\r\n\r\n",
                "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\nTransfer-Encoding: chunked\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Encoding: identity\r\nContent-Encoding: identity\r\n\r\n",
                "HTTP/1.1 200 OK\r\nX-Big: " + "a".repeat(17_000) + "\r\n\r\n"
        };
        for (String bh : badHeaders) {
            assertThrows(InvocationTargetException.class, () -> readHeaders.invoke(null, in(bh)));
        }

        // readLengthBody cases
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) readLengthBody.invoke(null, in("hello"), "5"));
        assertThrows(InvocationTargetException.class, () -> readLengthBody.invoke(null, in("hello"), "abc"));
        assertThrows(InvocationTargetException.class, () -> readLengthBody.invoke(null, in("hello"), "-1"));
        assertThrows(InvocationTargetException.class, () -> readLengthBody.invoke(null, in("hello"), "2000000"));
        assertThrows(InvocationTargetException.class, () -> readLengthBody.invoke(null, in("hi"), "5"));

        // readChunkedBody cases
        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8),
                (byte[]) readChunkedBody.invoke(null, in("5;ext=1\r\nhello\r\n6\r\n world\r\n0\r\nX-Trailer: 1\r\n\r\n")));
        assertThrows(InvocationTargetException.class, () -> readChunkedBody.invoke(null, in("zz\r\n")));
        assertThrows(InvocationTargetException.class, () -> readChunkedBody.invoke(null, in("100000\r\n")));
        assertThrows(InvocationTargetException.class, () -> readChunkedBody.invoke(null, in("5\r\nhi\r\n")));
        assertThrows(InvocationTargetException.class, () -> readChunkedBody.invoke(null, in("2\r\nhiXX")));
        assertThrows(InvocationTargetException.class, () -> readChunkedBody.invoke(null, in("2\r\nhi\rX")));

        // readUntilEof & readLine cases
        assertArrayEquals("eof".getBytes(StandardCharsets.UTF_8), (byte[]) readUntilEof.invoke(null, in("eof")));
        assertThrows(InvocationTargetException.class, () -> readUntilEof.invoke(null, new ByteArrayInputStream(new byte[1_000_005])));
        assertThrows(InvocationTargetException.class, () -> readLine.invoke(null, in("no-crlf"), 100));
        assertThrows(InvocationTargetException.class, () -> readLine.invoke(null, in("toolong\r\n"), 3));

        // PublicHttpsFeedUrlPolicy branches
        PublicHttpsFeedUrlPolicy pol = new PublicHttpsFeedUrlPolicy(h -> new InetAddress[]{InetAddress.getByAddress("h", new byte[]{8, 8, 8, 8})});
        assertThrows(IllegalArgumentException.class, () -> pol.validate(null));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("   "));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://e.com/" + "a".repeat(2050)));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("http://[::1"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("http://e.com/rss"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https:///nohost"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://u:p@e.com/rss"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://e.com/rss#frag"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://e.com:8443/rss"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://localhost/rss"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://a.localhost/rss"));
        assertThrows(IllegalArgumentException.class, () -> pol.validate("https://a.local/rss"));
        assertNotNull(pol.validate("https://e.com:443/rss"));

        byte[][] badIps = new byte[][]{
                {0, 0, 0, 0}, {0, 1, 1, 1}, {10, 0, 0, 1}, {127, 0, 0, 1}, {(byte) 224, 0, 0, 1},
                {100, 64, 0, 1}, {100, 127, 0, 1}, {(byte) 169, (byte) 254, 1, 1},
                {(byte) 172, 16, 0, 1}, {(byte) 172, 31, 0, 1},
                {(byte) 192, 0, 0, 1}, {(byte) 192, 0, 2, 1}, {(byte) 192, (byte) 168, 1, 1}, {(byte) 192, 88, 99, 1},
                {(byte) 198, 18, 0, 1}, {(byte) 198, 19, 0, 1}, {(byte) 198, 51, 0, 1},
                {(byte) 203, 0, 113, 1},
                {(byte) 0xfc, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x02, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x3f, (byte) 0xff, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}
        };
        for (byte[] ip : badIps) {
            assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", ip)));
        }

        byte[][] goodIps = new byte[][]{
                {8, 8, 8, 8}, {100, 63, 0, 1}, {100, (byte) 128, 0, 1}, {(byte) 169, 1, 1, 1},
                {(byte) 172, 15, 0, 1}, {(byte) 172, 32, 0, 1},
                {(byte) 192, 1, 1, 1}, {(byte) 192, 0, 1, 1}, {(byte) 192, 88, 1, 1},
                {(byte) 198, 20, 0, 1}, {(byte) 203, 1, 1, 1}, {(byte) 203, 0, 1, 1},
                {0x26, 0x06, 0x47, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0e, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x01, 0x0d, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x3f, (byte) 0xfe, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x3f, (byte) 0xff, 0x10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {0x20, 0x03, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {(byte) 192, 0, 3, 1}, {(byte) 198, 52, 0, 1}, {(byte) 203, 0, 114, 1}
        };
        for (byte[] ip : goodIps) {
            assertTrue(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", ip)));
        }
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{0x00, 0x64, (byte) 0xff, (byte) 0x9b, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{0x00, 0x64, (byte) 0xff, (byte) 0x9a, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{0x00, 0x64, (byte) 0xfe, (byte) 0x9b, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
        assertFalse(PublicHttpsFeedUrlPolicy.isPublic(InetAddress.getByAddress("h", new byte[]{0x00, 0x65, (byte) 0xff, (byte) 0x9b, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
    }

    @Test
    void cliJsonApiServerStoreAndSchedulerCoverAllBranches() throws Exception {
        
        // startFromEnvironment & parseLong & JdkHttpFeedFetcher local TLS socket tests
        assertThrows(IllegalArgumentException.class, () -> CollectorApiServer.startFromEnvironment(Map.of(
                "COLLECTOR_SECRET_KEY", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "COLLECTOR_API_KEY", "a".repeat(32),
                "COLLECTOR_PORT", "not-a-number"
        )));
        try (AutoCloseable handle = CollectorApiServer.startFromEnvironment(Map.of(
                "COLLECTOR_SECRET_KEY", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "COLLECTOR_API_KEY", "a".repeat(32),
                "COLLECTOR_PORT", "0",
                "COLLECTOR_DATA_FILE", tempDir.resolve("env-data.json").toString()
        ))) {
            assertNotNull(handle);
        }

        // JdkHttpFeedFetcher fetch(URI) loop with resolved public IP that fails to connect on closed localhost port via fetch(URI, InetAddress)
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"))) {
            int p = ss.getLocalPort();
            Thread acceptor = new Thread(() -> {
                try (java.net.Socket s = ss.accept()) {
                    s.getOutputStream().write(new byte[]{1, 2, 3});
                } catch (Exception ignored) {}
            });
            acceptor.setDaemon(true);
            acceptor.start();
            Method fetchAddr = JdkHttpFeedFetcher.class.getDeclaredMethod("fetch", URI.class, InetAddress.class);
            fetchAddr.setAccessible(true);
            assertThrows(InvocationTargetException.class,
                    () -> fetchAddr.invoke(new JdkHttpFeedFetcher(), URI.create("https://127.0.0.1:" + p + "/feed?q=1"), InetAddress.getByName("127.0.0.1")));
        }
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"))) {
            int p = ss.getLocalPort();
            Thread acceptor = new Thread(() -> {
                try (java.net.Socket s = ss.accept()) {
                    s.getOutputStream().write(new byte[]{1, 2, 3});
                } catch (Exception ignored) {}
            });
            acceptor.setDaemon(true);
            acceptor.start();
            Method fetchAddr = JdkHttpFeedFetcher.class.getDeclaredMethod("fetch", URI.class, InetAddress.class);
            fetchAddr.setAccessible(true);
            assertThrows(InvocationTargetException.class,
                    () -> fetchAddr.invoke(new JdkHttpFeedFetcher(), URI.create("https://example.com:" + p), InetAddress.getByName("127.0.0.1")));
        }

        // FeedItemJson control characters & null publishedAt
        FeedItem i1 = new FeedItem(CollectorSource.WORDPRESS, "id\"\\\b\f\n\r\t\u0007", "Title", URI.create("https://e.com/1"), null, Instant.now());
        FeedItem i2 = new FeedItem(CollectorSource.WORDPRESS, "id2", "Title2", URI.create("https://e.com/2"), Instant.now(), Instant.now());
        String encoded = FeedItemJson.encode(List.of(i1, i2));
        assertTrue(encoded.contains("\\u0007"));
        assertTrue(encoded.contains("\"publishedAt\":null"));

        // CollectorCli branches
        var stubFetcher = new dev.henrique.collector.feed.FeedFetcher() {
            @Override public byte[] fetch(URI uri) {
                return "<rss version='2.0'><channel><item><title>T</title><link>https://e.com/1</link><guid>g1</guid></item></channel></rss>".getBytes(StandardCharsets.UTF_8);
            }
        };
        var parser = new RssFeedParser();
        var svc = new CollectorService(new SourceRegistry(), new GoogleNewsCollector(stubFetcher, parser), new WordPressFeedCollector(stubFetcher, parser));
        assertEquals(2, CollectorCli.run(new String[0], svc));
        assertEquals(1, CollectorCli.run(new String[]{"unknown-src"}, svc));
        assertEquals(1, CollectorCli.run(new String[]{"issuu"}, svc));
        assertEquals(0, CollectorCli.run(new String[]{"google-news", "java"}, svc));
        for (String s : new String[]{"wordpress", "mastodon", "lemmy", "dev-to", "hacker-news", "stack-exchange", "github", "tabnews"}) {
            CollectorCli.run(new String[]{s, ""}, svc);
        }

        // SecretBox branches
        assertThrows(IllegalArgumentException.class, () -> new SecretBox(null));
        assertThrows(IllegalArgumentException.class, () -> new SecretBox(new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> SecretBox.fromEnvironment(null));
        assertThrows(IllegalArgumentException.class, () -> SecretBox.fromEnvironment("not-hex"));
        SecretBox box = SecretBox.fromEnvironment("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertThrows(IllegalStateException.class, () -> box.decrypt("dG9vc2hvcnQ=")); // <= 12 bytes

        // CollectorStateStore & CollectorScheduler
        Path stateFile = tempDir.resolve("sub/state.json");
        CollectorStateStore store = new CollectorStateStore(stateFile);
        store.addItems(List.of(i1, i2));
        ScheduledFeed f1 = store.createFeed(CollectorSource.GOOGLE_NEWS, box.encrypt("java"), 300, Instant.now().minusSeconds(10));
        ScheduledFeed f2 = store.createFeed(CollectorSource.WORDPRESS, "invalid-cipher", 300, Instant.now().minusSeconds(10));

        // Reload store from disk (covers reading items with null and non-null publishedAt and feeds)
        CollectorStateStore storeReloaded = new CollectorStateStore(stateFile);
        assertEquals(2, storeReloaded.items(null, 10).size());
        assertEquals(0, storeReloaded.items(CollectorSource.MASTODON, 10).size());
        assertEquals(2, storeReloaded.feeds().size());

        // WikipediaCollector, DevToCollector, RestJsonSupport branches
        Method reqIn = Class.forName("dev.henrique.collector.source.RestJsonSupport").getDeclaredMethod("requiredInput", String.class, int.class, String.class);
        reqIn.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> reqIn.invoke(null, null, 10, "s"));
        assertThrows(InvocationTargetException.class, () -> reqIn.invoke(null, "a".repeat(20), 10, "s"));
        assertThrows(InvocationTargetException.class, () -> reqIn.invoke(null, "a\u0007b", 10, "s"));

        Method safeUri = Class.forName("dev.henrique.collector.source.RestJsonSupport").getDeclaredMethod("safeUri", String[].class);
        safeUri.setAccessible(true);
        assertNull(safeUri.invoke(null, (Object) new String[]{"https:///nohost"}));

        Method itemsM = Class.forName("dev.henrique.collector.source.RestJsonSupport").getDeclaredMethod("items",
                com.fasterxml.jackson.databind.JsonNode.class, String.class, CollectorSource.class,
                Class.forName("dev.henrique.collector.source.RestJsonSupport$ItemMapper"));
        itemsM.setAccessible(true);
        var om = new com.fasterxml.jackson.databind.ObjectMapper();
        assertThrows(InvocationTargetException.class, () -> itemsM.invoke(null, om.readTree("[1]"), "field", CollectorSource.DEV_TO, null));
        assertThrows(InvocationTargetException.class, () -> itemsM.invoke(null, om.readTree("{\"field\":123}"), "field", CollectorSource.DEV_TO, null));

        try (CollectorScheduler scheduler = new CollectorScheduler(svc, storeReloaded, box, 30)) {
            scheduler.start();
            Method tickSafely = CollectorScheduler.class.getDeclaredMethod("tickSafely");
            tickSafely.setAccessible(true);
            tickSafely.invoke(scheduler);
        }

        // CollectorApiServer validation & endpoints
        String apiKey = "a".repeat(32);
        assertThrows(IllegalArgumentException.class, () -> new CollectorApiServer(new InetSocketAddress("127.0.0.1", 0), svc, store, box, null, 30));
        assertThrows(IllegalArgumentException.class, () -> new CollectorApiServer(new InetSocketAddress("127.0.0.1", 0), svc, store, box, "short", 30));
        assertThrows(IllegalArgumentException.class, () -> new CollectorApiServer(new InetSocketAddress("127.0.0.1", 0), svc, store, box, apiKey, 0));
        assertThrows(IllegalArgumentException.class, () -> new CollectorApiServer(new InetSocketAddress("127.0.0.1", 0), svc, store, box, apiKey, 4000));

        try (CollectorApiServer api = new CollectorApiServer(new InetSocketAddress("127.0.0.1", 0), svc, store, box, apiKey, 30)) {
            api.start();
            String base = "http://127.0.0.1:" + api.address().getPort();
            HttpClient client = HttpClient.newHttpClient();

            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/health")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/openapi.json")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(401, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/sources")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(401, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/sources")).GET().header("Authorization", "Basic 123").build(), HttpResponse.BodyHandlers.ofString()).statusCode());

            // Authorized requests
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/sources")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items?limit=10&source=WORDPRESS&flag")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items?limit=0")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items?limit=600")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items?source=INVALID")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());

            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());

            // POST /v1/feeds edge cases
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds")).POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"GOOGLE_NEWS\",\"input\":\"java\",\"intervalSeconds\":10}")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds")).POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"GOOGLE_NEWS\",\"input\":\"java\",\"intervalSeconds\":99999}")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(422, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds")).POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"ISSUU\",\"input\":\"java\",\"intervalSeconds\":600}")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());

            // Invalid JSON bodies
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/collections")).POST(HttpRequest.BodyPublishers.ofString("not-json")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/collections")).POST(HttpRequest.BodyPublishers.ofString("[1,2]")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/collections")).POST(HttpRequest.BodyPublishers.ofString("x".repeat(9000))).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/collections")).POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"   \"}")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());

            // DELETE /v1/feeds/{id} & method mismatch branches
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds")).DELETE().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds/" + f1.id())).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds/%20")).DELETE().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items?%20")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/collections")).POST(HttpRequest.BodyPublishers.ofString("null")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/collections")).POST(HttpRequest.BodyPublishers.ofString("{\"source\":123}")).header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(400, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds/a/b")).DELETE().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds/missing")).DELETE().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(204, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/feeds/" + f1.id())).DELETE().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items?a=1&b=")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/items")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/v1/unknown")).GET().header("Authorization", "Bearer " + apiKey).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }

    private static InputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
