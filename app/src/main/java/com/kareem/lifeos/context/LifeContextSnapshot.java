package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable derived view rebuilt from raw observations. */
public final class LifeContextSnapshot {
    public final String engineVersion;
    public final long rebuiltAt;
    public final List<Episode> episodes;
    public final List<Situation> situations;

    public LifeContextSnapshot(String engineVersion, long rebuiltAt,
                               List<Episode> episodes, List<Situation> situations) {
        this.engineVersion = engineVersion == null ? "" : engineVersion;
        this.rebuiltAt = rebuiltAt;
        this.episodes = Collections.unmodifiableList(episodes == null ?
                new ArrayList<Episode>() : new ArrayList<Episode>(episodes));
        this.situations = Collections.unmodifiableList(situations == null ?
                new ArrayList<Situation>() : new ArrayList<Situation>(situations));
    }
}
