package com.kareem.lifeos;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class LifeOsApp extends Application {
    private static int resumedActivities;

    static synchronized boolean isAppForeground(){return resumedActivities>0;}

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity a,Bundle b){}
            @Override public void onActivityStarted(Activity a){}
            @Override public void onActivityResumed(Activity a){
                synchronized(LifeOsApp.class){resumedActivities++;}
                // Deep AICore inference is legal only while LifeOS is genuinely foreground.
                // Trigger from every resumed LifeOS surface so Now never owns the brain lifecycle.
                try{NotificationBrain.analyzeForeground(a,null);}catch(Throwable ignored){}
            }
            @Override public void onActivityPaused(Activity a){synchronized(LifeOsApp.class){resumedActivities=Math.max(0,resumedActivities-1);}}
            @Override public void onActivityStopped(Activity a){}
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
            @Override public void onActivityDestroyed(Activity a){}
        });
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
