package com.kareem.lifeos;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Validates the real Android revoked-permission state after the app process has been killed
 * by external `pm revoke` commands. The normal connected suite skips this test; CI invokes
 * it in a fresh instrumentation process with -e permissionPhase revoked.
 */
@RunWith(AndroidJUnit4.class)
public final class PermissionRevokedStateInstrumentationTest {
    @Test public void providersAndSurfacesRecoverAfterExternalPermissionRevocation() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        assumeTrue("revoked".equals(args.getString("permissionPhase")));

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context target = instrumentation.getTargetContext();

        assertDenied(target, Manifest.permission.READ_CONTACTS);
        assertDenied(target, Manifest.permission.READ_CALENDAR);
        assertDenied(target, Manifest.permission.ACCESS_FINE_LOCATION);
        assertDenied(target, Manifest.permission.ACCESS_COARSE_LOCATION);
        assertDenied(target, Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 33) {
            assertDenied(target, Manifest.permission.POST_NOTIFICATIONS);
        }

        assertEquals(ContactPersonRepository.Availability.SETUP_REQUIRED,
                ContactPersonRepository.availability(target));
        assertEquals(CalendarEventRepository.Availability.SETUP_REQUIRED,
                CalendarEventRepository.availability(target));
        assertEquals(PlaceRepository.Availability.SETUP_REQUIRED,
                PlaceRepository.availability(target));

        launch(instrumentation, new Intent(target, CapabilityActivity.class)
                .putExtra("capability", "people"));
        launch(instrumentation, new Intent(target, CapabilityActivity.class)
                .putExtra("capability", "events"));
        launch(instrumentation, new Intent(target, CapabilityActivity.class)
                .putExtra("capability", "places"));
        launch(instrumentation, new Intent(target, VoiceRecorderActivity.class));
        launch(instrumentation, new Intent(target, SystemStatusActivity.class));
    }

    private static void assertDenied(Context c, String permission) {
        assertEquals("Expected permission to remain revoked after process restart: " + permission,
                PackageManager.PERMISSION_DENIED, c.checkSelfPermission(permission));
    }

    private static void launch(Instrumentation instrumentation, Intent intent) throws Exception {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity a = instrumentation.startActivitySync(intent);
        assertNotNull("Activity failed after permission revocation: " + intent, a);
        instrumentation.waitForIdleSync();
        assertFalse("Activity immediately finished after permission revocation: " + intent,
                a.isFinishing());
        a.runOnUiThread(a::finish);
        instrumentation.waitForIdleSync();
    }
}
