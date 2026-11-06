package com.henrique.multisourcecollector.source;

public record SourceCapability(CollectorSource source, CapabilityStatus status, String reason) { }
