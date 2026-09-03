package com.kareem.lifeos.context;

import com.kareem.lifeos.engine.RawNode;
import com.kareem.lifeos.engine.RawScreenSnapshot;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ObservationAdaptersTest {
    @Test public void notificationAdapterPreservesSourceFacts() {
        NotificationCapture capture = new NotificationCapture(
                "key-1", "com.example.chat", "Ahmed", "Project A",
                "Please call me tomorrow", 1234L);
        RawObservation o = new NotificationObservationAdapter().adapt(capture);

        assertNotNull(o);
        assertEquals(RawObservation.SourceKind.NOTIFICATION, o.sourceKind);
        assertEquals("com.example.chat", o.sourcePackage);
        assertEquals("com.example.chat|project a", o.streamId);
        assertEquals("POSTED", o.eventType);
        assertEquals("Please call me tomorrow", o.text);
        assertEquals("Ahmed", o.attributes.get("title"));
        assertEquals("Project A", o.attributes.get("conversation_title"));
    }

    @Test public void accessibilityAdapterPreservesRawTreePayload() {
        RawNode node = new RawNode(1,-1,0,"android.widget.TextView","id/title","Hello","",0,0,100,40,false,false,false);
        RawScreenSnapshot snapshot = new RawScreenSnapshot(
                "com.example.app", 2222L, 1080, 2400, Arrays.asList(node));
        RawObservation o = new AccessibilityObservationAdapter().adapt(snapshot);

        assertNotNull(o);
        assertEquals(RawObservation.SourceKind.ACCESSIBILITY, o.sourceKind);
        assertEquals("TREE_SNAPSHOT", o.eventType);
        assertEquals("com.example.app", o.sourcePackage);
        assertTrue(o.rawPayload.contains("Hello"));
        assertEquals("1", o.attributes.get("node_count"));
    }

    @Test public void rawObservationDefensivelyCopiesAttributes() {
        java.util.HashMap<String,String> attrs = new java.util.HashMap<String,String>();
        attrs.put("x", "before");
        RawObservation o = new RawObservation("id", RawObservation.SourceKind.OTHER,
                "pkg", "stream", "type", 1L, "text", "payload", attrs);
        attrs.put("x", "after");
        assertEquals("before", o.attributes.get("x"));
        try {
            o.attributes.put("y", "no");
            fail("attributes must be immutable");
        } catch (UnsupportedOperationException expected) {}
    }
}
