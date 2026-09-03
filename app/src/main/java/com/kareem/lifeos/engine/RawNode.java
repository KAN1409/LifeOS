package com.kareem.lifeos.engine;

/** Immutable text-bearing node captured from an accessibility tree. */
public final class RawNode {
    public final int id;
    public final int parentId;
    public final int depth;
    public final String className;
    public final String viewId;
    public final String text;
    public final String contentDescription;
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;
    public final boolean clickable;
    public final boolean scrollable;
    public final boolean editable;

    public RawNode(int id,int parentId,int depth,String className,String viewId,String text,String contentDescription,
                   int left,int top,int right,int bottom,boolean clickable,boolean scrollable,boolean editable){
        this.id=id;this.parentId=parentId;this.depth=depth;
        this.className=s(className);this.viewId=s(viewId);this.text=s(text);this.contentDescription=s(contentDescription);
        this.left=left;this.top=top;this.right=right;this.bottom=bottom;
        this.clickable=clickable;this.scrollable=scrollable;this.editable=editable;
    }

    public int width(){return Math.max(0,right-left);}
    public int height(){return Math.max(0,bottom-top);}
    public int centerX(){return left+width()/2;}
    public int centerY(){return top+height()/2;}
    public boolean hasText(){return !text.trim().isEmpty()||!contentDescription.trim().isEmpty();}
    private static String s(String x){return x==null?"":x;}
}
