package com.kareem.lifeos.memory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.kareem.lifeos.retrieval.HybridMemoryRecall;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;

/**
 * Persistent LifeOS memory adapted from Teya's durable-memory behavior.
 *
 * Unlike the donor, every record can carry the semantic assertion and raw evidence IDs
 * that grounded it, so recall never severs memory from provenance.
 */
public final class PersistentLifeMemoryStore extends SQLiteOpenHelper implements LifeMemoryRepository {
    private static final String DB_NAME = "lifeos_memory_v2.db";
    private static final int DB_VERSION = 1;
    private static volatile PersistentLifeMemoryStore instance;

    private PersistentLifeMemoryStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static PersistentLifeMemoryStore get(Context context) {
        if (instance == null) synchronized (PersistentLifeMemoryStore.class) {
            if (instance == null) instance = new PersistentLifeMemoryStore(context);
        }
        return instance;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE memories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_entity_id TEXT NOT NULL," +
                "text TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "added_at INTEGER NOT NULL," +
                "last_accessed_at INTEGER NOT NULL," +
                "strength REAL NOT NULL," +
                "tier TEXT NOT NULL," +
                "embedding BLOB," +
                "source_assertion_id TEXT NOT NULL," +
                "evidence_ids_json TEXT NOT NULL)");
        db.execSQL("CREATE INDEX memory_subject_idx ON memories(subject_entity_id,added_at DESC)");
        db.execSQL("CREATE INDEX memory_tier_idx ON memories(tier,category,last_accessed_at DESC)");
        db.execSQL("CREATE INDEX memory_category_idx ON memories(category,added_at DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override public synchronized long remember(String subjectEntityId, String text,
                                      MemoryRecord.Category category, float[] embedding,
                                      String sourceAssertionId, List<String> evidenceIds,
                                      long now) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) return -1L;
        ContentValues v = new ContentValues();
        v.put("subject_entity_id", safe(subjectEntityId));
        v.put("text", clean);
        v.put("category", (category == null ? MemoryRecord.Category.FACT : category).name());
        v.put("added_at", now);
        v.put("last_accessed_at", now);
        v.put("strength", 1.0f);
        v.put("tier", MemoryRecord.Tier.HOT.name());
        if (embedding != null) v.put("embedding", floatsToBytes(embedding));
        v.put("source_assertion_id", safe(sourceAssertionId));
        v.put("evidence_ids_json", evidenceJson(evidenceIds));
        return getWritableDatabase().insert("memories", null, v);
    }

    @Override public synchronized List<MemoryRecord> recall(String query, float[] queryEmbedding,
                                                   int topK, long now) {
        List<MemoryRecord> ranked = HybridMemoryRecall.rank(searchable(), query, queryEmbedding, topK);
        SQLiteDatabase db = getWritableDatabase();
        for (MemoryRecord record : ranked) {
            ContentValues v = new ContentValues();
            v.put("strength", 1.0f);
            v.put("last_accessed_at", now);
            v.put("tier", MemoryRecord.Tier.HOT.name());
            db.update("memories", v, "id=?", new String[]{Long.toString(record.id)});
        }
        List<MemoryRecord> reinforced = new ArrayList<MemoryRecord>();
        for (MemoryRecord record : ranked) reinforced.add(record.withRecall(now));
        return reinforced;
    }

    /** HOT subject memory for bounded always-available context. */
    @Override public synchronized List<MemoryRecord> hotForSubject(String subjectEntityId, int limit) {
        return query("subject_entity_id=? AND tier=?",
                new String[]{safe(subjectEntityId), MemoryRecord.Tier.HOT.name()},
                Math.max(1, Math.min(100, limit)));
    }

    /** General retrieval pool includes COLD records and all non-subject-specific durable memory. */
    @Override public synchronized List<MemoryRecord> searchable() {
        return query(null, null, 5000);
    }

    @Override public synchronized List<MemoryRecord> recentEpisodic(long since, int limit) {
        return query("category=? AND added_at>=?",
                new String[]{MemoryRecord.Category.EPISODIC.name(), Long.toString(since)},
                Math.max(1, Math.min(1000, limit)));
    }

    @Override public synchronized boolean hasSimilar(String text, String subjectEntityId) {
        List<MemoryRecord> scope = subjectEntityId == null || subjectEntityId.trim().isEmpty()
                ? searchable() : query("subject_entity_id=?", new String[]{subjectEntityId}, 5000);
        for (MemoryRecord record : scope) {
            if (record.category != MemoryRecord.Category.EPISODIC
                    && MemoryAlgorithms.similarText(record.text, text)) return true;
        }
        return false;
    }

    /** Deterministic forgetting pass. Episodic records may die; durable records only cool. */
    @Override public synchronized DecaySummary runDecay(long now) {
        List<MemoryRecord> all = searchable();
        SQLiteDatabase db = getWritableDatabase();
        int cooled = 0, pruned = 0;
        for (MemoryRecord record : all) {
            float strength = MemoryAlgorithms.strengthNow(record, now);
            if (MemoryAlgorithms.shouldPrune(record, now)) {
                db.delete("memories", "id=?", new String[]{Long.toString(record.id)});
                pruned++;
                continue;
            }
            MemoryRecord.Tier tier = MemoryAlgorithms.tierFor(strength);
            if (record.tier == MemoryRecord.Tier.HOT && tier == MemoryRecord.Tier.COLD) cooled++;
            ContentValues v = new ContentValues();
            v.put("strength", strength);
            v.put("tier", tier.name());
            db.update("memories", v, "id=?", new String[]{Long.toString(record.id)});
        }
        return new DecaySummary(all.size(), cooled, pruned, now);
    }

    @Override public synchronized int forgetBySubstring(String query, String subjectEntityId) {
        String normalized = MemoryAlgorithms.normalize(query);
        if (normalized.length() < 3) return 0;
        List<MemoryRecord> pool = subjectEntityId == null || subjectEntityId.trim().isEmpty()
                ? searchable() : query("subject_entity_id=?", new String[]{subjectEntityId}, 5000);
        int removed = 0;
        SQLiteDatabase db = getWritableDatabase();
        for (MemoryRecord record : pool) {
            if (MemoryAlgorithms.normalize(record.text).contains(normalized)) {
                removed += db.delete("memories", "id=?", new String[]{Long.toString(record.id)});
            }
        }
        return removed;
    }

    @Override public synchronized void eraseAll() { getWritableDatabase().delete("memories", null, null); }

    private List<MemoryRecord> query(String selection, String[] args, int limit) {
        List<MemoryRecord> out = new ArrayList<MemoryRecord>();
        Cursor c = getReadableDatabase().query("memories",
                new String[]{"id","subject_entity_id","text","category","added_at","last_accessed_at","strength","tier","embedding","source_assertion_id","evidence_ids_json"},
                selection,args,null,null,"last_accessed_at DESC,id DESC",Integer.toString(limit));
        try {
            while (c.moveToNext()) {
                MemoryRecord.Category category;
                MemoryRecord.Tier tier;
                try { category = MemoryRecord.Category.valueOf(c.getString(3)); }
                catch (Exception e) { category = MemoryRecord.Category.FACT; }
                try { tier = MemoryRecord.Tier.valueOf(c.getString(7)); }
                catch (Exception e) { tier = MemoryRecord.Tier.HOT; }
                out.add(new MemoryRecord(c.getLong(0), c.getString(1), c.getString(2), category,
                        c.getLong(4), c.getLong(5), c.getFloat(6), tier,
                        bytesToFloats(c.isNull(8) ? null : c.getBlob(8)), c.getString(9),
                        evidenceList(c.getString(10))));
            }
        } finally { c.close(); }
        return out;
    }

    private static byte[] floatsToBytes(float[] values) {
        ByteBuffer b = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) b.putFloat(value);
        return b.array();
    }

    private static float[] bytesToFloats(byte[] values) {
        if (values == null || values.length == 0 || values.length % 4 != 0) return null;
        ByteBuffer b = ByteBuffer.wrap(values).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[values.length / 4];
        for (int i = 0; i < out.length; i++) out[i] = b.getFloat();
        return out;
    }

    private static String evidenceJson(List<String> evidenceIds) {
        JSONArray a = new JSONArray();
        if (evidenceIds != null) for (String id : evidenceIds) a.put(safe(id));
        return a.toString();
    }

    private static List<String> evidenceList(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        try {
            JSONArray a = new JSONArray(json);
            for (int i=0;i<a.length();i++) out.add(a.optString(i, ""));
        } catch (Exception ignored) {}
        return out;
    }

    private static String safe(String value) { return value == null ? "" : value; }

    public static final class DecaySummary {
        public final int scanned, cooled, pruned; public final long at;
        public DecaySummary(int scanned, int cooled, int pruned, long at) {
            this.scanned=scanned; this.cooled=cooled; this.pruned=pruned; this.at=at;
        }
    }
}
