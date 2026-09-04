package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/** Opens the strongest destination LifeOS can still prove for one piece of evidence. */
final class SourceOpener {
    static final int NONE=0,APP=1,CONVERSATION=2,EXACT=3;
    static final class Result {final boolean opened;final int precision;final String message;Result(boolean opened,int precision,String message){this.opened=opened;this.precision=precision;this.message=message;}}
    private SourceOpener(){}

    static String buttonLabel(Activity a,long eventId,LifeDb.Event event){String app=event==null?"source":LifeDb.friendlyApp(event.app);SourceLocatorStore.Locator l=SourceLocatorStore.get(a).forEvent(eventId);return l!=null&&!l.notificationKey.isEmpty()?"Open original in "+app:"Open "+app;}

    static Result open(Activity a,long eventId,LifeDb.Event event){
        if(event==null)return new Result(false,NONE,"The original source is no longer available.");
        SourceLocatorStore.Locator l=SourceLocatorStore.get(a).forEvent(eventId);
        if(l!=null&&!l.notificationKey.isEmpty()&&LifeNotificationListener.openActiveNotification(l.notificationKey))return new Result(true,EXACT,"Opened the original source.");
        String low=event.app==null?"":event.app.toLowerCase();
        if(low.contains("whatsapp")){
            String label=LifeDb.personLabel(event),digits=label.replaceAll("[^0-9+]","");
            if(digits.matches("\\+?[0-9]{8,}"))try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+digits.replace("+",""))));return new Result(true,CONVERSATION,"Opened the WhatsApp conversation.");}catch(Throwable ignored){}
        }
        try{Intent launch=a.getPackageManager().getLaunchIntentForPackage(event.app);if(launch!=null){launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);a.startActivity(launch);return new Result(true,APP,"Opened "+LifeDb.friendlyApp(event.app)+". The exact item is no longer directly addressable.");}}catch(Throwable ignored){}
        return new Result(false,NONE,"The exact source is no longer directly addressable. Your captured copy is preserved here.");
    }
}
