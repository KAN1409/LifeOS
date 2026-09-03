package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Raw screen evidence. No semantic filtering is performed here. */
public final class RawScreenSnapshot {
    public final String packageName;
    public final long capturedAt;
    public final int screenWidth;
    public final int screenHeight;
    public final List<RawNode> nodes;

    public RawScreenSnapshot(String packageName,long capturedAt,int screenWidth,int screenHeight,List<RawNode> nodes){
        this.packageName=packageName==null?"":packageName;
        this.capturedAt=capturedAt;
        this.screenWidth=Math.max(1,screenWidth);
        this.screenHeight=Math.max(1,screenHeight);
        this.nodes=Collections.unmodifiableList(new ArrayList<RawNode>(nodes==null?Collections.<RawNode>emptyList():nodes));
    }
}
