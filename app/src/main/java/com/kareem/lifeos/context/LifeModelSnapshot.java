package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Immutable, rebuildable life model derived from raw evidence and context. */
public final class LifeModelSnapshot {
    public final String engineVersion;
    public final long rebuiltAt;
    public final List<LifeFact> factHistory;
    public final Map<String,LifeFact> currentFacts;
    public final List<Situation> situations;

    public LifeModelSnapshot(String engineVersion, long rebuiltAt, List<LifeFact> factHistory,
                             Map<String,LifeFact> currentFacts, List<Situation> situations) {
        this.engineVersion = engineVersion == null ? "" : engineVersion;
        this.rebuiltAt = rebuiltAt;
        this.factHistory = Collections.unmodifiableList(factHistory == null ?
                new ArrayList<LifeFact>() : new ArrayList<LifeFact>(factHistory));
        this.currentFacts = Collections.unmodifiableMap(currentFacts == null ?
                new LinkedHashMap<String,LifeFact>() : new LinkedHashMap<String,LifeFact>(currentFacts));
        this.situations = Collections.unmodifiableList(situations == null ?
                new ArrayList<Situation>() : new ArrayList<Situation>(situations));
    }
}
