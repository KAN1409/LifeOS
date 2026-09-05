package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A normalized interpretation derived from raw observations.
 * It always keeps provenance so interpretations can be rebuilt when the engine improves.
 */
public final class ContextEvent {
    public final String eventId;
    public final String type;
    public final String actorId;
    public final String streamId;
    public final String summary;
    public final long occurredAt;
    public final double confidence;
    public final List<String> evidenceIds;

    public ContextEvent(String eventId, String type, String actorId, String streamId,
                        String summary, long occurredAt, double confidence,
                        List<String> evidenceIds) {
        this.eventId = safe(eventId);
        this.type = safe(type);
        this.actorId = safe(actorId);
        this.streamId = safe(streamId);
        this.summary = safe(summary);
        this.occurredAt = occurredAt;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.evidenceIds = Collections.unmodifiableList(evidenceIds == null
                ? new ArrayList<String>() : new ArrayList<String>(evidenceIds));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
