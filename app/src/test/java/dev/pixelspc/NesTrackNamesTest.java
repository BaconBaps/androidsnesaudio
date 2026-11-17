package dev.pixelspc;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class NesTrackNamesTest {
    @Test public void nesFallbacksKeepRealLabelsAndOtherFormatsKeepTheirExistingNames(){
        assertEquals("Track 01",Library.trackTitle("Mario.NSF","",3,0));assertEquals("Track 12",Library.trackTitle("Game.nsfe"," ",15,11));
        assertEquals("Overworld",Library.trackTitle("Game.nsfe","Overworld",10,0));assertEquals("game",Library.trackTitle("game.mid","Piano",1,0));
        assertEquals("theme · 2",Library.trackTitle("theme.vgm","",2,1));
    }
    private JSONObject track(String path,String title,int index)throws Exception{return new JSONObject().put("path",path).put("title",title).put("index",index).put("game","NES Game").put("artist","Composer").put("system","Nintendo NES").put("length",180000).put("loopLength",0);}
    @Test public void renamePersistsPerSubsongWithoutChangingSourceOrOtherImports()throws Exception{
        android.content.Context c=RuntimeEnvironment.getApplication();File source=new File(c.getFilesDir(),"fixture.nsf");Files.write(source.toPath(),new byte[]{1,2,3});
        JSONArray tracks=new JSONArray().put(track(source.toString(),"NES Game · 1",0)).put(track(source.toString(),"NES Game · 2",1));
        File store=new File(c.getFilesDir(),"library.json");Files.write(store.toPath(),new JSONArray().put(new JSONObject().put("id","fixture").put("name","NES Game").put("tracks",tracks)).toString().getBytes("UTF-8"));
        Library library=new Library(c);assertEquals("Track 01",library.games.get(0).tracks.get(0).title);Library.Track first=library.games.get(0).tracks.get(0);library.renameTrack(first,"  Title screen  ");
        Library restored=new Library(c);assertEquals("Title screen",restored.games.get(0).tracks.get(0).title);assertTrue(restored.games.get(0).tracks.get(0).renamed);assertEquals("Track 02",restored.games.get(0).tracks.get(1).title);assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(source.toPath()));
        for(String invalid:new String[]{" ",String.join("",Collections.nCopies(121,"x")),"two\nlines"})try{library.renameTrack(first,invalid);fail();}catch(IOException expected){assertEquals("Title screen",first.title);}
    }
    @Test public void nsfeNamesAndExplicitUserNamesSurviveLoading()throws Exception{
        assertEquals("Boss",Library.Track.from(track("/game.nsfe","Boss",0)).title);
        assertEquals("NES Game · 1",Library.Track.from(track("/game.nsf","NES Game · 1",0).put("renamed",true)).title);
    }
}
