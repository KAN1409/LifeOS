package com.kareem.lifeos.engine;

public final class RawNode {
    public final String text;
    public final String className;
    public final int left, top, right, bottom;
    public RawNode(String text,String className,int left,int top,int right,int bottom){this.text=text==null?"":text;this.className=className==null?"":className;this.left=left;this.top=top;this.right=right;this.bottom=bottom;}
}
