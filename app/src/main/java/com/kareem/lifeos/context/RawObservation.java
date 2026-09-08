package com.kareem.lifeos.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable source-agnostic fact captured from the device.
 *
 * Capture adapters must not infer life meaning here. A WhatsApp notification,
 * accessibility node, call, calendar event, file event, or future sensor all
 * enter Cortex/LifeOS through this same contract. Interpretation happens later.
 */
public final class RawObservation {
    public enum SourceKind {
        NOTIFICATION, ACCESSIBILITY, SCREEN, CALL, SMS, EMAIL, CALENDAR,
        FILE, MEDIA, APP_ACTIVITY, VOICE, LOCATION, DEVICE, OTHER
    }

    public final String observationId;
    public final SourceKind sourceKind;
    public final String sourcePackage;
    public final String streamId;
    public final String eventType;
    public final long observedAt;
    public final String text;
    public final String rawPayload;
    public final Map<String,String> attributes;

    public RawObservation(String observationId, SourceKind sourceKind,
                          String sourcePackage, String streamId, String eventType,
                          long observedAt, String text, String rawPayload,
                          Map<String,String> attributes) {
        this.observationId = safe(observationId);
        this.sourceKind = sourceKind == null ? SourceKind.OTHER : sourceKind;
        this.sourcePackage = safe(sourcePackage);
        this.streamId = safe(streamId);
        this.eventType = safe(eventType);
        this.observedAt = observedAt;
        this.text = safe(text);
        this.rawPayload = safe(rawPayload);
        this.attributes = Collections.unmodifiableMap(attributes == null
                ? new HashMap<String,String>()
                : new HashMap<String,String>(attributes));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
