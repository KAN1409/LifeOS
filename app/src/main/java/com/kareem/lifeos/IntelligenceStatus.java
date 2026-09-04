package com.kareem.lifeos;

import android.content.Context;

/** Compact snapshot for Now/pull-to-refresh. Counts are durable, not inferred from UI state. */
final class IntelligenceStatus {
    static final class Snapshot {
        final int ready,pending,open,confirmed,provisional,modelProgress;
        final String modelState;
        final boolean backgroundRunning;
        Snapshot(int ready,int pending,int open,int confirmed,String modelState,int modelProgress,boolean backgroundRunning){
            this.ready=Math.max(0,ready);this.pending=Math.max(0,pending);this.open=Math.max(0,open);
            this.confirmed=Math.max(0,confirmed);this.provisional=Math.max(0,open-confirmed);
            this.modelState=modelState==null?"unknown":modelState;this.modelProgress=Math.max(0,Math.min(100,modelProgress));
            this.backgroundRunning=backgroundRunning;
        }
        boolean upToDate(){return pending==0;}
        boolean modelReady(){return "ready".equals(modelState);}
        String line(){
            if("downloading".equals(modelState)||"queued_download".equals(modelState)||"verifying".equals(modelState)){
                String p=modelProgress>0?" "+modelProgress+"%":"";
                return "Background brain "+("verifying".equals(modelState)?"verifying":"downloading")+p+" · "+pending+" safely queued";
            }
            if("download_failed".equals(modelState)||"download_paused".equals(modelState)||"unavailable".equals(modelState)){
                return "Background brain "+modelState.replace('_',' ')+" · "+pending+" safely queued";
            }
            if(!modelReady()){
                return "Preparing background brain · "+pending+" safely queued";
            }
            if(pending==0){
                String extra=provisional>0?" · "+provisional+" provisional attention":"";
                return "✓ Intelligence ready · "+ready+" analyzed · 0 pending"+extra;
            }
            String state=backgroundRunning?"processing in background":"safely queued";
            String extra=provisional>0?" · "+provisional+" provisional":"";
            return ready+" ready · "+pending+" "+state+extra;
        }
    }

    private IntelligenceStatus(){}

    static Snapshot snapshot(Context context){
        AttentionStore a=AttentionStore.get(context);BackgroundModelManager.Status model=BackgroundModelManager.status(context);
        return new Snapshot(NotificationMeaningStore.get(context).count(),a.pendingCount(),a.openCount(),a.confirmedOpenCount(),model.state,model.progress,BackgroundBrain.isRunning());
    }
}
