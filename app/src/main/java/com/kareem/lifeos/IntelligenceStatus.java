package com.kareem.lifeos;

import android.content.Context;

/** Compact snapshot for Now/pull-to-refresh. Counts are durable, not inferred from UI state. */
final class IntelligenceStatus {
    static final class Snapshot {
        final int ready,pending,open,confirmed,provisional;
        Snapshot(int ready,int pending,int open,int confirmed){
            this.ready=Math.max(0,ready);this.pending=Math.max(0,pending);this.open=Math.max(0,open);
            this.confirmed=Math.max(0,confirmed);this.provisional=Math.max(0,open-confirmed);
        }
        boolean upToDate(){return pending==0;}
        String line(){
            if(pending==0){
                String extra=provisional>0?" · "+provisional+" provisional attention":"";
                return "✓ Intelligence ready · "+ready+" analyzed · 0 pending"+extra;
            }
            String extra=provisional>0?" · "+provisional+" provisional":"";
            return ready+" ready · "+pending+" queued / processing"+extra;
        }
    }

    private IntelligenceStatus(){}

    static Snapshot snapshot(Context context){
        AttentionStore a=AttentionStore.get(context);
        return new Snapshot(NotificationMeaningStore.get(context).count(),a.pendingCount(),a.openCount(),a.confirmedOpenCount());
    }
}
