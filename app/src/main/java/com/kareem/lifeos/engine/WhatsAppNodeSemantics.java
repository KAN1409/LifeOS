package com.kareem.lifeos.engine;

/** Stable structural roles exposed by WhatsApp accessibility view IDs. */
final class WhatsAppNodeSemantics {
    private WhatsAppNodeSemantics(){}
    static boolean applies(RawScreenSnapshot s){return s!=null&&"com.whatsapp".equals(s.packageName);}
    static boolean isMessageBody(RawNode n){return id(n,"message_text");}
    static boolean isMetadata(RawNode n){return id(n,"date")||id(n,"status");}
    static boolean isComposer(RawNode n){return id(n,"entry");}
    static boolean isComposerAction(RawNode n){return id(n,"voice_note_btn")||id(n,"emoji_picker_btn")||id(n,"input_attach_button")||id(n,"camera_btn");}
    static boolean isConversationTitle(RawNode n){return id(n,"conversation_contact_name");}
    private static boolean id(RawNode n,String suffix){return n!=null&&n.viewId.endsWith(":id/"+suffix);}
}
