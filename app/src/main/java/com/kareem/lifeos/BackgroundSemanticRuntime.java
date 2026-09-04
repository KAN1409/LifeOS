package com.kareem.lifeos;

import android.content.Context;
import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import java.io.File;

/**
 * Process-local LiteRT-LM runtime for LifeOS background semantics.
 *
 * This bridge is deliberately Java so LifeOS can consume the current LiteRT-LM runtime without
 * forcing the existing Teya/KSP Kotlin toolchain to move in lockstep with the runtime's compiler.
 */
final class BackgroundSemanticRuntime {
    private static Engine engine;
    private static String loadedPath="";

    private BackgroundSemanticRuntime(){}

    static synchronized String generate(Context context,String modelPath,String prompt)throws Exception{
        Context app=context.getApplicationContext();
        if(engine==null||!modelPath.equals(loadedPath)){
            reset();
            File cache=new File(app.getCacheDir(),"litertlm-background");cache.mkdirs();
            EngineConfig config=new EngineConfig(
                    modelPath,
                    new Backend.CPU(null,null),
                    null,
                    null,
                    null,
                    null,
                    cache.getAbsolutePath());
            Engine fresh=new Engine(config);fresh.initialize();engine=fresh;loadedPath=modelPath;
        }

        // Keep policy/instructions in the one-shot user prompt. A fresh Conversation per evidence
        // item prevents semantic state leaking across unrelated people/apps while reusing the heavy
        // Engine/model weights.
        String grounded="You are LifeOS background semantic classification. Return only the requested JSON. " +
                "Never invent facts, people, dates, urgency, actions, outcomes, or obligations.\n"+prompt;
        Conversation conversation=null;
        try{
            conversation=engine.createConversation(new ConversationConfig());
            Message response=conversation.sendMessage(grounded);
            return response==null?"":response.toString().trim();
        }finally{
            if(conversation!=null)try{conversation.close();}catch(Throwable ignored){}
        }
    }

    static synchronized void reset(){
        if(engine!=null)try{engine.close();}catch(Throwable ignored){}
        engine=null;loadedPath="";
    }
}
