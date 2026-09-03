package com.kareem.lifeos.engine;
import java.util.List;
public final class RawScreenSnapshot {
    public final String packageName; public final int width,height; public final List<RawNode> nodes;
    public RawScreenSnapshot(String packageName,int width,int height,List<RawNode> nodes){this.packageName=packageName;this.width=width;this.height=height;this.nodes=nodes;}
}
