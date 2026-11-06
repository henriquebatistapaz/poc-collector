package com.henrique.multisourcecollector.source;

import static org.junit.jupiter.api.Assertions.*;

import com.henrique.multisourcecollector.feed.RssFeedParser;
import org.junit.jupiter.api.Test;

class SourceRegistryTest {
    @Test
    void describesCapabilityOfEveryRequestedSource() {
        SourceRegistry registry = new SourceRegistry();
        assertEquals(6, registry.all().size());
        assertEquals(CapabilityStatus.CONDITIONAL, registry.get(CollectorSource.GOOGLE_NEWS).status());
        assertEquals(CapabilityStatus.CONDITIONAL, registry.get(CollectorSource.GOOGLE_ALERTS).status());
        assertEquals(CapabilityStatus.AVAILABLE, registry.get(CollectorSource.WORDPRESS).status());
        assertEquals(CapabilityStatus.UNAVAILABLE, registry.get(CollectorSource.REDDIT).status());
        assertEquals(CapabilityStatus.UNVERIFIED, registry.get(CollectorSource.ISSUU).status());
        assertEquals(CapabilityStatus.UNAVAILABLE, registry.get(CollectorSource.MERCADO_LIVRE).status());
    }

    @Test
    void refusesCollectorsWithoutPermittedAccess() {
        SourceRegistry registry = new SourceRegistry();
        var fetcher = (com.henrique.multisourcecollector.feed.FeedFetcher) uri -> new byte[0];
        var parser = new RssFeedParser();
        var news = new GoogleNewsCollector(fetcher, parser);
        var alerts = new GoogleAlertsCollector(fetcher, parser, new GoogleAlertsUrlPolicy());
        var wordpress = new WordPressFeedCollector(fetcher, parser, new PublicHttpsFeedUrlPolicy(host -> new java.net.InetAddress[]{
                java.net.InetAddress.getByAddress(new byte[]{8, 8, 8, 8})}));
        CollectorService service = new CollectorService(registry, news, alerts, wordpress);
        assertThrows(SourceAccessException.class, () -> service.collect(CollectorSource.REDDIT, "r/example"));
        assertThrows(SourceAccessException.class, () -> service.collect(CollectorSource.ISSUU, "example"));
        assertThrows(SourceAccessException.class, () -> service.collect(CollectorSource.MERCADO_LIVRE, "query"));
    }
}
