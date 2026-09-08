package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Recomputes semantic assertions deterministically from retained raw evidence. */
public final class SemanticReplayEngine {
    private SemanticReplayEngine() {}

    public static List<SemanticAssertion> replay(List<RawObservation> observations,
                                                  SemanticInterpreter interpreter) {
        if (interpreter == null) throw new IllegalArgumentException("interpreter required");
        List<RawObservation> ordered = new ArrayList<RawObservation>(
                observations == null ? Collections.<RawObservation>emptyList() : observations);
        Collections.sort(ordered, new Comparator<RawObservation>() {
            @Override public int compare(RawObservation a, RawObservation b) {
                int t = Long.compare(a.observedAt, b.observedAt);
                return t != 0 ? t : a.observationId.compareTo(b.observationId);
            }
        });
        List<SemanticAssertion> out = new ArrayList<SemanticAssertion>();
        for (RawObservation observation : ordered) {
            if (observation == null) continue;
            List<SemanticAssertion> assertions = interpreter.interpret(observation);
            if (assertions != null) for (SemanticAssertion assertion : assertions)
                if (assertion != null) out.add(assertion);
        }
        Collections.sort(out, new Comparator<SemanticAssertion>() {
            @Override public int compare(SemanticAssertion a, SemanticAssertion b) {
                int t = Long.compare(a.observedAt, b.observedAt);
                return t != 0 ? t : a.assertionId.compareTo(b.assertionId);
            }
        });
        return out;
    }
}
