package com.kareem.lifeos;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.kareem.lifeos.engine.GenericConversationParser;
import com.kareem.lifeos.engine.RawNode;
import com.kareem.lifeos.engine.RawScreenSnapshot;
import com.kareem.lifeos.engine.ConversationSnapshot;
import com.kareem.lifeos.engine.ParsedEvent;
import java.util.ArrayList;
import java.util.List;

public final class LifeScreenContextService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return;
        ArrayList<RawNode> nodes=new ArrayList<RawNode>();
        walk(root,nodes);
        int w=Math.max(1,getResources().getDisplayMetrics().widthPixels);
        int h=Math.max(1,getResources().getDisplayMetrics().heightPixels);
        RawScreenSnapshot snap=new RawScreenSnapshot(String.valueOf(e.getPackageName()),w,h,nodes);
        ConversationSnapshot parsed=new GenericConversationParser().parse(snap);
        if(parsed.events==null||parsed.events.isEmpty())return;
        long now=System.currentTimeMillis();
        LifeDb db=new LifeDb(this);
        try{
            for(ParsedEvent p:parsed.events){
                if(p.kind!=ParsedEvent.Kind.MESSAGE)continue;
                db.addObservation("SCREEN",snap.packageName,parsed.title,p.text,p.direction.name(),now,p.confidence);
            }
        }finally{db.close();}
    }
    private void walk(AccessibilityNodeInfo n,List<RawNode> out){
        if(n==null)return;
        Rect b=new Rect();n.getBoundsInScreen(b);
        CharSequence txt=n.getText();CharSequence desc=n.getContentDescription();
        String visible=txt!=null?txt.toString():(desc!=null?desc.toString():"");
        out.add(new RawNode(visible,String.valueOf(n.getClassName()),b.left,b.top,b.right,b.bottom));
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo c=n.getChild(i);
            if(c!=null){walk(c,out);c.recycle();}
        }
    }
    @Override public void onInterrupt(){}
}
