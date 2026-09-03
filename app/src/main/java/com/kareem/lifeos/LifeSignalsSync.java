package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.graphiti.GraphitiGateway;
import java.time.Instant;
import java.util.List;

/** LifeOS-to-donor adapter. Intelligence remains in Graphiti; decision shape follows Log4brains ADR. */
final class LifeSignalsSync {
    private LifeSignalsSync(){}

    static int syncRecentInteractions(Context context,int limit) throws Exception{
        GraphitiGateway api=new GraphitiGateway(context);
        List<LifeDb.Event> events=new LifeDb(context).recentEvents(limit);
        int sent=0;
        for(LifeDb.Event e:events){
            if(e.body==null||e.body.trim().isEmpty())continue;
            String role=(e.title==null||e.title.trim().isEmpty())?"interaction":e.title.trim();
            String source=(e.app==null?"":e.app)+(e.threadKey==null||e.threadKey.isEmpty()?"":" · "+e.threadKey);
            api.addMessage(GraphitiGateway.SOCIAL_GROUP,"lifeos-event-"+e.id,role,"user",role,e.body,Instant.ofEpochMilli(e.at).toString(),source);
            sent++;
        }
        return sent;
    }

    static void recordDecision(Context context,String title,String status,String deciders,String decisionContext,String options,String outcome,String consequences) throws Exception{
        StringBuilder adr=new StringBuilder();
        adr.append("# ").append(clean(title)).append("\n\n");
        adr.append("- Status: ").append(empty(status)?"accepted":clean(status)).append("\n");
        if(!empty(deciders))adr.append("- Deciders: ").append(clean(deciders)).append("\n");
        adr.append("- Date: ").append(Instant.now().toString()).append("\n\n");
        adr.append("## Context and Problem Statement\n\n").append(clean(decisionContext)).append("\n\n");
        adr.append("## Considered Options\n\n").append(clean(options)).append("\n\n");
        adr.append("## Decision Outcome\n\n").append(clean(outcome)).append("\n\n");
        adr.append("### Consequences\n\n").append(clean(consequences)).append("\n");
        new GraphitiGateway(context).addMessage(GraphitiGateway.DECISION_GROUP,"lifeos-decision-"+System.currentTimeMillis(),clean(title),"user","decision",adr.toString(),Instant.now().toString(),"LifeOS Decision Memory · Log4brains ADR contract");
    }

    private static boolean empty(String s){return s==null||s.trim().isEmpty();}
    private static String clean(String s){return s==null?"":s.trim();}
}
