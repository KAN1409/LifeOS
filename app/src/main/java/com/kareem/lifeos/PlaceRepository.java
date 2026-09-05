package com.kareem.lifeos;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real place capability: user-labeled locations saved from Android location evidence. */
final class PlaceRepository {
    enum Availability { OPERATIONAL, SETUP_REQUIRED }
    interface Callback { void onResult(PlaceObject place,String error); }
    static final class PlaceObject {final String id,label,provider;final double latitude,longitude;final float accuracy;final long observedAt,addedAt;PlaceObject(String id,String label,double latitude,double longitude,float accuracy,String provider,long observedAt,long addedAt){this.id=id;this.label=s(label);this.latitude=latitude;this.longitude=longitude;this.accuracy=accuracy;this.provider=s(provider);this.observedAt=observedAt;this.addedAt=addedAt;}}
    private PlaceRepository(){}
    static Availability availability(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED?Availability.OPERATIONAL:Availability.SETUP_REQUIRED;}
    static List<PlaceObject> list(Context c,int limit){ArrayList<PlaceObject> out=new ArrayList<>();for(String[] x:UserObjectStore.get(c).places(limit))out.add(read(x));return out;}
    static PlaceObject load(Context c,String id){String[] x=UserObjectStore.get(c).place(id);return x==null?null:read(x);}
    static int count(Context c){return UserObjectStore.get(c).placeCount();}
    static void captureCurrent(Context c,String label,Callback cb){if(availability(c)!=Availability.OPERATIONAL){cb.onResult(null,"Location permission required");return;}LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);if(lm==null){cb.onResult(null,"Android location service is unavailable");return;}String provider=bestProvider(lm);if(provider==null){cb.onResult(null,"No location provider is enabled");return;}try{if(Build.VERSION.SDK_INT>=30){lm.getCurrentLocation(provider,null,c.getMainExecutor(),loc->{if(loc==null)fallbackLast(c,lm,label,cb);else save(c,label,loc,cb);});return;}lm.requestSingleUpdate(provider,new LocationListener(){@Override public void onLocationChanged(Location location){save(c,label,location,cb);}@Override public void onStatusChanged(String provider,int status,Bundle extras){}@Override public void onProviderEnabled(String provider){}@Override public void onProviderDisabled(String provider){}},Looper.getMainLooper());}catch(SecurityException e){cb.onResult(null,"Location permission required");}catch(Exception e){fallbackLast(c,lm,label,cb);}}
    private static void fallbackLast(Context c,LocationManager lm,String label,Callback cb){try{Location best=null;for(String p:lm.getProviders(true)){Location x=lm.getLastKnownLocation(p);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}if(best==null)cb.onResult(null,"No current location fix is available yet");else save(c,label,best,cb);}catch(Exception e){cb.onResult(null,"Could not read current location");}}
    private static void save(Context c,String label,Location loc,Callback cb){String name=s(label);if(name.isEmpty())name="Saved place";String id="place:"+UUID.randomUUID().toString();long now=System.currentTimeMillis();UserObjectStore.get(c).addPlace(id,name,loc.getLatitude(),loc.getLongitude(),loc.hasAccuracy()?loc.getAccuracy():0,loc.getProvider(),loc.getTime()>0?loc.getTime():now,now);cb.onResult(load(c,id),null);}
    private static String bestProvider(LocationManager lm){try{if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))return LocationManager.GPS_PROVIDER;if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))return LocationManager.NETWORK_PROVIDER;List<String> xs=lm.getProviders(true);return xs.isEmpty()?null:xs.get(0);}catch(Exception e){return null;}}
    private static PlaceObject read(String[] x){return new PlaceObject(x[0],x[1],doublev(x[2]),doublev(x[3]),floatv(x[4]),x[5],longv(x[6]),longv(x[7]));}
    private static double doublev(String x){try{return Double.parseDouble(x);}catch(Exception e){return 0;}}
    private static float floatv(String x){try{return Float.parseFloat(x);}catch(Exception e){return 0;}}
    private static long longv(String x){try{return Long.parseLong(x);}catch(Exception e){return 0;}}
    private static String s(String x){return x==null?"":x.trim();}
}
