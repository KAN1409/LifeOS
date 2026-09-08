package com.kareem.lifeos.context;

/** Source DTO containing notification facts before semantic interpretation. */
public final class NotificationCapture {
    public final String key;
    public final String packageName;
    public final String title;
    public final String conversationTitle;
    public final String sender;
    public final String text;
    public final String category;
    public final String channelId;
    public final boolean groupConversation;
    public final boolean ongoing;
    public final long postedAt;

    /** Backward-compatible constructor for existing adapters/tests. */
    public NotificationCapture(String key, String packageName, String title,
                               String conversationTitle, String text, long postedAt) {
        this(key, packageName, title, conversationTitle, "", text, "", "", false, false, postedAt);
    }

    public NotificationCapture(String key, String packageName, String title,
                               String conversationTitle, String sender, String text,
                               String category, String channelId,
                               boolean groupConversation, boolean ongoing, long postedAt) {
        this.key = safe(key);
        this.packageName = safe(packageName);
        this.title = safe(title);
        this.conversationTitle = safe(conversationTitle);
        this.sender = safe(sender);
        this.text = safe(text);
        this.category = safe(category);
        this.channelId = safe(channelId);
        this.groupConversation = groupConversation;
        this.ongoing = ongoing;
        this.postedAt = postedAt;
    }

    /** Best structural identity available without interpreting message text. */
    public String streamLabel() {
        if (!conversationTitle.isEmpty()) return conversationTitle;
        if (!sender.isEmpty()) return sender;
        return title;
    }

    private static String safe(String v) { return v == null ? "" : v; }
}
