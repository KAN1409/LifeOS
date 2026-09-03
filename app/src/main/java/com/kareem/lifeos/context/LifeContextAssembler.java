package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic V2.2 context assembler.
 *
 * Rules are intentionally source-neutral and conservative:
 * - observations are ordered by time;
 * - an episode is a continuous interaction stream with <=15 minute gaps;
 * - interleaved streams do not break each other's episodes;
 * - situations merge episodes only when they share a non-application entity and are <=6 hours apart.
 * This avoids turning app identity alone into semantic identity.
 */
public final class LifeContextAssembler {
    public static final String VERSION = "v2.2.0";
    static final long EPISODE_GAP_MS = 15L * 60L * 1000L;
    static final long SITUATION_GAP_MS = 6L * 60L * 60L * 1000L;

    private LifeContextAssembler() {}

    public static LifeContextSnapshot rebuild(List<RawObservation> observations, long rebuiltAt) {
        List<RawObservation> ordered = new ArrayList<RawObservation>(
                observations == null ? Collections.<RawObservation>emptyList() : observations);
        Collections.sort(ordered, new Comparator<RawObservation>() {
            @Override public int compare(RawObservation a, RawObservation b) {
                int t = Long.compare(a.observedAt, b.observedAt);
                return t != 0 ? t : a.observationId.compareTo(b.observationId);
            }
        });
        List<Episode> episodes = buildEpisodes(ordered);
        List<Situation> situations = buildSituations(episodes);
        return new LifeContextSnapshot(VERSION, rebuiltAt, episodes, situations);
    }

    private static List<Episode> buildEpisodes(List<RawObservation> ordered) {
        List<EpisodeBuilder> completed = new ArrayList<EpisodeBuilder>();
        Map<String,EpisodeBuilder> activeByStream = new LinkedHashMap<String,EpisodeBuilder>();
        for (RawObservation o : ordered) {
            if (o == null) continue;
            EpisodeBuilder active = activeByStream.get(o.streamId);
            if (active == null) {
                activeByStream.put(o.streamId, new EpisodeBuilder(o));
            } else if (active.accepts(o)) {
                active.add(o);
            } else {
                completed.add(active);
                activeByStream.put(o.streamId, new EpisodeBuilder(o));
            }
        }
        completed.addAll(activeByStream.values());
        List<Episode> out = new ArrayList<Episode>();
        for (EpisodeBuilder b : completed) out.add(b.finish());
        Collections.sort(out, new Comparator<Episode>() {
            @Override public int compare(Episode a, Episode b) {
                int t = Long.compare(a.startedAt, b.startedAt);
                return t != 0 ? t : a.episodeId.compareTo(b.episodeId);
            }
        });
        return out;
    }

    private static List<Situation> buildSituations(List<Episode> episodes) {
        List<SituationBuilder> builders = new ArrayList<SituationBuilder>();
        for (Episode episode : episodes) {
            SituationBuilder best = null;
            for (SituationBuilder candidate : builders) {
                if (candidate.accepts(episode)) { best = candidate; break; }
            }
            if (best == null) builders.add(new SituationBuilder(episode));
            else best.add(episode);
        }
        List<Situation> out = new ArrayList<Situation>();
        for (SituationBuilder b : builders) out.add(b.finish());
        Collections.sort(out, new Comparator<Situation>() {
            @Override public int compare(Situation a, Situation b) { return Long.compare(a.startedAt, b.startedAt); }
        });
        return out;
    }

    private static final class EpisodeBuilder {
        final String streamId;
        final List<ContextEvent> events = new ArrayList<ContextEvent>();
        List<EntityRef> entities = new ArrayList<EntityRef>();
        long start;
        long end;

        EpisodeBuilder(RawObservation first) {
            streamId = first.streamId;
            start = end = first.observedAt;
            add(first);
        }

        boolean accepts(RawObservation o) {
            return streamId.equals(o.streamId) && o.observedAt - end <= EPISODE_GAP_MS;
        }

        void add(RawObservation o) {
            start = Math.min(start, o.observedAt);
            end = Math.max(end, o.observedAt);
            List<String> evidence = new ArrayList<String>();
            evidence.add(o.observationId);
            events.add(new ContextEvent("event:" + o.observationId, o.eventType, "", o.streamId,
                    o.text, o.observedAt, 1.0, evidence));
            entities = EntityResolver.merge(entities, EntityResolver.resolve(o));
        }

        Episode finish() {
            String id = "episode:" + EntityResolver.normalize(streamId) + ":" + start;
            return new Episode(id, streamId, start, end, events, entities);
        }
    }

    private static final class SituationBuilder {
        final List<Episode> episodes = new ArrayList<Episode>();
        List<EntityRef> entities = new ArrayList<EntityRef>();
        long start;
        long end;

        SituationBuilder(Episode first) { add(first); }

        boolean accepts(Episode episode) {
            if (episode.startedAt - end > SITUATION_GAP_MS) return false;
            return sharesSemanticEntity(entities, episode.entities);
        }

        void add(Episode episode) {
            episodes.add(episode);
            start = episodes.size() == 1 ? episode.startedAt : Math.min(start, episode.startedAt);
            end = Math.max(end, episode.endedAt);
            entities = EntityResolver.merge(entities, episode.entities);
        }

        Situation finish() {
            EntityRef primary = primaryEntity(entities);
            String title = primary == null ? (episodes.isEmpty() ? "Situation" : episodes.get(0).streamId) : primary.label;
            String key = primary == null ? EntityResolver.normalize(title) : primary.entityId;
            double confidence = primary == null ? 0.55 : primary.confidence;
            return new Situation("situation:" + key + ":" + start, start, end, title,
                    episodes, entities, confidence);
        }
    }

    private static boolean sharesSemanticEntity(List<EntityRef> a, List<EntityRef> b) {
        Map<String,EntityRef.Kind> ids = new LinkedHashMap<String,EntityRef.Kind>();
        if (a != null) for (EntityRef e : a) if (e != null && e.kind != EntityRef.Kind.APPLICATION) ids.put(e.entityId, e.kind);
        if (b != null) for (EntityRef e : b) if (e != null && e.kind != EntityRef.Kind.APPLICATION && ids.containsKey(e.entityId)) return true;
        return false;
    }

    private static EntityRef primaryEntity(List<EntityRef> entities) {
        EntityRef best = null;
        if (entities != null) for (EntityRef e : entities) {
            if (e == null || e.kind == EntityRef.Kind.APPLICATION) continue;
            if (best == null || e.confidence > best.confidence) best = e;
        }
        return best;
    }
}
