package com.kareem.lifeos;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Tests the app in both denied and granted runtime-permission states without relying on UI dialogs. */
@RunWith(AndroidJUnit4.class)
public final class PermissionMatrixInstrumentationTest {
    private Instrumentation instrumentation; private Context target;
    @Before public void setup(){instrumentation=InstrumentationRegistry.getInstrumentation();target=instrumentation.getTargetContext();}

    @Test public void contactsProviderSurvivesGrantAndRevoke() throws Exception {
        revoke(Manifest.permission.READ_CONTACTS);
        assertEquals(ContactPersonRepository.Availability.SETUP_REQUIRED,ContactPersonRepository.availability(target));
        launchCapability("people");
        grant(Manifest.permission.READ_CONTACTS);
        assertEquals(ContactPersonRepository.Availability.OPERATIONAL,ContactPersonRepository.availability(target));
        ContactPersonRepository.count(target);
        launchCapability("people");
        revoke(Manifest.permission.READ_CONTACTS);
        assertEquals(ContactPersonRepository.Availability.SETUP_REQUIRED,ContactPersonRepository.availability(target));
    }

    @Test public void calendarProviderSurvivesGrantAndRevoke() throws Exception {
        revoke(Manifest.permission.READ_CALENDAR);
        assertEquals(CalendarEventRepository.Availability.SETUP_REQUIRED,CalendarEventRepository.availability(target));
        launchCapability("events");
        grant(Manifest.permission.READ_CALENDAR);
        assertEquals(CalendarEventRepository.Availability.OPERATIONAL,CalendarEventRepository.availability(target));
        CalendarEventRepository.recentAndUpcoming(target,20);
        launchCapability("events");
        revoke(Manifest.permission.READ_CALENDAR);
        assertEquals(CalendarEventRepository.Availability.SETUP_REQUIRED,CalendarEventRepository.availability(target));
    }

    @Test public void locationAndMicrophoneDenialPathsDoNotCrash() throws Exception {
        revoke(Manifest.permission.ACCESS_FINE_LOCATION);revoke(Manifest.permission.ACCESS_COARSE_LOCATION);
        assertEquals(PlaceRepository.Availability.SETUP_REQUIRED,PlaceRepository.availability(target));
        launchCapability("places");
        revoke(Manifest.permission.RECORD_AUDIO);
        launch(new Intent(target,VoiceRecorderActivity.class));
        grant(Manifest.permission.RECORD_AUDIO);
        launch(new Intent(target,VoiceRecorderActivity.class));
        revoke(Manifest.permission.RECORD_AUDIO);
    }

    @Test public void notificationPermissionStateIsHandledAcrossSupportedApis() throws Exception {
        if(Build.VERSION.SDK_INT<33)return;
        revoke(Manifest.permission.POST_NOTIFICATIONS);
        assertEquals(PackageManager.PERMISSION_DENIED,target.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS));
        launch(new Intent(target,SystemStatusActivity.class));
        grant(Manifest.permission.POST_NOTIFICATIONS);
        assertEquals(PackageManager.PERMISSION_GRANTED,target.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS));
        launch(new Intent(target,SystemStatusActivity.class));
        revoke(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void launchCapability(String id)throws Exception{launch(new Intent(target,CapabilityActivity.class).putExtra("capability",id));}
    private void launch(Intent intent)throws Exception{
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity a=instrumentation.startActivitySync(intent);assertNotNull(a);instrumentation.waitForIdleSync();assertFalse(a.isFinishing());
        a.runOnUiThread(a::finish);instrumentation.waitForIdleSync();
    }
    private void grant(String p){try{instrumentation.getUiAutomation().grantRuntimePermission(target.getPackageName(),p);}catch(SecurityException ignored){}}
    private void revoke(String p){try{instrumentation.getUiAutomation().revokeRuntimePermission(target.getPackageName(),p);}catch(SecurityException ignored){}}
}
