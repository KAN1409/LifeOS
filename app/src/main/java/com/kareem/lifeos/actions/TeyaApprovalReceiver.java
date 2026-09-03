package com.kareem.lifeos.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.kareem.lifeos.context.ActionProposal;
import org.json.JSONObject;
import java.util.Collections;

/** Receives the minimal LifeOS approval/result broadcasts emitted by the pinned Teya Harness patch. */
public final class TeyaApprovalReceiver extends BroadcastReceiver {
    public static final String ACTION_REQUEST="com.kareem.lifeos.TEYA_APPROVAL_REQUEST";
    public static final String ACTION_RESULT="com.kareem.lifeos.TEYA_EXECUTION_RESULT";
    public static final String ACTION_EXECUTE="com.kareem.lifeos.TEYA_EXECUTE_APPROVED";
    public static final String EXTRA_ID="tool_call_id",EXTRA_NAME="tool_name",EXTRA_ARGS="tool_args",EXTRA_RESULT="tool_result",EXTRA_OK="tool_ok";

    @Override public void onReceive(Context context, Intent intent){
        if(intent==null)return;
        PersistentActionQueue queue=new PersistentActionQueue(context);
        if(ACTION_REQUEST.equals(intent.getAction())){
            String id=s(intent,EXTRA_ID),name=s(intent,EXTRA_NAME),args=s(intent,EXTRA_ARGS);
            if(id.isEmpty()||name.isEmpty())return;
            queue.enqueue(new ActionProposal(id,"",name,target(args),args,risk(name),id,Collections.emptyList()));
        }else if(ACTION_RESULT.equals(intent.getAction())){
            String id=s(intent,EXTRA_ID);if(id.isEmpty())return;
            queue.markExecuted(id,intent.getBooleanExtra(EXTRA_OK,false),s(intent,EXTRA_RESULT));
        }
    }

    public static void executeApproved(Context context,PersistentActionQueue.Item item){
        if(context==null||item==null)return;
        Intent i=new Intent();
        i.setClassName(context,"com.teya.agent.harness.HarnessService");
        i.setAction(ACTION_EXECUTE);
        i.putExtra(EXTRA_ID,item.proposal.proposalId);
        i.putExtra(EXTRA_NAME,item.proposal.actionType);
        i.putExtra(EXTRA_ARGS,item.proposal.payloadSummary);
        context.startForegroundService(i);
    }

    private static String target(String raw){
        try{JSONObject o=new JSONObject(raw);for(String k:new String[]{"name","title","item","items","label"}){String v=o.optString(k,"").trim();if(!v.isEmpty())return v;}}catch(Exception ignored){}
        return "";
    }
    private static ActionProposal.Risk risk(String n){
        if("place_call".equals(n))return ActionProposal.Risk.SENSITIVE;
        if(n.startsWith("cancel_")||n.startsWith("delete_")||"clear_shopping_list".equals(n))return ActionProposal.Risk.IRREVERSIBLE_WRITE;
        return ActionProposal.Risk.REVERSIBLE_WRITE;
    }
    private static String s(Intent i,String k){String v=i.getStringExtra(k);return v==null?"":v;}
}
