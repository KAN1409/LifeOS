package com.kareem.lifeos;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/**
 * Keeps LifeOS content out from under status/navigation bars and display cutouts.
 * Android 15+ enforces edge-to-edge for targetSdk 35, so every activity must consume
 * real runtime insets instead of assuming a fixed status-bar height.
 */
final class SystemBars {
    private SystemBars() {}

    static void apply(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        // Keep system chrome visually continuous with LifeOS' dark shell.
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);

        View decor = window.getDecorView();
        decor.setOnApplyWindowInsetsListener((view, insets) -> {
            View content = activity.findViewById(android.R.id.content);
            if (content != null) {
                int left, top, right, bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(
                            WindowInsets.Type.statusBars()
                                    | WindowInsets.Type.navigationBars()
                                    | WindowInsets.Type.displayCutout());
                    left = bars.left;
                    top = bars.top;
                    right = bars.right;
                    bottom = bars.bottom;
                } else {
                    left = insets.getSystemWindowInsetLeft();
                    top = insets.getSystemWindowInsetTop();
                    right = insets.getSystemWindowInsetRight();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                content.setPadding(left, top, right, bottom);
            }
            // Do not consume the insets: child widgets such as scrolling containers may still
            // need IME or gesture information of their own.
            return insets;
        });
        decor.requestApplyInsets();
    }
}
