package com.kareem.lifeos;

import android.content.Context;
import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import java.io.File;

/** Process-local LiteRT-LM runtime shared by background semantics and grounded Ask LifeOS. */
final class BackgroundSemanticRuntime {
    private static Engine engine;private static String loadedPath="";
    private BackgroundSemanticRuntime(){}
    static synchronized String generate(Context context,String modelPath,String prompt)throws Exception{return run(context,modelPath,"You are LifeOS background semantic classification. Return only the requested JSON. Never invent facts, people, dates, urgency, actions, outcomes, or obligations.\n"+prompt);}
    static synchronized String generateFreeform(Context context,String modelPath,String prompt)throws Exception{return run(context,modelPath,prompt);}
    private static String run(Context context,String modelPath,String grounded)throws Exception{Context app=context.getApplicationContext();if(engine==null||!modelPath.equals(loadedPath)){reset();File cache=new File(app.getCacheDir(),"litertlm-background");cache.mkdirs();EngineConfig config=new EngineConfig(modelPath,new Backend.CPU(null,null),null,null,null,null,cache.getAbsolutePath());Engine fresh=new Engine(config);fresh.initialize();engine=fresh;loadedPath=modelPath;}Conversation conversation=null;try{conversation=engine.createConversation(new ConversationConfig());Message response=conversation.sendMessage(grounded);return response==null?"":response.toString().trim();}finally{if(conversation!=null)try{conversation.close();}catch(Throwable ignored){}}}
    static synchronized void reset(){if(engine!=null)try{engine.close();}catch(Throwable ignored){}engine=null;loadedPath="";}
}
