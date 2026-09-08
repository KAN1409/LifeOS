package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cross-source, cross-episode real-life situation assembled from evidence-linked episodes. */
public final class Situation {
    public final String situationId;
    public final long startedAt;
    public final long updatedAt;
    public final String title;
    public final List<Episode> episodes;
    public final List<EntityRef> entities;
    public final double confidence;

    public Situation(String situationId, long startedAt, long updatedAt, String title,
                     List<Episode> episodes, List<EntityRef> entities, double confidence) {
        this.situationId = situationId == null ? "" : situationId;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
        this.title = title == null ? "" : title;
        this.episodes = Collections.unmodifiableList(episodes == null ?
                new ArrayList<Episode>() : new ArrayList<Episode>(episodes));
        this.entities = Collections.unmodifiableList(entities == null ?
                new ArrayList<EntityRef>() : new ArrayList<EntityRef>(entities));
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
}
