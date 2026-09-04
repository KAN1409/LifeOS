package com.kareem.lifeos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/** Applies one grounded semantic meaning to the exact evidence item that produced it. */
final class NotificationMeaningApplier {
    private NotificationMeaningApplier(){}

    static boolean apply(Context context,LifeDb db,NotificationMeaning meaning,long eventId,long sourceAt){
        if(context==null||db==null||meaning==null||eventId<=0)return false;
        LifeDb.Event source=db.eventById(eventId);AttentionStore attention=AttentionStore.get(context);
        boolean compatible=meaning.needsAttention()&&attentionCompatible(source,meaning);

        if(meaning.canSummarize()&&source!=null)LocalGroundedMemory.materializeMeaning(context,source,meaning);
        if(compatible)attention.applyModel(meaning,eventId,sourceAt);else attention.rejectProvisional(meaning,eventId,sourceAt);

        if(!meaning.canSummarize())return false;
        SQLiteDatabase sql=db.getWritableDatabase();
        sql.execSQL("UPDATE open_loops SET status='superseded' WHERE status='open' AND evidence_id=?",new Object[]{eventId});
        AttentionStore.Item current=attention.forEvent(eventId);
        if(current!=null&&AttentionStore.HANDLED.equals(current.status))return false;
        if(!compatible)return false;

        String kind=meaning.loopKind();String fingerprint="brain|"+meaning.sourceObservationId;long now=System.currentTimeMillis();
        sql.execSQL("INSERT OR IGNORE INTO open_loops(evidence_id,fingerprint,kind,title,due_at,confidence,priority,status,created_at) VALUES(?,?,?,?,0,?,?, 'open',?)",
                new Object[]{eventId,fingerprint,kind,meaning.summary,meaning.confidence,meaning.priority(),now});
        sql.execSQL("UPDATE open_loops SET kind=?,title=?,confidence=?,priority=?,status='open' WHERE fingerprint=?",
                new Object[]{kind,meaning.summary,meaning.confidence,meaning.priority(),fingerprint});
        return true;
    }

    private static boolean attentionCompatible(LifeDb.Event source,NotificationMeaning meaning){
        if(source==null||meaning==null)return false;
        if("PERSON_CONVERSATION".equals(meaning.type))return EventSemantics.isPersonConversation(source);
        if("CONTENT_READY".equals(meaning.type)||"PROMOTION".equals(meaning.type)||"SYSTEM_EVENT".equals(meaning.type)||"OTHER".equals(meaning.type))return false;
        return !EventSemantics.isPersonConversation(source);
    }
}
