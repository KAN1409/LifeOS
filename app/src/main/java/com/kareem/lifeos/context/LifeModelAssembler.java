package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the durable semantic layer without reparsing source text.
 *
 * Only observations carrying explicit normalized `life_fact_*` attributes become LifeFacts.
 * This keeps semantic extraction replaceable and prevents the Life Model from depending on
 * WhatsApp/UI wording. History is retained; current facts are a projection over revisions.
 */
public final class LifeModelAssembler {
    public static final String VERSION = "v2.3.0";

    private LifeModelAssembler() {}

    public static LifeModelSnapshot rebuild(List<RawObservation> observations,
                                            LifeContextSnapshot context,
                                            long rebuiltAt) {
        List<RawObservation> ordered = new ArrayList<RawObservation>(
                observations == null ? Collections.<RawObservation>emptyList() : observations);
        Collections.sort(ordered, new Comparator<RawObservation>() {
            @Override public int compare(RawObservation a, RawObservation b) {
                int t = Long.compare(a.observedAt, b.observedAt);
                return t != 0 ? t : a.observationId.compareTo(b.observationId);
            }
        });

        List<LifeFact> history = new ArrayList<LifeFact>();
        LinkedHashMap<String,LifeFact> latest = new LinkedHashMap<String,LifeFact>();
        for (RawObservation observation : ordered) {
            LifeFact fact = factFrom(observation);
            if (fact == null) continue;
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

    private static LifeFact factFrom(RawObservation o) {
        if (o == null || o.attributes == null) return null;
        String predicate = attr(o, "life_fact_predicate");
        if (predicate.isEmpty()) return null;

        String subject = attr(o, "life_fact_subject_id");
        if (subject.isEmpty()) subject = attr(o, "canonical_entity_id");
        if (subject.isEmpty()) subject = "stream:" + EntityResolver.normalize(o.streamId);

        String value = attr(o, "life_fact_value");
        String stateText = attr(o, "life_fact_state");
        LifeFact.State state = "RETRACTED".equals(stateText.toUpperCase(Locale.ROOT))
                ? LifeFact.State.RETRACTED : LifeFact.State.ASSERTED;
        long validFrom = parseLong(attr(o, "life_fact_valid_from"), o.observedAt);
        long validTo = parseLong(attr(o, "life_fact_valid_to"), 0L);
        double confidence = parseDouble(attr(o, "life_fact_confidence"), 1.0);

        List<String> evidence = new ArrayList<String>();
        evidence.add(o.observationId);
        return new LifeFact("fact:" + o.observationId, normalizeSubject(subject),
                EntityResolver.normalize(predicate), value, state, o.observedAt,
                validFrom, validTo, confidence, evidence);
    }

    private static String normalizeSubject(String subject) {
        String s = EntityResolver.normalize(subject);
        return s.startsWith("entity:") || s.startsWith("stream:") ? s : "entity:" + s;
    }

    private static String attr(RawObservation o, String key) {
        String value = o.attributes.get(key);
        return value == null ? "" : value.trim();
    }

    private static long parseLong(String value, long fallback) {
        try { return Long.parseLong(value); } catch (Exception ignored) { return fallback; }
    }

    private static double parseDouble(String value, double fallback) {
        try { return Math.max(0.0, Math.min(1.0, Double.parseDouble(value))); }
        catch (Exception ignored) { return fallback; }
    }
}
