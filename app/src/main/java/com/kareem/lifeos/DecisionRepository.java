package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
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
    static int count(Context c){try(LifeDb db=new LifeDb(c)){long n=db.count("decisions");return n>Integer.MAX_VALUE?Integer.MAX_VALUE:(int)Math.max(0,n);}}
    static DecisionObject load(Context c,String id){String target=s(id);if(!target.startsWith("decision:"))return null;long numeric;try{numeric=Long.parseLong(target.substring("decision:".length()));}catch(Exception e){return null;}try(LifeDb db=new LifeDb(c);Cursor q=db.getReadableDatabase().rawQuery("SELECT id,created_at,title,context,options,choice,consequences,status FROM decisions WHERE id=? LIMIT 1",new String[]{String.valueOf(numeric)})){if(!q.moveToFirst())return null;return new DecisionObject("decision:"+q.getLong(0),q.getString(2),q.getString(3),q.getString(4),q.getString(5),q.getString(6),q.getString(7),q.getLong(1));}}
    private static String s(String x){return x==null?"":x.trim();}
}
