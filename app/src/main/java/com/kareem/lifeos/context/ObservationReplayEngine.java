package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Rebuilds derived context deterministically from retained raw observations. */
public final class ObservationReplayEngine {
    private ObservationReplayEngine() {}

    public static List<ContextEvent> replay(List<RawObservation> observations,
                                            ContextInterpreter interpreter) {
        if (interpreter == null) throw new IllegalArgumentException("interpreter required");
        List<RawObservation> ordered = new ArrayList<RawObservation>(
                observations == null ? Collections.<RawObservation>emptyList() : observations);
        Collections.sort(ordered, new Comparator<RawObservation>() {
            @Override public int compare(RawObservation a, RawObservation b) {
                int t = Long.compare(a.observedAt, b.observedAt);
                return t != 0 ? t : a.observationId.compareTo(b.observationId);
            }
        });

        List<ContextEvent> out = new ArrayList<ContextEvent>();
        for (RawObservation observation : ordered) {
            if (observation == null) continue;
            List<ContextEvent> events = interpreter.interpret(observation);
            if (events != null) for (ContextEvent event : events) if (event != null) out.add(event);
        }
        return out;
    }
}
