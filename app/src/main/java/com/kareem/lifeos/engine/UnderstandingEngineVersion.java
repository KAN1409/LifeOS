package com.kareem.lifeos.engine;

/** Version identity for the canonical interpretation algorithm. Bump whenever replay semantics change. */
public final class UnderstandingEngineVersion {
    public static final String CURRENT="M1.5";
    private UnderstandingEngineVersion(){}
    public static boolean isCurrent(String stored){return CURRENT.equals(stored==null?"":stored.trim());}
}
