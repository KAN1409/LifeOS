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
    @Override public void onAccessibilityEvent(AccessibilityEvent e){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;ArrayList<RawNode> nodes=new ArrayList<RawNode>();walk(root,-1,0,nodes,new int[]{0});Rect r=new Rect();root.getBoundsInScreen(r);int w=Math.max(1,getResources().getDisplayMetrics().widthPixels),h=Math.max(1,getResources().getDisplayMetrics().heightPixels);RawScreenSnapshot snap=new RawScreenSnapshot(String.valueOf(e.getPackageName()),System.currentTimeMillis(),w,h,nodes);ConversationSnapshot parsed=GenericConversationParser.parse(snap);if(parsed.events.isEmpty())return;LifeDb db=new LifeDb(this);try{for(ParsedEvent p:parsed.events){if(p.kind!=ParsedEvent.Kind.MESSAGE)continue;db.addObservation("SCREEN",snap.packageName,parsed.title,p.text,p.direction.name(),p.occurredAt,p.confidence);}}finally{db.close();}}
    private void walk(AccessibilityNodeInfo n,int parent,int depth,List<RawNode> out,int[] next){if(n==null)return;int id=next[0]++;Rect b=new Rect();n.getBoundsInScreen(b);out.add(new RawNode(id,parent,depth,b.left,b.top,b.right,b.bottom,String.valueOf(n.getClassName()),String.valueOf(n.getViewIdResourceName()),String.valueOf(n.getText()),String.valueOf(n.getContentDescription()),n.isClickable(),n.isScrollable(),n.isEditable(),n.isSelected()));for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){walk(c,id,depth+1,out,next);c.recycle();}}}
    @Override public void onInterrupt(){}
}
