package com.kareem.lifeos.context;

import com.kareem.lifeos.engine.RawEvidenceSerializer;
import com.kareem.lifeos.engine.RawScreenSnapshot;
import java.util.HashMap;
import java.util.Map;

/** Preserves complete accessibility-tree snapshots before conversation-specific filtering. */
public final class AccessibilityObservationAdapter implements ObservationAdapter<RawScreenSnapshot> {
    @Override public RawObservation adapt(RawScreenSnapshot snapshot) {
        if (snapshot == null) return null;
        Map<String,String> attrs = new HashMap<String,String>();
        attrs.put("screen_width", Integer.toString(snapshot.screenWidth));
        attrs.put("screen_height", Integer.toString(snapshot.screenHeight));
        attrs.put("node_count", Integer.toString(snapshot.nodes.size()));
        return new RawObservation(
                "accessibility|" + snapshot.packageName + "|" + snapshot.capturedAt,
                RawObservation.SourceKind.ACCESSIBILITY,
                snapshot.packageName,
                snapshot.packageName,
                "TREE_SNAPSHOT",
                snapshot.capturedAt,
                "",
                RawEvidenceSerializer.snapshot(snapshot),
                attrs);
    }
}
