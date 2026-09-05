package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds Ask context from the same canonical objects visible in the product UI. */
final class GroundedQueryEngine {
    private GroundedQueryEngine(){}
    static String context(Context c,String question){return context(c,question,"","");}
    static String context(Context c,String question,String focusCapability,String focusObjectId){String q=norm(question);StringBuilder b=new StringBuilder();b.append("CANONICAL LIFEOS CONTEXT\n");b.append("Use only the object IDs and evidence-backed state below. If the requested capability is absent, say that it is not connected yet.\n\n");boolean any=false;
        if(focusCapability!=null&&!focusCapability.trim().isEmpty()&&focusObjectId!=null&&!focusObjectId.trim().isEmpty()){FunctionalCapabilityRegistry.ObjectItem focus=FunctionalCapabilityRegistry.load(c,focusCapability.trim(),focusObjectId.trim());if(focus!=null){any=true;b.append("[FOCUSED OBJECT]\n").append(focus.objectId).append(" | capability=").append(focus.capabilityId).append(" | ").append(focus.title).append(" | ").append(focus.summary).append(" | ").append(focus.meta).append('\n');if("voice".equals(focus.capabilityId)){VoiceMemoryRepository.VoiceObject v=VoiceMemoryRepository.load(c,focus.objectId);if(v!=null){b.append("audio_source_present=").append(new java.io.File(v.filePath).exists()).append(" | transcription_status=").append(v.status).append(" | language=").append(v.language).append(" | engine=").append(v.engine).append('\n');if(v.hasTranscript())b.append("VERBATIM_TRANSCRIPT:\n").append(v.transcript).append('\n');}}b.append('\n');}}
        if(wantsAttention(q)){List<ObligationRepository.ObligationObject> os=ObligationRepository.open(c,8);if(!os.isEmpty()){any=true;b.append("[OPEN OBLIGATIONS]\n");for(ObligationRepository.ObligationObject o:os)b.append(o.id).append(" | ").append(o.title).append(" | ").append(o.summary).append(" | action=").append(o.action).append(" | evidence=").append(o.evidenceCount).append('\n');b.append('\n');}}
        List<FunctionalCapabilityRegistry.ObjectItem> hits=FunctionalSearchEngine.search(c,question,"All",12);if(!hits.isEmpty()){any=true;b.append("[MATCHING OBJECTS]\n");for(FunctionalCapabilityRegistry.ObjectItem x:hits)b.append(x.objectId).append(" | capability=").append(x.capabilityId).append(" | ").append(x.title).append(" | ").append(x.summary).append(" | ").append(x.meta).append('\n');b.append('\n');}
        if(wantsRecent(q)){List<CanonicalTimelineRepository.TimelineObject> xs=CanonicalTimelineRepository.recent(c,10);if(!xs.isEmpty()){any=true;b.append("[RECENT CANONICAL TIMELINE]\n");for(CanonicalTimelineRepository.TimelineObject x:xs)b.append(x.id).append(" | ").append(x.kind).append(" | ").append(x.title).append(" | ").append(x.summary).append('\n');b.append('\n');}}
        if(wantsActions(q)){List<PersistentActionQueue.Item> actions=new PersistentActionQueue(c).pending();if(!actions.isEmpty()){any=true;b.append("[REAL PENDING ACTION PROPOSALS]\n");int n=0;for(PersistentActionQueue.Item a:actions){b.append("action:").append(a.proposal.proposalId).append(" | ").append(a.proposal.actionType).append(" | ").append(a.proposal.target).append(" | ").append(a.proposal.payloadSummary).append('\n');if(++n>=8)break;}b.append('\n');}}
        if(!any)b.append("No canonical connected object matched this question.\n");return b.toString();}

    static String fallbackAnswer(Context c,String q){return fallbackAnswer(c,q,"","");}
    static String fallbackAnswer(Context c,String q,String focusCapability,String focusObjectId){String low=norm(q);List<String> lines=new ArrayList<>();if(focusCapability!=null&&!focusCapability.isEmpty()&&focusObjectId!=null&&!focusObjectId.isEmpty()){FunctionalCapabilityRegistry.ObjectItem focus=FunctionalCapabilityRegistry.load(c,focusCapability,focusObjectId);if(focus!=null){if("voice".equals(focus.capabilityId)){VoiceMemoryRepository.VoiceObject v=VoiceMemoryRepository.load(c,focus.objectId);if(v!=null&&v.hasTranscript())return "This voice memory says:\n"+v.transcript;}lines.add("• "+focus.title+(focus.summary.isEmpty()?"":" — "+focus.summary));}}
        if(wantsAttention(low)){for(ObligationRepository.ObligationObject o:ObligationRepository.open(c,5))lines.add("• "+o.title+" — "+o.summary);}if(lines.isEmpty()){for(FunctionalCapabilityRegistry.ObjectItem x:FunctionalSearchEngine.search(c,q,"All",5))lines.add("• "+x.title+(x.summary.isEmpty()?"":" — "+x.summary));}if(lines.isEmpty()&&wantsRecent(low)){for(CanonicalTimelineRepository.TimelineObject x:CanonicalTimelineRepository.recent(c,5))lines.add("• "+x.title+(x.summary.isEmpty()?"":" — "+x.summary));}if(lines.isEmpty())return "I don't have a connected canonical LifeOS object that supports that answer yet.";StringBuilder b=new StringBuilder("Here's what LifeOS can ground right now:\n");for(String x:lines)b.append(x).append('\n');return b.toString().trim();}

    private static boolean wantsAttention(String q){return contains(q,"attention","priority","priorities","open","waiting","commitment","commitments","need to do","what should i do","مهم","اهتمام","مستني","التزام");}
    private static boolean wantsRecent(String q){return contains(q,"recent","recently","today","happened","timeline","changed","update","updates","اليوم","النهارده","حصل","جديد");}
    private static boolean wantsActions(String q){return contains(q,"action","actions","reply","send","call","do next","ready","approve","اعمل","رد","ابعت");}
    private static boolean contains(String q,String... xs){for(String x:xs)if(q.contains(x))return true;return false;}
    private static String norm(String x){return x==null?"":x.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();}
}
