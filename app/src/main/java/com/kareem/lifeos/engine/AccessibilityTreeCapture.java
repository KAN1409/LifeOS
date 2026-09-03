package com.kareem.lifeos.engine;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts an Android accessibility tree into immutable raw evidence.
 * This class performs no semantic filtering and does not persist anything.
 */
public final class AccessibilityTreeCapture {
    private static final int MAX_DEPTH=40;
    private static final int MAX_NODES=500;

    private AccessibilityTreeCapture(){}

    public static RawScreenSnapshot capture(AccessibilityNodeInfo root,String packageName,long capturedAt,int screenWidth,int screenHeight){
        List<RawNode> nodes=new ArrayList<RawNode>();
        if(root!=null)collect(root,-1,0,nodes);
        return new RawScreenSnapshot(packageName,capturedAt,screenWidth,screenHeight,nodes);
    }

    private static void collect(AccessibilityNodeInfo node,int parentId,int depth,List<RawNode> out){
        if(node==null||depth>MAX_DEPTH||out.size()>=MAX_NODES)return;
        int id=out.size();
        Rect bounds=new Rect();
        node.getBoundsInScreen(bounds);
        CharSequence className=node.getClassName();
        CharSequence text=node.getText();
        CharSequence description=node.getContentDescription();
        String viewId="";
        try{String x=node.getViewIdResourceName();if(x!=null)viewId=x;}catch(Exception ignored){}
        out.add(new RawNode(
                id,parentId,depth,
                value(className),viewId,value(text),value(description),
                bounds.left,bounds.top,bounds.right,bounds.bottom,
                node.isClickable(),node.isScrollable(),node.isEditable()));
        for(int i=0;i<node.getChildCount()&&out.size()<MAX_NODES;i++){
            AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null){
                collect(child,id,depth+1,out);
                child.recycle();
            }
        }
    }

    private static String value(CharSequence x){return x==null?"":x.toString();}
}
