package com.kareem.lifeos;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility adapter for older detail code. New product UI uses FunctionalCapabilityRegistry.
 * This adapter intentionally exposes only capabilities backed by a real provider.
 */
final class LifeOsCapabilityRegistry {
    static final class Capability {
        final String id,label,description,icon,query;final int color,count;final String countLabel,secondary;
        Capability(String id,String label,String description,String icon,int color,String query,int count,String countLabel,String secondary){this.id=id;this.label=label;this.description=description;this.icon=icon;this.color=color;this.query=query;this.count=Math.max(0,count);this.countLabel=countLabel==null?"":countLabel;this.secondary=secondary==null?"":secondary;}
        String primaryLine(){return count+" "+countLabel;}
    }
    private LifeOsCapabilityRegistry(){}
    static List<Capability> all(Context context){ArrayList<Capability> out=new ArrayList<>();for(FunctionalCapabilityRegistry.Capability c:FunctionalCapabilityRegistry.all(context)){if(c.availability==FunctionalCapabilityRegistry.Availability.UNAVAILABLE)continue;out.add(new Capability(c.id,c.label,c.description,c.icon,c.color,"",c.count,countLabel(c.id,c.count),c.status));}return out;}
    static Capability find(Context context,String id){for(Capability c:all(context))if(c.id.equals(id))return c;List<Capability> xs=all(context);return xs.isEmpty()?null:xs.get(0);}
    static List<LifeIntelligenceEngine.Result> browse(Context context,String id,int limit){ArrayList<LifeIntelligenceEngine.Result> out=new ArrayList<>();for(FunctionalCapabilityRegistry.ObjectItem x:FunctionalCapabilityRegistry.list(context,id,limit)){out.add(new LifeIntelligenceEngine.Result(kind(id),x.title,x.summary,x.eventId,100));}return out;}
    static LifeIntelligenceEngine.Result first(Context context,String id){List<LifeIntelligenceEngine.Result> xs=browse(context,id,1);return xs.isEmpty()?null:xs.get(0);}
    private static String kind(String id){if("people".equals(id))return "People";if("conversations".equals(id))return "Conversation";if("decisions".equals(id))return "Decision";if("events".equals(id))return "Event";if("commitments".equals(id))return "Commitment";return "Object";}
    private static String countLabel(String id,int count){if("people".equals(id))return count==1?"person":"people";if("conversations".equals(id))return count==1?"conversation":"conversations";if("decisions".equals(id))return count==1?"decision":"decisions";if("events".equals(id))return count==1?"event":"events";if("commitments".equals(id))return count==1?"commitment":"commitments";return count==1?"item":"items";}
}
