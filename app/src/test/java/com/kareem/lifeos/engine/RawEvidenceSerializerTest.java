package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class RawEvidenceSerializerTest {
    @Test public void snapshotPreservesStructureAndEscapesText(){
        RawNode n=new RawNode(7,2,3,"android.widget.TextView","message_text","hello\nthere","desc\tvalue",10,20,300,80,true,false,false);
        RawScreenSnapshot s=new RawScreenSnapshot("com.example.chat",12345L,1080,2400,Arrays.asList(n));
        String out=RawEvidenceSerializer.snapshot(s);
        assertTrue(out.contains("v1\tcom.example.chat\t12345\t1080\t2400"));
        assertTrue(out.contains("7\t2\t3\tandroid.widget.TextView\tmessage_text"));
        assertTrue(out.contains("hello\\nthere"));
        assertTrue(out.contains("desc\\tvalue"));
        assertTrue(out.contains("10,20,300,80\t100"));
    }
}
