package dev.pixelspc;

import java.io.File;
import java.nio.file.Files;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class LibraryDeleteTest {
    private JSONObject game(String id,String path) throws Exception {
        JSONObject track=new JSONObject().put("path",path).put("index",0).put("title","Tone").put("game","Same game").put("artist","").put("system","Mega Drive").put("length",120000).put("loopLength",0);
        return new JSONObject().put("id",id).put("name","Same game").put("tracks",new JSONArray().put(track));
    }
    @Test public void deletingOneDuplicatePersistsAndKeepsOtherImportAndOriginal() throws Exception {
        android.content.Context c=RuntimeEnvironment.getApplication();
        File first=new File(c.getFilesDir(),"sets/first/tone.vgz"),second=new File(c.getFilesDir(),"sets/second/tone.vgz"),original=new File(c.getCacheDir(),"original.vgz");
        first.getParentFile().mkdirs(); second.getParentFile().mkdirs();
        for(File f:new File[]{first,second,original}) Files.write(f.toPath(),new byte[]{1,2,3});
        Files.write(new File(c.getFilesDir(),"library.json").toPath(),new JSONArray().put(game("first",first.toString())).put(game("second",second.toString())).toString().getBytes("UTF-8"));
        Library library=new Library(c); Library.Game removed=library.games.get(0);
        String key="track-loop:"+first+":0";
        c.getSharedPreferences("playback",0).edit().putBoolean(key,true).commit();
        new SoundFonts(c).setTrack(first.toString(),"custom-bank");
        library.remove(removed);
        assertFalse(first.exists()); assertFalse(first.getParentFile().exists()); assertTrue(second.exists()); assertTrue(original.exists());
        Library restored=new Library(c); assertEquals(1,restored.games.size()); assertEquals("second",restored.games.get(0).id);
        assertFalse(c.getSharedPreferences("playback",0).contains(key));assertEquals("",new SoundFonts(c).overrideId(first.toString()));
    }
    @Test public void deletionCannotEscapeThePrivateSetFolder() throws Exception {
        android.content.Context c=RuntimeEnvironment.getApplication();
        File keep=new File(c.getFilesDir(),"keep.txt"); Files.write(keep.toPath(),new byte[]{1});
        Files.write(new File(c.getFilesDir(),"library.json").toPath(),new JSONArray().put(game("../",keep.toString())).toString().getBytes("UTF-8"));
        Library library=new Library(c);
        try { library.remove(library.games.get(0)); fail("Unsafe folder accepted"); } catch(java.io.IOException expected) { }
        assertTrue(keep.exists()); assertEquals(1,new Library(c).games.size());
    }
}
