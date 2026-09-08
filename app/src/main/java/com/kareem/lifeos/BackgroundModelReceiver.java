package com.kareem.lifeos;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restarts semantic draining as soon as the durable model download finishes. */
public final class BackgroundModelReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(context==null||intent==null||!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))return;
        long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,0L);
        if(!BackgroundModelManager.isOurDownload(context,id))return;
        BackgroundBrain.poke(context);
    }
}
