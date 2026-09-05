package com.kareem.lifeos;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class LifeOsApp extends Application {
    private static int resumedActivities;

    static synchronized boolean isAppForeground(){return resumedActivities>0;}

    @Override public void onCreate() {
        super.onCreate();
        // Provision the swappable local model without blocking startup, and immediately attempt to
        // drain durable semantic work left from a previous process/session.
        try{BackgroundModelManager.provisionAsync(this);}catch(Throwable ignored){}
        try{BackgroundBrain.poke(this);}catch(Throwable ignored){}

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity a,Bundle b){
                // Android 15+ lays targetSdk 35 apps edge-to-edge. Apply real device insets
                // globally so headers never sit under the clock, battery, camera cutout or nav bar.
                try{SystemBars.apply(a);}catch(Throwable ignored){}
            }
            @Override public void onActivityStarted(Activity a){}
            @Override public void onActivityResumed(Activity a){
                synchronized(LifeOsApp.class){resumedActivities++;}
                // Re-request insets after the activity has installed its content view. This also
                // handles rotation and system-bar mode changes without fixed device padding.
                try{SystemBars.apply(a);}catch(Throwable ignored){}

                // Background LiteRT-LM is the primary semantic engine. A resumed LifeOS surface
                // only gives the durable worker another opportunity to drain; it is not required
                // for understanding to begin.
                try{BackgroundBrain.poke(a);}catch(Throwable ignored){}

                // Gemini Nano/AICore is fallback-only while the background model is still being
                // provisioned. Never let both engines race over the same durable queue.
                if(!BackgroundModelManager.isReadyFast(a)){
                    try{NotificationBrain.analyzeForeground(a,null);}catch(Throwable ignored){}
                }
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
