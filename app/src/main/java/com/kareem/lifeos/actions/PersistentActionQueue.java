package com.kareem.lifeos.actions;

import android.content.Context;
import android.content.SharedPreferences;
import com.kareem.lifeos.context.ActionApproval;
import com.kareem.lifeos.context.ActionProposal;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent integration boundary for grounded action proposals, approvals and execution results. */
public final class PersistentActionQueue {
    private static final String PREFS="lifeos_action_queue", KEY="items";
    private final SharedPreferences prefs;
    public PersistentActionQueue(Context c){prefs=c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    public synchronized void enqueue(ActionProposal p){if(p==null||p.proposalId.isEmpty())return;List<Item> xs=all();for(Item x:xs)if(x.proposal.proposalId.equals(p.proposalId))return;xs.add(new Item(p,null,"pending",""));save(xs);}
    public synchronized List<Item> pending(){List<Item> out=new ArrayList<>();for(Item x:all())if("pending".equals(x.state))out.add(x);return out;}
    public synchronized List<Item> history(){List<Item> xs=all();Collections.reverse(xs);return xs;}
    public synchronized void decide(String id,boolean approved){List<Item> xs=all();long now=System.currentTimeMillis();for(int i=0;i<xs.size();i++){Item x=xs.get(i);if(x.proposal.proposalId.equals(id)&&"pending".equals(x.state)){xs.set(i,new Item(x.proposal,new ActionApproval(id,approved,now),approved?"approved":"denied",x.result));break;}}save(xs);}
    public synchronized void markExecuted(String id,boolean ok,String result){List<Item> xs=all();for(int i=0;i<xs.size();i++){Item x=xs.get(i);if(x.proposal.proposalId.equals(id)){xs.set(i,new Item(x.proposal,x.approval,ok?"executed":"failed",result==null?"":result));break;}}save(xs);}

    private List<Item> all(){ArrayList<Item> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString(KEY,"[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);ActionProposal p=new ActionProposal(o.optString("id"),o.optString("situation"),o.optString("type"),o.optString("target"),o.optString("summary"),risk(o.optString("risk")),o.optString("key"),strings(o.optJSONArray("evidence")));String state=o.optString("state","pending");ActionApproval ap=null;if(o.has("approvedAt"))ap=new ActionApproval(p.proposalId,o.optBoolean("approved"),o.optLong("approvedAt"));out.add(new Item(p,ap,state,o.optString("result","")));}}catch(Exception ignored){}return out;}
    private void save(List<Item> xs){JSONArray a=new JSONArray();try{for(Item x:xs){JSONObject o=new JSONObject();o.put("id",x.proposal.proposalId);o.put("situation",x.proposal.situationId);o.put("type",x.proposal.actionType);o.put("target",x.proposal.target);o.put("summary",x.proposal.payloadSummary);o.put("risk",x.proposal.risk.name());o.put("key",x.proposal.idempotencyKey);o.put("evidence",new JSONArray(x.proposal.evidenceIds));o.put("state",x.state);o.put("result",x.result);if(x.approval!=null){o.put("approved",x.approval.approved);o.put("approvedAt",x.approval.approvedAt);}a.put(o);}}catch(Exception ignored){}prefs.edit().putString(KEY,a.toString()).apply();}
    private static ActionProposal.Risk risk(String s){try{return ActionProposal.Risk.valueOf(s);}catch(Exception e){return ActionProposal.Risk.SENSITIVE;}}
    private static List<String> strings(JSONArray a){ArrayList<String> out=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++){String s=a.optString(i,"");if(!s.isEmpty())out.add(s);}return out;}

    public static final class Item{public final ActionProposal proposal;public final ActionApproval approval;public final String state;public final String result;Item(ActionProposal p,ActionApproval a,String s,String r){proposal=p;approval=a;state=s;result=r==null?"":r;}}
}
