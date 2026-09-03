package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shadow-mode store that keeps immutable raw evidence separately from the current canonical view.
 * Canonical interpretation may be replaced/rebuilt without deleting raw evidence.
 */
public final class ShadowCanonicalStore {
    private static final int MAX_RAW=256;
    private static final ShadowCanonicalStore INSTANCE=new ShadowCanonicalStore();
    private final List<RawEvidenceRecord> raw=new ArrayList<RawEvidenceRecord>();
    private List<CanonicalEvent> canonical=Collections.emptyList();

    private ShadowCanonicalStore(){}
    public static ShadowCanonicalStore shared(){return INSTANCE;}

    public synchronized void appendRaw(RawEvidenceRecord record){
        if(record==null)return;
        raw.add(record);
        while(raw.size()>MAX_RAW)raw.remove(0);
    }

    public synchronized List<RawEvidenceRecord> rawEvidence(){
        return Collections.unmodifiableList(new ArrayList<RawEvidenceRecord>(raw));
    }

    public synchronized void replaceCanonical(List<CanonicalEvent> events){
        canonical=Collections.unmodifiableList(new ArrayList<CanonicalEvent>(events==null?Collections.<CanonicalEvent>emptyList():events));
    }

    public synchronized List<CanonicalEvent> canonicalEvents(){
        return canonical;
    }

    public synchronized void clearCanonicalOnly(){
        canonical=Collections.emptyList();
    }

    static synchronized void resetForTests(){
        INSTANCE.raw.clear();
        INSTANCE.canonical=Collections.emptyList();
    }
}
