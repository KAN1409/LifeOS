package com.kareem.lifeos;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.security.NetworkSecurityPolicy;
import com.kareem.lifeos.context.UniversalObservationStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Non-destructive runtime QA harness. It validates real providers, canonical semantic invariants,
 * persistence/load contracts, search federation, source accessibility and device setup state.
 * It never invents fixtures inside the user's live database and never marks user objects handled.
 */
final class FullQaHarness {
    private FullQaHarness(){}

    static final class Report {
        final JSONObject json; final int pass,fail,warn,skip;
        Report(JSONObject json,int pass,int fail,int warn,int skip){this.json=json;this.pass=pass;this.fail=fail;this.warn=warn;this.skip=skip;}
    }

    static Report run(Context context){
        Context c=context.getApplicationContext();Runner r=new Runner(c);
        r.identity();
        r.deviceSetup();
        r.databases();
        r.obligations();
        r.timeline();
        r.capabilities();
        r.files();
        r.voice();
        r.imagesAndOcr();
        r.providerObjects();
        return r.finish();
    }

    static Report write(Context c,File out){
        Report report=run(c);try(FileOutputStream f=new FileOutputStream(out)){f.write(report.json.toString(2).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}return report;
    }

    private static final class Runner {
        final Context c;final JSONArray checks=new JSONArray();int pass,fail,warn,skip;
        Runner(Context c){this.c=c;}

        void identity(){
            try{
                PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);long version=Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;
                check("app.package","build",eq("com.kareem.lifeos",c.getPackageName()),"Package identity is "+c.getPackageName(),"Expected com.kareem.lifeos");
                check("app.version","build",version>=45,"versionCode "+version,"Unexpected downgrade: "+version);
                ApplicationInfo a=c.getApplicationInfo();boolean backup=(a.flags&ApplicationInfo.FLAG_ALLOW_BACKUP)!=0;
                check("security.backup_disabled","security",!backup,"Android backup disabled","android:allowBackup is effectively enabled");
                boolean clear=NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted();
                check("security.cleartext_global","security",!clear,"Global cleartext traffic is blocked by policy","Global cleartext traffic appears permitted");
            }catch(Throwable t){error("app.identity","build",t);}
        }

        void deviceSetup(){
            permission("permission.notifications",Manifest.permission.POST_NOTIFICATIONS,Build.VERSION.SDK_INT<33);
            permission("permission.microphone",Manifest.permission.RECORD_AUDIO,false);
            permission("permission.contacts",Manifest.permission.READ_CONTACTS,false);
            permission("permission.calendar",Manifest.permission.READ_CALENDAR,false);
            boolean location=granted(Manifest.permission.ACCESS_FINE_LOCATION)||granted(Manifest.permission.ACCESS_COARSE_LOCATION);
            state("permission.location","setup",location?"PASS":"WARN",location?"Location permission available":"Location permission is not granted; Place capture cannot run");
            try{
                String listeners=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");boolean enabled=listeners!=null&&listeners.contains(c.getPackageName());
                state("service.notification_listener","setup",enabled?"PASS":"WARN",enabled?"Notification listener enabled":"Notification listener is not enabled");
            }catch(Throwable t){error("service.notification_listener","setup",t);}
            try{
                String services=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);boolean enabled=services!=null&&services.contains(c.getPackageName());
                state("service.accessibility","setup",enabled?"PASS":"WARN",enabled?"Accessibility service enabled":"Accessibility service is not enabled");
            }catch(Throwable t){error("service.accessibility","setup",t);}
            try{state("model.background","intelligence",BackgroundModelManager.isReadyFast(c)?"PASS":"WARN",BackgroundModelManager.isReadyFast(c)?"Background semantic model ready":"Background model is not currently verified ready");}catch(Throwable t){error("model.background","intelligence",t);}
        }

        void databases(){
            try(LifeDb db=new LifeDb(c)){
                int recent=db.recentEvents(200).size();state("db.life","data","PASS","LifeDb opens; "+recent+" recent events readable");
            }catch(Throwable t){error("db.life","data",t);}
            try{long raw=UniversalObservationStore.get(c).count();state("db.raw_observations","data",raw>0?"PASS":"WARN",raw+" durable raw observations");}catch(Throwable t){error("db.raw_observations","data",t);}
            try{AttentionStore a=AttentionStore.get(c);int open=a.openCount(),pending=a.pendingCount();state("db.attention","data","PASS",open+" open/provisional attention rows · "+pending+" pending semantic work items");}catch(Throwable t){error("db.attention","data",t);}
            try{int meanings=NotificationMeaningStore.get(c).count();state("db.semantic_meanings","data",meanings>0?"PASS":"WARN",meanings+" stored notification meanings");}catch(Throwable t){error("db.semantic_meanings","data",t);}
        }

        void obligations(){
            try(LifeDb db=new LifeDb(c)){
                List<ObligationRepository.ObligationObject> xs=ObligationRepository.open(c,1000);Set<String> ids=new HashSet<>();boolean unique=true,loads=true,canonical=true,evidence=true;
                AttentionStore store=AttentionStore.get(c);NotificationMeaningStore meanings=NotificationMeaningStore.get(c);
                for(ObligationRepository.ObligationObject o:xs){
                    if(!ids.add(o.id))unique=false;if(ObligationRepository.load(c,o.id)==null)loads=false;
                    if(o.evidenceEventIds.isEmpty())evidence=false;
                    for(Long eventId:o.evidenceEventIds){if(eventId==null||eventId<=0){evidence=false;continue;}LifeDb.Event e=db.eventById(eventId);AttentionStore.Item item=store.forEvent(eventId);if(e==null||item==null){evidence=false;continue;}NotificationMeaning m=meanings.forObservation(item.sourceObservationId);if(!CanonicalSemanticPolicy.isCanonicalAttention(item,e,m))canonical=false;}
                }
                check("obligation.ids_unique","semantic",unique,"All "+xs.size()+" obligation IDs are unique","Duplicate canonical obligation IDs found");
                check("obligation.reload","semantic",loads,"Every obligation reloads by stable ID","At least one obligation cannot reload by ID");
                check("obligation.evidence","semantic",evidence,"Every obligation has resolvable evidence","At least one obligation has missing/invalid evidence");
                check("obligation.canonical_gate","semantic",canonical,"Every surfaced obligation still passes the v45 exact-evidence gate","A surfaced obligation bypasses canonical semantic policy");
                FunctionalCapabilityRegistry.Capability cap=FunctionalCapabilityRegistry.find(c,"commitments");check("obligation.count_consistency","semantic",cap!=null&&cap.count==xs.size(),"Commitments capability count matches repository: "+xs.size(),"Commitments capability count disagrees with canonical repository");
            }catch(Throwable t){error("obligation.runtime","semantic",t);}
        }

        void timeline(){
            try(LifeDb db=new LifeDb(c)){
                List<CanonicalTimelineRepository.TimelineObject> xs=CanonicalTimelineRepository.recent(c,250);Set<String> ids=new HashSet<>();boolean unique=true,load=true,canonical=true;
                NotificationMeaningStore meanings=NotificationMeaningStore.get(c);
                for(CanonicalTimelineRepository.TimelineObject x:xs){if(!ids.add(x.id))unique=false;if(CanonicalTimelineRepository.load(c,x.id)==null)load=false;if(!x.calendar&&x.eventId>0){LifeDb.Event e=db.eventById(x.eventId);NotificationMeaning m=e==null?null:meanings.forStreamAt(e.threadKey,e.at);if(e==null||!CanonicalSemanticPolicy.isCanonicalTimeline(e,m))canonical=false;}}
                check("timeline.ids_unique","timeline",unique,"Timeline IDs are unique across "+xs.size()+" items","Duplicate timeline IDs found");
                check("timeline.reload","timeline",load,"Timeline items reload by ID","At least one timeline item cannot reload");
                check("timeline.canonical_gate","timeline",canonical,"Every non-calendar timeline item passes canonical timeline policy","A timeline item bypasses semantic filtering");
            }catch(Throwable t){error("timeline.runtime","timeline",t);}
        }

        void capabilities(){
            try{
                List<FunctionalCapabilityRegistry.Capability> caps=FunctionalCapabilityRegistry.all(c);Set<String> ids=new HashSet<>();boolean unique=true;
                for(FunctionalCapabilityRegistry.Capability cap:caps){if(!ids.add(cap.id))unique=false;int limit=Math.min(5000,Math.max(50,cap.count+5));List<FunctionalCapabilityRegistry.ObjectItem> list=FunctionalCapabilityRegistry.list(c,cap.id,limit);
                    if(cap.availability==FunctionalCapabilityRegistry.Availability.OPERATIONAL&&cap.count<=5000)check("capability.count."+cap.id,"capability",cap.count==list.size(),cap.label+" count matches provider: "+cap.count,cap.label+" count="+cap.count+" but provider list="+list.size());
                    else state("capability.count."+cap.id,"capability","SKIP",cap.label+" is "+cap.availability);
                    boolean reload=true;for(int i=0;i<Math.min(25,list.size());i++){FunctionalCapabilityRegistry.ObjectItem item=list.get(i);FunctionalCapabilityRegistry.ObjectItem loaded=FunctionalCapabilityRegistry.load(c,cap.id,item.objectId);if(loaded==null||!item.objectId.equals(loaded.objectId)){reload=false;break;}}
                    check("capability.reload."+cap.id,"capability",reload,cap.label+" sample objects reload by stable ID",cap.label+" contains a non-reloadable object");
                    if(!list.isEmpty()&&!list.get(0).title.trim().isEmpty()){
                        FunctionalCapabilityRegistry.ObjectItem target=list.get(0);List<FunctionalCapabilityRegistry.ObjectItem> hits=FunctionalSearchEngine.search(c,target.title,cap.id,50);boolean found=false;for(FunctionalCapabilityRegistry.ObjectItem h:hits)if(target.objectId.equals(h.objectId)){found=true;break;}
                        check("search.roundtrip."+cap.id,"search",found,"Search round-trip finds a real "+cap.label+" object","Search could not find sampled "+cap.label+" object by its own title");
                    }else state("search.roundtrip."+cap.id,"search","SKIP","No "+cap.label+" object available for search round-trip");
                }
                check("capability.ids_unique","capability",unique,"All capability IDs are unique","Duplicate capability IDs found");
            }catch(Throwable t){error("capability.runtime","capability",t);}
        }

        void files(){
            try{
                List<FileRepository.FileObject> xs=FileRepository.list(c,500);boolean load=true,source=true,unique=true;Set<String> ids=new HashSet<>();
                for(FileRepository.FileObject f:xs){if(!ids.add(f.id))unique=false;if(FileRepository.load(c,f.id)==null)load=false;try(InputStream in=c.getContentResolver().openInputStream(Uri.parse(f.uri))){if(in==null)source=false;}catch(Exception e){source=false;}}
                check("files.ids_unique","files",unique,"File IDs are unique","Duplicate File IDs found");check("files.reload","files",load,"All connected files reload by ID","A connected file cannot reload");check("files.uri_access","files",source,"All connected file URIs are still readable","At least one persisted document URI is no longer readable");
            }catch(Throwable t){error("files.runtime","files",t);}
        }

        void voice(){
            try{
                List<VoiceMemoryRepository.VoiceObject> xs=VoiceMemoryRepository.list(c,500);boolean unique=true,load=true,files=true,wav=true,status=true;Set<String> ids=new HashSet<>();
                for(VoiceMemoryRepository.VoiceObject v:xs){if(!ids.add(v.id))unique=false;if(VoiceMemoryRepository.load(c,v.id)==null)load=false;File f=new File(v.filePath);if(!f.exists()||f.length()<44){files=false;wav=false;continue;}if(!validWav(f))wav=false;if("complete".equals(v.status)&&v.transcript.trim().isEmpty())status=false;if(v.durationMs<0||v.sizeBytes<0)status=false;}
                check("voice.ids_unique","voice",unique,"Voice memory IDs are unique","Duplicate voice memory IDs found");check("voice.reload","voice",load,"Voice memories reload by stable ID","A voice memory cannot reload");check("voice.audio_files","voice",files,"All voice source files still exist","At least one voice memory lost its source audio");check("voice.wav_header","voice",wav,"All stored voice files have RIFF/WAVE headers","At least one stored voice file is not a valid WAV container");check("voice.state_consistency","voice",status,"Voice transcript states are internally consistent","Voice status/transcript metadata is inconsistent");
            }catch(Throwable t){error("voice.runtime","voice",t);}
        }

        void imagesAndOcr(){
            try{
                List<ImageRepository.ImageObject> xs=ImageRepository.list(c,500);boolean unique=true,load=true,uri=true,state=true;Set<String> ids=new HashSet<>();
                for(ImageRepository.ImageObject x:xs){if(!ids.add(x.id))unique=false;if(ImageRepository.load(c,x.id)==null)load=false;try(InputStream in=c.getContentResolver().openInputStream(Uri.parse(x.uri))){if(in==null)uri=false;}catch(Exception e){uri=false;}OcrResult latest=x.latest(c);if("complete".equals(x.ocrStatus)&&latest==null)state=false;if((x.width<0||x.height<0))state=false;}
                check("image.ids_unique","ocr",unique,"OCR test image IDs are unique","Duplicate image IDs found");check("image.reload","ocr",load,"OCR images reload by stable ID","An OCR image cannot reload");check("image.uri_access","ocr",uri,"OCR source image URIs are readable","At least one OCR source URI is inaccessible");check("ocr.state_consistency","ocr",state,"OCR image state is internally consistent","OCR complete/status metadata is inconsistent");
                OcrBenchmark.Summary b=OcrBenchmark.summarize(c);if(b.samples<30)state("ocr.quality_gate","ocr","WARN",b.samples+" scored samples; promotion requires at least 30 (CER "+pct(b.cer)+", WER "+pct(b.wer)+")");else check("ocr.quality_gate","ocr",b.readyForPromotion(),"OCR benchmark passes: CER "+pct(b.cer)+" · WER "+pct(b.wer),"OCR benchmark fails promotion gate: CER "+pct(b.cer)+" · WER "+pct(b.wer));
            }catch(Throwable t){error("ocr.runtime","ocr",t);}
        }

        void providerObjects(){
            try{
                boolean contacts=true;if(ContactPersonRepository.availability(c)==ContactPersonRepository.Availability.OPERATIONAL){for(ContactPersonRepository.PersonObject p:ContactPersonRepository.list(c,200)){if(ContactPersonRepository.load(c,p.id)==null){contacts=false;break;}}state("provider.contacts","provider",contacts?"PASS":"FAIL",contacts?"Contacts provider reloads sampled people":"A Contact object cannot reload");}else state("provider.contacts","provider","WARN","Contacts permission not granted; provider cannot be fully tested");
                boolean calendar=true;if(CalendarEventRepository.availability(c)==CalendarEventRepository.Availability.OPERATIONAL){for(CalendarEventRepository.CalendarEventObject e:CalendarEventRepository.recentAndUpcoming(c,200)){if(CalendarEventRepository.load(c,e.id)==null||e.end<e.begin){calendar=false;break;}}state("provider.calendar","provider",calendar?"PASS":"FAIL",calendar?"Calendar provider reloads sampled events and time ranges are valid":"Calendar provider has a broken event object");}else state("provider.calendar","provider","WARN","Calendar permission not granted; provider cannot be fully tested");
                boolean places=true;for(PlaceRepository.PlaceObject p:PlaceRepository.list(c,500)){if(PlaceRepository.load(c,p.id)==null||p.latitude<-90||p.latitude>90||p.longitude<-180||p.longitude>180||p.accuracy<0){places=false;break;}}check("provider.places","provider",places,"Saved places reload and coordinates are valid","Saved place data is invalid");
                boolean projects=true;Set<String> pids=new HashSet<>();for(ProjectRepository.ProjectObject p:ProjectRepository.list(c,1000)){if(!pids.add(p.id)||ProjectRepository.load(c,p.id)==null||p.name.trim().isEmpty()){projects=false;break;}}check("provider.projects","provider",projects,"Projects reload with unique IDs and names","Project provider contains invalid objects");
                boolean decisions=true;for(DecisionRepository.DecisionObject d:DecisionRepository.list(c,1000)){if(DecisionRepository.load(c,d.id)==null||d.id.trim().isEmpty()){decisions=false;break;}}check("provider.decisions","provider",decisions,"Decisions reload by stable ID","Decision provider contains invalid objects");
                boolean conversations=true;for(ConversationRepository.ConversationObject q:ConversationRepository.list(c,1000)){if(ConversationRepository.load(c,q.id)==null||q.latestEventId<=0){conversations=false;break;}}check("provider.conversations","provider",conversations,"Conversations reload and point to durable evidence","Conversation provider contains invalid objects");
            }catch(Throwable t){error("provider.runtime","provider",t);}
        }

        Report finish(){
            JSONObject root=new JSONObject();try{root.put("schema","LIFEOS_QA_V1");root.put("generated_at",System.currentTimeMillis());root.put("package",c.getPackageName());root.put("device",Build.MANUFACTURER+" "+Build.MODEL);root.put("android",Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT);JSONObject summary=new JSONObject();summary.put("pass",pass);summary.put("fail",fail);summary.put("warn",warn);summary.put("skip",skip);summary.put("total",pass+fail+warn+skip);root.put("summary",summary);root.put("checks",checks);}catch(Exception ignored){}return new Report(root,pass,fail,warn,skip);
        }

        void permission(String id,String permission,boolean notApplicable){if(notApplicable){state(id,"setup","SKIP","Not required on this Android version");return;}boolean ok=granted(permission);state(id,"setup",ok?"PASS":"WARN",ok?permission+" granted":permission+" not granted");}
        boolean granted(String permission){return c.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;}
        void check(String id,String area,boolean ok,String passMessage,String failMessage){state(id,area,ok?"PASS":"FAIL",ok?passMessage:failMessage);}
        void error(String id,String area,Throwable t){state(id,area,"FAIL",t.getClass().getSimpleName()+": "+safe(t.getMessage()));}
        void state(String id,String area,String status,String message){if("PASS".equals(status))pass++;else if("FAIL".equals(status))fail++;else if("WARN".equals(status))warn++;else skip++;try{JSONObject x=new JSONObject();x.put("id",id);x.put("area",area);x.put("status",status);x.put("message",message);checks.put(x);}catch(Exception ignored){}}
    }

    private static boolean validWav(File file){try(FileInputStream in=new FileInputStream(file)){byte[] h=new byte[12];if(in.read(h)!=12)return false;return "RIFF".equals(new String(h,0,4,StandardCharsets.US_ASCII))&&"WAVE".equals(new String(h,8,4,StandardCharsets.US_ASCII));}catch(Exception e){return false;}}
    private static boolean eq(String a,String b){return a==null?b==null:a.equals(b);}private static String safe(String x){return x==null?"":x.replaceAll("\\s+"," ").trim();}private static String pct(double x){return String.format(Locale.US,"%.1f%%",x*100);}
}
