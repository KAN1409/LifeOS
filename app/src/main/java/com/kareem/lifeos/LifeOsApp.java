package com.kareem.lifeos;

import android.app.Application;

public final class LifeOsApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ExperienceAudit.install(this);
    }
}
