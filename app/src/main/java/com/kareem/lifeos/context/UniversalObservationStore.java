package com.kareem.lifeos.context;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** Durable append-oriented store for source-agnostic raw observations. */
public final class UniversalObservationStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "lifeos_context_v2.db";
    private static final int DB_VERSION = 1;
    private static final int MAX_ROWS = 20000;
    private static volatile UniversalObservationStore instance;

    private UniversalObservationStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static UniversalObservationStore get(Context context) {
        if (instance == null) {
            synchronized (UniversalObservationStore.class) {
                if (instance == null) instance = new UniversalObservationStore(context);
            }
        }
        return instance;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE observations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "observation_id TEXT NOT NULL UNIQUE," +
                "source_kind TEXT NOT NULL," +
                "source_package TEXT NOT NULL," +
                "stream_id TEXT NOT NULL," +
                "event_type TEXT NOT NULL," +
                "observed_at INTEGER NOT NULL," +
                "text TEXT NOT NULL," +
                "raw_payload TEXT NOT NULL," +
                "attributes_json TEXT NOT NULL)");
        db.execSQL("CREATE INDEX observations_time_idx ON observations(observed_at DESC)");
        db.execSQL("CREATE INDEX observations_source_idx ON observations(source_kind,source_package,observed_at DESC)");
        db.execSQL("CREATE INDEX observations_stream_idx ON observations(stream_id,observed_at DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public synchronized boolean append(RawObservation o) {
        if (o == null || o.observationId.isEmpty()) return false;
        ContentValues v = new ContentValues();
        v.put("observation_id", o.observationId);
        v.put("source_kind", o.sourceKind.name());
        v.put("source_package", o.sourcePackage);
        v.put("stream_id", o.streamId);
        v.put("event_type", o.eventType);
        v.put("observed_at", o.observedAt);
        v.put("text", o.text);
        v.put("raw_payload", o.rawPayload);
        v.put("attributes_json", attributesJson(o.attributes));
        long id = getWritableDatabase().insertWithOnConflict(
                "observations", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) trim();
        return id > 0;
    }

    public synchronized List<RawObservation> recent(int limit) {
        int safe = Math.max(1, Math.min(2000, limit));
        List<RawObservation> out = new ArrayList<RawObservation>();
        Cursor c = getReadableDatabase().query("observations", columns(),
                null,null,null,null,"observed_at DESC,id DESC",Integer.toString(safe));
        try { while (c.moveToNext()) out.add(read(c)); }
        finally { c.close(); }
        return out;
    }

    /** Exact durable lookup used by the semantic queue; opening/dismissing the Android notification cannot remove it. */
    public synchronized RawObservation byObservationId(String observationId) {
        String id=observationId==null?"":observationId.trim();if(id.isEmpty())return null;
        Cursor c=getReadableDatabase().query("observations",columns(),"observation_id=?",new String[]{id},null,null,null,"1");
        try{return c.moveToFirst()?read(c):null;}finally{c.close();}
    }

    /** Chronological context ending at the target observation time. */
    public synchronized List<RawObservation> streamThrough(String streamId,long throughAt,int limit) {
        String stream=streamId==null?"":streamId.trim();if(stream.isEmpty())return Collections.emptyList();
        int safe=Math.max(1,Math.min(50,limit));List<RawObservation> out=new ArrayList<RawObservation>();
        Cursor c=getReadableDatabase().query("observations",columns(),"stream_id=? AND observed_at<=?",
                new String[]{stream,String.valueOf(throughAt)},null,null,"observed_at DESC,id DESC",Integer.toString(safe));
        try{while(c.moveToNext())out.add(read(c));}finally{c.close();}
        Collections.reverse(out);return out;
    }

    public synchronized LifeContextSnapshot rebuildContext(int observationLimit, long rebuiltAt) {
        return LifeContextAssembler.rebuild(recent(observationLimit), rebuiltAt);
    }

    public synchronized LifeContextSnapshot rebuildContext(int observationLimit) {
        return rebuildContext(observationLimit, System.currentTimeMillis());
    }

    /** Replays semantics from raw evidence, then rebuilds the Life Model. */
    public synchronized LifeModelSnapshot rebuildLifeModel(int observationLimit,
                                                            SemanticInterpreter interpreter,
                                                            long rebuiltAt) {
        List<RawObservation> evidence = recent(observationLimit);
        LifeContextSnapshot context = LifeContextAssembler.rebuild(evidence, rebuiltAt);
        List<SemanticAssertion> assertions = SemanticReplayEngine.replay(evidence, interpreter);
        return LifeModelAssembler.rebuild(assertions, context, rebuiltAt);
    }

    public synchronized LifeModelSnapshot rebuildLifeModel(int observationLimit,
                                                            SemanticInterpreter interpreter) {
        return rebuildLifeModel(observationLimit, interpreter, System.currentTimeMillis());
    }

    public synchronized int count() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM observations", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; }
        finally { c.close(); }
    }

    public synchronized void eraseAll() { getWritableDatabase().delete("observations", null, null); }

    private void trim() {
        getWritableDatabase().execSQL("DELETE FROM observations WHERE id NOT IN (SELECT id FROM observations ORDER BY id DESC LIMIT " + MAX_ROWS + ")");
    }

    private static String[] columns(){return new String[]{"observation_id","source_kind","source_package","stream_id","event_type","observed_at","text","raw_payload","attributes_json"};}
    private static RawObservation read(Cursor c){
        RawObservation.SourceKind kind;
        try { kind = RawObservation.SourceKind.valueOf(c.getString(1)); }
        catch (Exception e) { kind = RawObservation.SourceKind.OTHER; }
        return new RawObservation(c.getString(0),kind,c.getString(2),c.getString(3),c.getString(4),
                c.getLong(5),c.getString(6),c.getString(7),attributesMap(c.getString(8)));
    }

    private static String attributesJson(Map<String,String> values) {
        JSONObject o = new JSONObject();
        if (values != null) for (Map.Entry<String,String> e : values.entrySet()) {
            try { o.put(e.getKey(), e.getValue()); } catch (Exception ignored) {}
        }
        return o.toString();
    }

    private static java.util.Map<String,String> attributesMap(String json) {
        java.util.Map<String,String> out = new java.util.HashMap<String,String>();
        try {
            JSONObject o = new JSONObject(json == null ? "{}" : json);
            java.util.Iterator<String> keys = o.keys();
            while (keys.hasNext()) { String k = keys.next(); out.put(k, o.optString(k, "")); }
        } catch (Exception ignored) {}
        return out;
    }
}
