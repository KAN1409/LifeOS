package com.kareem.lifeos.engine;

import java.util.Locale;

/** Derives a sensor-independent thread key when the screen exposes one. */
public final class ConversationIdentityExtractor {
    private ConversationIdentityExtractor(){}
    public static String fromSnapshot(RawScreenSnapshot snapshot){
        if(snapshot==null)return "";
        if(WhatsAppNodeSemantics.applies(snapshot))for(RawNode n:snapshot.nodes)if(WhatsAppNodeSemantics.isConversationTitle(n)){
            String title=value(n);if(!title.isEmpty())return snapshot.packageName+"|"+title.toLowerCase(Locale.ROOT).trim();
        }
        return "";
    }
    private static String value(RawNode n){return !n.text.trim().isEmpty()?n.text.trim():n.contentDescription.trim();}
}
