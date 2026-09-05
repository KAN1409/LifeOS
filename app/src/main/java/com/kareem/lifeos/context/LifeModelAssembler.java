package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the semantic Life Model from replaceable semantic assertions.
 * Raw observations remain immutable evidence and are never treated as semantic truth directly.
 */
public final class LifeModelAssembler {
    public static final String VERSION = "v2.3.1";

    private LifeModelAssembler() {}

    public static LifeModelSnapshot rebuild(List<SemanticAssertion> assertions,
                                            LifeContextSnapshot context,
                                            long rebuiltAt) {
        List<SemanticAssertion> ordered = new ArrayList<SemanticAssertion>(
                assertions == null ? Collections.<SemanticAssertion>emptyList() : assertions);
        Collections.sort(ordered, new Comparator<SemanticAssertion>() {
            @Override public int compare(SemanticAssertion a, SemanticAssertion b) {
                int t = Long.compare(a.observedAt, b.observedAt);
                return t != 0 ? t : a.assertionId.compareTo(b.assertionId);
            }
        });

        List<LifeFact> history = new ArrayList<LifeFact>();
        LinkedHashMap<String,LifeFact> latest = new LinkedHashMap<String,LifeFact>();
        for (SemanticAssertion assertion : ordered) {
            if (assertion == null || assertion.predicate.trim().isEmpty()) continue;
            LifeFact fact = factFrom(assertion);
            history.add(fact);
            LifeFact old = latest.get(fact.logicalKey());
            if (old == null || fact.observedAt > old.observedAt ||
                    (fact.observedAt == old.observedAt && fact.factId.compareTo(old.factId) > 0)) {
                latest.put(fact.logicalKey(), fact);
            }
        }

        LinkedHashMap<String,LifeFact> current = new LinkedHashMap<String,LifeFact>();
        for (Map.Entry<String,LifeFact> e : latest.entrySet()) {
            LifeFact fact = e.getValue();
            if (fact.state == LifeFact.State.ASSERTED &&
                    (fact.validTo <= 0L || fact.validTo >= rebuiltAt)) {
                current.put(e.getKey(), fact);
            }
        }

        List<Situation> situations = context == null ?
                Collections.<Situation>emptyList() : context.situations;
        return new LifeModelSnapshot(VERSION, rebuiltAt, history, current, situations);
    }

    private static LifeFact factFrom(SemanticAssertion a) {
        LifeFact.State state = a.state == SemanticAssertion.State.RETRACTED
                ? LifeFact.State.RETRACTED : LifeFact.State.ASSERTED;
        return new LifeFact("fact:" + a.assertionId, normalizeSubject(a.subjectEntityId),
                EntityResolver.normalize(a.predicate), a.value, state, a.observedAt,
                a.validFrom, a.validTo, a.confidence, a.evidenceIds);
    }

    private static String normalizeSubject(String subject) {
        String s = EntityResolver.normalize(subject);
        if (s.isEmpty()) return "entity:unknown";
        return s.startsWith("entity:") || s.startsWith("stream:") ? s : "entity:" + s;
    }
}
