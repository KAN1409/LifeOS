package com.kareem.lifeos;

import android.content.Context;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search federation over registered functional providers only. */
final class FunctionalSearchEngine {
    static final class Hit {final FunctionalCapabilityRegistry.ObjectItem item;final double score;Hit(FunctionalCapabilityRegistry.ObjectItem item,double score){this.item=item;this.score=score;}}
    private FunctionalSearchEngine(){}

    static List<FunctionalCapabilityRegistry.ObjectItem> search(Context c,String query,String capabilityFilter,int limit){String q=norm(query);ArrayList<Hit> hits=new ArrayList<>();for(FunctionalCapabilityRegistry.Capability cap:FunctionalCapabilityRegistry.all(c)){if(cap.availability!=FunctionalCapabilityRegistry.Availability.OPERATIONAL)continue;if(capabilityFilter!=null&&!capabilityFilter.isEmpty()&&!"All".equals(capabilityFilter)&&!cap.id.equals(capabilityFilter))continue;for(FunctionalCapabilityRegistry.ObjectItem item:FunctionalCapabilityRegistry.list(c,cap.id,600)){double score=match(q,item.title+" "+item.summary+" "+item.meta);if(q.isEmpty()||score>0)hits.add(new Hit(item,score));}}hits.sort(Comparator.comparingDouble((Hit h)->h.score).reversed());ArrayList<FunctionalCapabilityRegistry.ObjectItem> out=new ArrayList<>();for(Hit h:hits){out.add(h.item);if(out.size()>=Math.max(1,limit))break;}return out;}
    private static double match(String q,String text){if(q.isEmpty())return .1;String n=norm(text);if(n.equals(q))return 3;if(n.startsWith(q))return 2.5;if(n.contains(q))return 2;String[] ts=q.split(" ");int hit=0,total=0;for(String t:ts){if(t.length()<2)continue;total++;if(n.contains(t))hit++;}return total==0?0:(double)hit/total;}
    private static String norm(String x){return x==null?"":x.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
}
