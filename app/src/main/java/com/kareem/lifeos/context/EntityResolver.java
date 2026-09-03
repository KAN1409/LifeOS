package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Conservative entity resolver. It promotes only explicit capture metadata and stream identity;
 * it does not guess people from arbitrary message text.
 */
public final class EntityResolver {
    private EntityResolver() {}

    public static List<EntityRef> resolve(RawObservation observation) {
        LinkedHashMap<String,EntityRef> out = new LinkedHashMap<String,EntityRef>();
        if (observation == null) return new ArrayList<EntityRef>();

        if (!observation.sourcePackage.isEmpty()) {
            put(out, new EntityRef("app:" + normalize(observation.sourcePackage),
                    EntityRef.Kind.APPLICATION, observation.sourcePackage, 1.0));
        }

        String conversation = firstNonEmpty(observation.attributes.get("conversation_title"),
                observation.attributes.get("conversation"));
        String title = observation.attributes.get("title");
        String streamLabel = streamLabel(observation.streamId, observation.sourcePackage);

        if (!conversation.isEmpty()) {
            put(out, new EntityRef("conversation:" + normalize(conversation),
                    EntityRef.Kind.CONVERSATION, conversation, 0.98));
        } else if (!streamLabel.isEmpty()) {
            put(out, new EntityRef("conversation:" + normalize(streamLabel),
                    EntityRef.Kind.CONVERSATION, streamLabel, 0.80));
        } else if (title != null && !title.trim().isEmpty() && observation.sourceKind == RawObservation.SourceKind.NOTIFICATION) {
            put(out, new EntityRef("conversation:" + normalize(title),
                    EntityRef.Kind.CONVERSATION, title.trim(), 0.65));
        }
        return new ArrayList<EntityRef>(out.values());
    }

    public static List<EntityRef> merge(List<EntityRef> a, List<EntityRef> b) {
        LinkedHashMap<String,EntityRef> out = new LinkedHashMap<String,EntityRef>();
        if (a != null) for (EntityRef e : a) put(out, e);
        if (b != null) for (EntityRef e : b) put(out, e);
        return new ArrayList<EntityRef>(out.values());
    }

    private static void put(Map<String,EntityRef> out, EntityRef e) {
        if (e == null || e.entityId.isEmpty()) return;
        EntityRef old = out.get(e.entityId);
        if (old == null || e.confidence > old.confidence) out.put(e.entityId, e);
    }

    static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        return b == null ? "" : b.trim();
    }

    private static String streamLabel(String streamId, String sourcePackage) {
        if (streamId == null || streamId.trim().isEmpty()) return "";
        String stream = streamId.trim();
        String pkg = sourcePackage == null ? "" : sourcePackage.trim();
        if (!pkg.isEmpty() && stream.equals(pkg)) return "";
        if (!pkg.isEmpty() && stream.startsWith(pkg + "|")) return stream.substring(pkg.length() + 1).trim();
        int pipe = stream.indexOf('|');
        return pipe >= 0 && pipe + 1 < stream.length() ? stream.substring(pipe + 1).trim() : stream;
    }
}
