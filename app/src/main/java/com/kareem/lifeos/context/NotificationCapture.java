package com.kareem.lifeos.context;

/** Source DTO containing notification facts before semantic interpretation. */
public final class NotificationCapture {
    public final String key;
    public final String packageName;
    public final String title;
    public final String conversationTitle;
    public final String text;
    public final long postedAt;

    public NotificationCapture(String key, String packageName, String title,
                               String conversationTitle, String text, long postedAt) {
        this.key = safe(key);
        this.packageName = safe(packageName);
        this.title = safe(title);
        this.conversationTitle = safe(conversationTitle);
        this.text = safe(text);
        this.postedAt = postedAt;
    }

    private static String safe(String v) { return v == null ? "" : v; }
}
