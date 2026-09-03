package com.kareem.lifeos;

import com.kareem.lifeos.engine.RawNode;
import com.kareem.lifeos.engine.RawScreenSnapshot;
import com.kareem.lifeos.engine.StructuralElement;
import com.kareem.lifeos.engine.StructuralElementExtractor;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class StructuralElementExtractorTest {
    @Test public void bottomClickableActionsAreNotMessages(){
        RawScreenSnapshot s=new RawScreenSnapshot("test",1,1080,2400,Arrays.asList(
            new RawNode(1,-1,0,"ScrollView","","", "",0,250,1080,2000,false,true,false),
            new RawNode(2,1,1,"TextView","","Actual message","",80,900,760,1030,false,false,false),
            new RawNode(3,1,1,"Button","","Share","",40,2050,250,2180,true,false,false),
            new RawNode(4,1,1,"Button","","Copy","",270,2050,470,2180,true,false,false),
            new RawNode(5,1,1,"Button","","Menu","",490,2050,700,2180,true,false,false),
            new RawNode(6,1,1,"EditText","","","",80,2200,900,2340,true,false,true)
        ));
        List<StructuralElement> xs=StructuralElementExtractor.extract(s);
        assertEquals(StructuralElement.Role.MESSAGE_CANDIDATE,roleOf(xs,2));
        assertEquals(StructuralElement.Role.ACTION,roleOf(xs,3));
        assertEquals(StructuralElement.Role.ACTION,roleOf(xs,4));
        assertEquals(StructuralElement.Role.ACTION,roleOf(xs,5));
        assertEquals(StructuralElement.Role.COMPOSER,roleOf(xs,6));
    }

    @Test public void centeredPassiveTextBecomesMarkerRegardlessOfWords(){
        RawScreenSnapshot s=new RawScreenSnapshot("test",1,1080,2400,Arrays.asList(
            new RawNode(1,-1,0,"ScrollView","","","",0,250,1080,2000,false,true,false),
            new RawNode(2,1,1,"TextView","","anything at all","",420,700,660,770,false,false,false)
        ));
        assertEquals(StructuralElement.Role.CENTER_MARKER,roleOf(StructuralElementExtractor.extract(s),2));
    }

    @Test public void passiveDescriptionInsideLowerComposerActionIsNotMessage(){
        RawScreenSnapshot s=new RawScreenSnapshot("test",1,1080,2400,Arrays.asList(
            new RawNode(1,-1,0,"ScrollView","","","",0,250,1080,2050,false,true,false),
            new RawNode(2,-1,0,"ViewGroup","","","",0,1950,1080,2400,true,false,false),
            new RawNode(3,2,1,"TextView","","","Voice message, Button. Double tap and hold to record",100,1980,900,2100,false,false,false)
        ));
        assertEquals(StructuralElement.Role.ACTION,roleOf(StructuralElementExtractor.extract(s),3));
    }

    private static StructuralElement.Role roleOf(List<StructuralElement> xs,int id){
        for(StructuralElement x:xs)if(x.node.id==id)return x.role;
        fail("Missing node "+id);
        return null;
    }
}
