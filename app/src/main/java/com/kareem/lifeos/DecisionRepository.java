package com.kareem.lifeos;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/** Real Decision Memory objects explicitly recorded by the user in LifeOS. */
final class DecisionRepository {
    static final class DecisionObject {
        final String id,title,context,options,choice,consequences,status;final long createdAt;
        DecisionObject(String id,String title,String context,String options,String choice,String consequences,String status,long createdAt){this.id=id;this.title=s(title);this.context=s(context);this.options=s(options);this.choice=s(choice);this.consequences=s(consequences);this.status=s(status);this.createdAt=createdAt;}
    }
    private DecisionRepository(){}
    static List<DecisionObject> list(Context c,int limit){ArrayList<DecisionObject> out=new ArrayList<>();try(LifeDb db=new LifeDb(c)){for(LifeDb.Decision d:db.recentDecisions(Math.max(1,limit)))out.add(new DecisionObject("decision:"+d.id,d.title,d.context,d.options,d.choice,d.consequences,d.status,d.createdAt));}return out;}
    static int count(Context c){return list(c,1000).size();}
    static DecisionObject load(Context c,String id){String target=s(id);for(DecisionObject d:list(c,2000))if(d.id.equals(target))return d;return null;}
    private static String s(String x){return x==null?"":x.trim();}
}
