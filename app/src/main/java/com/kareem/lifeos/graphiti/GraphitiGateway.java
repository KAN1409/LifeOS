package com.kareem.lifeos.graphiti;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Thin transport adapter over the pinned upstream getzep/graphiti FastAPI service. */
public final class GraphitiGateway {
    private static final String PREFS="lifeos_graphiti", KEY_BASE="base_url";
    public static final String DEFAULT_BASE="http://127.0.0.1:8000";
    public static final String SOCIAL_GROUP="lifeos-social";
    public static final String DECISION_GROUP="lifeos-decisions";
    private final Context context;
    public GraphitiGateway(Context context){this.context=context.getApplicationContext();}
    public String baseUrl(){String v=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_BASE,DEFAULT_BASE);if(v==null||v.trim().isEmpty())return DEFAULT_BASE;v=v.trim();while(v.endsWith("/"))v=v.substring(0,v.length()-1);return v;}
    public void setBaseUrl(String value){if(value==null||value.trim().isEmpty())value=DEFAULT_BASE;context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_BASE,value.trim()).apply();}
    public boolean health() throws Exception{return "healthy".equalsIgnoreCase(get("/healthcheck").optString("status"));}

    public List<Fact> search(String query,int maxFacts) throws Exception{return search(query,maxFacts,null);}
    public List<Fact> search(String query,int maxFacts,String groupId) throws Exception{
        JSONObject body=new JSONObject().put("query",query).put("max_facts",maxFacts);
        if(groupId!=null&&!groupId.trim().isEmpty())body.put("group_ids",new JSONArray().put(groupId));
        JSONArray xs=post("/search",body).optJSONArray("facts"); if(xs==null)return Collections.emptyList();
        List<Fact> out=new ArrayList<>();
        for(int i=0;i<xs.length();i++){JSONObject x=xs.getJSONObject(i);out.add(new Fact(x.optString("uuid"),x.optString("name"),x.optString("fact"),nullable(x,"valid_at"),nullable(x,"invalid_at"),nullable(x,"created_at")));}
        return out;
    }

    /** Uses Graphiti's upstream AddMessagesRequest contract unchanged. */
    public void addMessage(String groupId,String uuid,String name,String roleType,String role,String content,String timestamp,String sourceDescription) throws Exception{
        JSONObject message=new JSONObject()
                .put("content",content)
                .put("name",name==null?"":name)
                .put("role_type",roleType==null?"user":roleType)
                .put("role",role==null?JSONObject.NULL:role)
                .put("timestamp",timestamp)
                .put("source_description",sourceDescription==null?"":sourceDescription);
        if(uuid!=null&&!uuid.isEmpty())message.put("uuid",uuid);
        post("/messages",new JSONObject().put("group_id",groupId).put("messages",new JSONArray().put(message)));
    }

    private JSONObject get(String p)throws Exception{return request("GET",p,null);} private JSONObject post(String p,JSONObject b)throws Exception{return request("POST",p,b);}
    private JSONObject request(String method,String path,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(baseUrl()+path).openConnection();c.setConnectTimeout(1800);c.setReadTimeout(6000);c.setRequestMethod(method);c.setRequestProperty("Accept","application/json");
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream os=c.getOutputStream()){os.write(bytes);}}
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String text=read(in);c.disconnect();if(code<200||code>=300)throw new IllegalStateException(path+" HTTP "+code+(text.isEmpty()?"":": "+text));return text.isEmpty()?new JSONObject():new JSONObject(text);
    }
    private static String read(InputStream in)throws Exception{if(in==null)return "";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){for(String line;(line=r.readLine())!=null;)b.append(line);}return b.toString();}
    private static String nullable(JSONObject o,String k){return o.isNull(k)?null:o.optString(k,null);}
    public static final class Fact{public final String uuid,name,fact,validAt,invalidAt,createdAt;public Fact(String uuid,String name,String fact,String validAt,String invalidAt,String createdAt){this.uuid=uuid;this.name=name;this.fact=fact;this.validAt=validAt;this.invalidAt=invalidAt;this.createdAt=createdAt;}public boolean current(){return invalidAt==null||invalidAt.isEmpty();}}
}
