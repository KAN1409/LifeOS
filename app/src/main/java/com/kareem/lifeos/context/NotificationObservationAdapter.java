package com.kareem.lifeos.context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Converts notification capture facts into the source-neutral V2 contract. */
public final class NotificationObservationAdapter implements ObservationAdapter<NotificationCapture> {
    @Override public RawObservation adapt(NotificationCapture n) {
        if (n == null) return null;
        String stream = n.packageName + "|" + n.streamLabel().toLowerCase(Locale.ROOT).trim();
        Map<String,String> attrs = new HashMap<String,String>();
        attrs.put("title", n.title);
        attrs.put("conversation_title", n.conversationTitle);
        attrs.put("sender", n.sender);
        attrs.put("category", n.category);
        attrs.put("channel_id", n.channelId);
        attrs.put("group_conversation", Boolean.toString(n.groupConversation));
        attrs.put("ongoing", Boolean.toString(n.ongoing));
        attrs.put("structured_message", Boolean.toString(!n.sender.isEmpty() || !n.conversationTitle.isEmpty()));
        return new RawObservation(
                "notification|" + n.key,
                RawObservation.SourceKind.NOTIFICATION,
                n.packageName,
                stream,
                "POSTED",
                n.postedAt,
                n.text,
                "",
                attrs);
    }
}
