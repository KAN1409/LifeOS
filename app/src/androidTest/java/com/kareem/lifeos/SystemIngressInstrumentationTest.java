package com.kareem.lifeos;

import android.content.Context;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kareem.lifeos.context.RawObservation;
import com.kareem.lifeos.context.UniversalObservationStore;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Verifies Android itself can drive LifeOS ingress services. CI enables the real system service,
 * generates evidence outside LifeOS, then invokes only the matching verification phase.
 */
@RunWith(AndroidJUnit4.class)
public final class SystemIngressInstrumentationTest {
    @Test public void verifyNotificationListenerReceivedExternalSystemNotification(){
        assumeTrue("notification".equals(arg("phase")));String token=arg("token");assertFalse("CI did not provide notification token",token.isEmpty());
        Context c=context();boolean found=false;List<RawObservation> xs=UniversalObservationStore.get(c).recent(2000);
        for(RawObservation o:xs){if(o!=null&&o.sourceKind==RawObservation.SourceKind.NOTIFICATION&&(o.text.contains(token)||o.rawPayload.contains(token))){found=true;break;}}
        assertTrue("Android NotificationListenerService never delivered the external notification token to LifeOS",found);
    }

    @Test public void verifyAccessibilityServiceReceivedExternalAppTree(){
        assumeTrue("accessibility".equals(arg("phase")));Context c=context();boolean found=false;List<RawObservation> xs=UniversalObservationStore.get(c).recent(2000);
        for(RawObservation o:xs){if(o!=null&&o.sourceKind==RawObservation.SourceKind.ACCESSIBILITY&&"com.android.settings".equals(o.sourcePackage)){found=true;break;}}
        assertTrue("Android AccessibilityService never delivered a Settings UI tree to LifeOS",found);
    }

    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static String arg(String key){Bundle b=InstrumentationRegistry.getArguments();return b==null?"":b.getString(key,"");}
}
