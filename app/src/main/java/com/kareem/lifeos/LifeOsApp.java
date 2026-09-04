package com.kareem.lifeos;

import android.app.Application;

public final class LifeOsApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ExperienceAudit.install(this);
        // Bounded, idempotent migration of recent high-confidence human evidence into
        // the on-device memory store. Never block application startup.
        new Thread(() -> {
            try (LifeDb db = new LifeDb(this)) {
                LocalGroundedMemory.backfill(this, db, 240, 40);
            } catch (Throwable ignored) {}
        }, "lifeos-memory-backfill").start();
    }
}
