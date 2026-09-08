package com.kareem.lifeos;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real project capability: explicit project objects created and managed by the user. */
final class ProjectRepository {
    static final class ProjectObject {final String id,name,description,status;final long createdAt,updatedAt;ProjectObject(String id,String name,String description,String status,long createdAt,long updatedAt){this.id=id;this.name=s(name);this.description=s(description);this.status=s(status);this.createdAt=createdAt;this.updatedAt=updatedAt;}}
    private ProjectRepository(){}
    static ProjectObject create(Context c,String name,String description){String n=s(name);if(n.isEmpty())return null;String id="project:"+UUID.randomUUID().toString();long now=System.currentTimeMillis();UserObjectStore.get(c).addProject(id,n,s(description),"active",now);return load(c,id);}
    static List<ProjectObject> list(Context c,int limit){ArrayList<ProjectObject> out=new ArrayList<>();for(String[] x:UserObjectStore.get(c).projects(limit))out.add(read(x));return out;}
    static ProjectObject load(Context c,String id){String[] x=UserObjectStore.get(c).project(id);return x==null?null:read(x);}
    static int count(Context c){return UserObjectStore.get(c).projectCount();}
    static void setStatus(Context c,String id,String status){String v=s(status);if(v.isEmpty())v="active";UserObjectStore.get(c).updateProjectStatus(id,v);}
    private static ProjectObject read(String[] x){return new ProjectObject(x[0],x[1],x[2],x[3],longv(x[4]),longv(x[5]));}
    private static long longv(String x){try{return Long.parseLong(x);}catch(Exception e){return 0;}}
    private static String s(String x){return x==null?"":x.trim();}
}
