package com.kareem.lifeos;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class LifeNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn){if(sbn==null)return;Notification n=sbn.getNotification();if(n==null)return;Bundle x=n.extras;String title=x==null?"":String.valueOf(x.getCharSequence(Notification.EXTRA_TITLE,""));String body=x==null?"":String.valueOf(x.getCharSequence(Notification.EXTRA_TEXT,""));if(body.trim().length()==0)return;LifeDb db=new LifeDb(this);try{db.addObservation("NOTIFICATION",sbn.getPackageName(),title,body,"IN",sbn.getPostTime(),0.86);}finally{db.close();}}
}
