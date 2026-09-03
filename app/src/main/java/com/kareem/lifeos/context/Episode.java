package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Time-bounded cluster of context events that belong to one interaction stream. */
public final class Episode {
    public final String episodeId;
    public final String streamId;
    public final long startedAt;
    public final long endedAt;
    public final List<ContextEvent> events;
    public final List<EntityRef> entities;

    public Episode(String episodeId, String streamId, long startedAt, long endedAt,
                   List<ContextEvent> events, List<EntityRef> entities) {
        this.episodeId = episodeId == null ? "" : episodeId;
        this.streamId = streamId == null ? "" : streamId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.events = Collections.unmodifiableList(events == null ?
                new ArrayList<ContextEvent>() : new ArrayList<ContextEvent>(events));
        this.entities = Collections.unmodifiableList(entities == null ?
                new ArrayList<EntityRef>() : new ArrayList<EntityRef>(entities));
    }
}
