package com.kareem.lifeos.context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Converts notification capture facts into the source-neutral V2 contract. */
public final class NotificationObservationAdapter implements ObservationAdapter<NotificationCapture> {
    @Override public RawObservation adapt(NotificationCapture n) {
        if (n == null) return null;
        String identity = structuralIdentity(n);
        String stream = n.packageName + "|" + identity.toLowerCase(Locale.ROOT).trim();
        Map<String,String> attrs = new HashMap<String,String>();
        attrs.put("title", n.title);
        attrs.put("conversation_title", n.conversationTitle);
        attrs.put("sender", n.sender);
        attrs.put("resolved_stream_label", identity);
        attrs.put("category", n.category);
        attrs.put("channel_id", n.channelId);
        attrs.put("group_conversation", Boolean.toString(n.groupConversation));
        attrs.put("ongoing", Boolean.toString(n.ongoing));
        attrs.put("structured_message", Boolean.toString(!n.sender.isEmpty() || !n.conversationTitle.isEmpty() || inferredSender(n).length()>0));
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

    /**
     * Keep the raw V2 stream identity aligned with the legacy/current conversation identity.
     * Some messaging apps expose a generic title ("WhatsApp") and encode the sender as
     * "Ahmed: message". Treating those as one giant WhatsApp stream mixes unrelated people.
     */
    private static String structuralIdentity(NotificationCapture n){
        if(!n.conversationTitle.trim().isEmpty())return n.conversationTitle.trim();
        if(!n.sender.trim().isEmpty())return n.sender.trim();
        String inferred=inferredSender(n);if(!inferred.isEmpty())return inferred;
        String label=n.streamLabel().trim();return label.isEmpty()?"unknown":label;
    }

    private static String inferredSender(NotificationCapture n){
        String app=n.packageName.toLowerCase(Locale.ROOT),title=n.title.trim().toLowerCase(Locale.ROOT);
        boolean messaging=app.contains("whatsapp")||app.contains("telegram")||app.contains("facebook.orca")||app.contains("messenger")||app.contains("signal")||app.contains("android.apps.messaging");
        if(!messaging)return "";
        boolean generic=title.isEmpty()||title.equals("whatsapp")||title.equals("telegram")||title.equals("messenger")||title.equals("signal")||title.equals("messages")||title.contains("new message");
        if(!generic)return "";
        int colon=n.text.indexOf(':');if(colon<=1||colon>=80)return "";
        String candidate=n.text.substring(0,colon).trim();
        return candidate.matches(".*[\\p{L}].*")?candidate:"";
    }
}
