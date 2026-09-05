package com.kareem.lifeos;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Real people capability backed by Android Contacts. Conversation linking remains separate. */
final class ContactPersonRepository {
    enum Availability { OPERATIONAL, SETUP_REQUIRED }
    static final class PersonObject {
        final String id,lookupKey,name;
        final long contactId;
        final List<String> phones,emails;
        PersonObject(String id,long contactId,String lookupKey,String name,List<String> phones,List<String> emails){this.id=id;this.contactId=contactId;this.lookupKey=s(lookupKey);this.name=s(name);this.phones=phones;this.emails=emails;}
    }
    private ContactPersonRepository(){}

    static Availability availability(Context c){return c.checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED?Availability.OPERATIONAL:Availability.SETUP_REQUIRED;}

    static List<PersonObject> list(Context c,int limit){
        ArrayList<PersonObject> out=new ArrayList<>();if(availability(c)!=Availability.OPERATIONAL)return out;
        Cursor cur=null;try{cur=c.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,new String[]{ContactsContract.Contacts._ID,ContactsContract.Contacts.LOOKUP_KEY,ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},null,null,ContactsContract.Contacts.DISPLAY_NAME_PRIMARY+" COLLATE NOCASE ASC");while(cur!=null&&cur.moveToNext()&&out.size()<Math.max(1,limit)){long id=cur.getLong(0);String lookup=cur.getString(1),name=cur.getString(2);out.add(loadContact(c,id,lookup,name));}}catch(SecurityException ignored){}finally{if(cur!=null)cur.close();}return out;
    }
    static int count(Context c){if(availability(c)!=Availability.OPERATIONAL)return 0;Cursor cur=null;try{cur=c.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,new String[]{ContactsContract.Contacts._ID},null,null,null);return cur==null?0:cur.getCount();}catch(SecurityException e){return 0;}finally{if(cur!=null)cur.close();}}
    static PersonObject load(Context c,String objectId){String target=s(objectId);if(target.isEmpty())return null;for(PersonObject p:list(c,3000))if(p.id.equals(target))return p;return null;}

    private static PersonObject loadContact(Context c,long contactId,String lookup,String name){ArrayList<String> phones=new ArrayList<>(),emails=new ArrayList<>();ContentResolver r=c.getContentResolver();Cursor p=null,e=null;try{p=r.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},ContactsContract.CommonDataKinds.Phone.CONTACT_ID+"=?",new String[]{String.valueOf(contactId)},null);while(p!=null&&p.moveToNext())addUnique(phones,p.getString(0));}catch(SecurityException ignored){}finally{if(p!=null)p.close();}try{e=r.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS},ContactsContract.CommonDataKinds.Email.CONTACT_ID+"=?",new String[]{String.valueOf(contactId)},null);while(e!=null&&e.moveToNext())addUnique(emails,e.getString(0));}catch(SecurityException ignored){}finally{if(e!=null)e.close();}String key=s(lookup);if(key.isEmpty())key=String.valueOf(contactId);return new PersonObject("person:contact:"+key,contactId,key,name,phones,emails);}
    private static void addUnique(List<String> xs,String value){String v=s(value);if(!v.isEmpty()&&!xs.contains(v))xs.add(v);}
    private static String s(String x){return x==null?"":x.trim();}
}
