package com.kareem.lifeos.context;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Crash-safe idempotency ledger. Reservations survive app/process restarts. */
public final class PersistentActionLedger extends SQLiteOpenHelper implements ActionLedger {
    private static final String DB_NAME = "lifeos_actions_v2.db";
    private static final int DB_VERSION = 1;
    private static volatile PersistentActionLedger instance;

    private PersistentActionLedger(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static PersistentActionLedger get(Context context) {
        if (instance == null) {
            synchronized (PersistentActionLedger.class) {
                if (instance == null) instance = new PersistentActionLedger(context);
            }
        }
        return instance;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE action_reservations (" +
                "idempotency_key TEXT PRIMARY KEY," +
                "reserved_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX action_reservations_time_idx ON action_reservations(reserved_at DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override public synchronized boolean reserve(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) return false;
        ContentValues values = new ContentValues();
        values.put("idempotency_key", idempotencyKey.trim());
        values.put("reserved_at", System.currentTimeMillis());
        long row = getWritableDatabase().insertWithOnConflict(
                "action_reservations", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return row > 0;
    }
}
