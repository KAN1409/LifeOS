package com.kareem.lifeos.context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Android/text adaptation of Open Intelligence SecondBrain's cheap-first capture
 * dedup cascade (MIT, capture/dedup.py).
 *
 * The donor uses dirty-rect -> dHash -> pHash -> SSIM for images. LifeOS applies
 * the same principle to source-neutral Android observations: exact normalized
 * fingerprint first, then a cheap token-overlap gate for near-identical UI trees.
 * This class only decides whether a candidate is worth persisting; it never mutates
 * an already persisted RawObservation.
 */
public final class ObservationDeduplicator {
    public enum Gate { FIRST, EXACT, NEAR_DUPLICATE, PERSIST }

    public static final class Decision {
        public final boolean persist;
        public final Gate gate;
        public final double similarity;
        Decision(boolean persist, Gate gate, double similarity) {
            this.persist = persist;
            this.gate = gate;
            this.similarity = similarity;
        }
    }

    private static final double DEFAULT_NEAR_DUPLICATE = 0.985;
    private final double nearDuplicateThreshold;
    private final Map<String,Fingerprint> previous = new HashMap<String,Fingerprint>();

    public ObservationDeduplicator() { this(DEFAULT_NEAR_DUPLICATE); }

    public ObservationDeduplicator(double nearDuplicateThreshold) {
        this.nearDuplicateThreshold = Math.max(0.0, Math.min(1.0, nearDuplicateThreshold));
    }

    public synchronized Decision evaluate(RawObservation observation) {
        if (observation == null) return new Decision(false, Gate.EXACT, 1.0);
        String key = streamKey(observation);
        Fingerprint current = Fingerprint.of(observation);
        Fingerprint prior = previous.get(key);
        if (prior == null) {
            previous.put(key, current);
            return new Decision(true, Gate.FIRST, 0.0);
        }
        if (prior.sha256.equals(current.sha256)) {
            return new Decision(false, Gate.EXACT, 1.0);
        }
        double similarity = jaccard(prior.tokens, current.tokens);
        if (similarity >= nearDuplicateThreshold) {
            // Advance state just like SecondBrain advances duplicate-frame state,
            // preventing repeated comparison against a stale snapshot.
            previous.put(key, current);
            return new Decision(false, Gate.NEAR_DUPLICATE, similarity);
        }
        previous.put(key, current);
        return new Decision(true, Gate.PERSIST, similarity);
    }

    public synchronized void reset() { previous.clear(); }

    static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\\"(?:capturedAt|timestamp|observedAt)\\\"\\s*:\\s*\\d+", "\"time\":0")
                .replaceAll("\\s+", " ").trim();
    }

    private static String streamKey(RawObservation o) {
        return o.sourceKind.name() + "|" + o.sourcePackage + "|" + o.streamId + "|" + o.eventType;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> union = new HashSet<String>(a); union.addAll(b);
        Set<String> intersection = new HashSet<String>(a); intersection.retainAll(b);
        return union.isEmpty() ? 1.0 : ((double) intersection.size() / (double) union.size());
    }

    private static final class Fingerprint {
        final String sha256;
        final Set<String> tokens;
        Fingerprint(String sha256, Set<String> tokens) { this.sha256 = sha256; this.tokens = tokens; }

        static Fingerprint of(RawObservation o) {
            String payload = normalize((o.text == null ? "" : o.text) + " " +
                    (o.rawPayload == null ? "" : o.rawPayload));
            Set<String> tokens = new HashSet<String>();
            for (String token : payload.split("[^\\p{L}\\p{N}_]+")) {
                if (!token.isEmpty()) tokens.add(token);
            }
            return new Fingerprint(sha(payload), tokens);
        }

        private static String sha(String value) {
            try {
                byte[] bytes = MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8));
                StringBuilder out = new StringBuilder();
                for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b));
                return out.toString();
            } catch (Exception e) {
                return Integer.toHexString(value.hashCode());
            }
        }
    }
}
