package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class StructuralScreenClassifierTest {
    private RawNode n(int id,String text,int top,int bottom,boolean clickable,boolean scrollable,boolean editable){
        return new RawNode(id,-1,1,"android.view.View","",text,"",20,top,1000,bottom,clickable,scrollable,editable);
    }

    @Test public void conversationIsDetectedFromStructureNotWords(){
        RawScreenSnapshot s=new RawScreenSnapshot("example.app",1L,1080,2400,Arrays.asList(
            n(1,"Completely arbitrary title",100,180,false,false,false),
            n(2,"alpha",300,380,false,true,false),
            n(3,"beta",500,580,false,false,false),
            n(4,"gamma",700,780,false,false,false),
            n(5,"whatever the composer says",2100,2200,true,false,true),
            n(6,"action",2100,2200,true,false,false)
        ));
        ScreenState state=StructuralScreenClassifier.classify(s);
        assertEquals(ScreenState.Type.CONVERSATION,state.type);
        assertTrue(state.confidence>=0.8);
    }

    @Test public void scrollableTextWithoutComposerIsListLike(){
        RawScreenSnapshot s=new RawScreenSnapshot("example.app",1L,1080,2400,Arrays.asList(
            n(1,"one",200,280,false,true,false),n(2,"two",400,480,false,false,false),
            n(3,"three",600,680,false,false,false),n(4,"four",800,880,false,false,false)
        ));
        assertEquals(ScreenState.Type.CONVERSATION_LIST,StructuralScreenClassifier.classify(s).type);
    }

    @Test public void rawSnapshotPreservesUiTextInsteadOfFilteringIt(){
        RawNode marker=n(1,"7 unread messages",400,460,false,false,false);
        RawScreenSnapshot s=new RawScreenSnapshot("example.app",1L,1080,2400,Arrays.asList(marker));
        assertEquals("7 unread messages",s.nodes.get(0).text);
        assertEquals(ScreenState.Type.UNKNOWN,StructuralScreenClassifier.classify(s).type);
    }
}
