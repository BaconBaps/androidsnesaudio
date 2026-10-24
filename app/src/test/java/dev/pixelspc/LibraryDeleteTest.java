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
    @Test public void renamePersistsWithoutChangingTracksFilesOrPreferences() throws Exception {
        android.content.Context context=RuntimeEnvironment.getApplication();File first=new File(context.getFilesDir(),"sets/first/tone.vgz"),second=new File(context.getFilesDir(),"sets/second/tone.vgz");first.getParentFile().mkdirs();second.getParentFile().mkdirs();Files.write(first.toPath(),new byte[]{1});Files.write(second.toPath(),new byte[]{2});
        Files.write(new File(context.getFilesDir(),"library.json").toPath(),new JSONArray().put(game("first",first.toString())).put(game("second",second.toString())).toString().getBytes("UTF-8"));
        Library library=new Library(context);Library.Game entry=library.games.get(0);TrackFavorites favourites=new TrackFavorites(context);favourites.toggle(entry.tracks.get(0));SoundFonts fonts=new SoundFonts(context);fonts.setTrack(first.toString(),"bank");
        library.rename(entry,"  My soundtrack  ");Library restored=new Library(context);assertEquals("My soundtrack",restored.games.get(0).name);assertEquals("Same game",restored.games.get(1).name);assertEquals("first",restored.games.get(0).id);assertEquals(first.toString(),restored.games.get(0).tracks.get(0).path);assertEquals("Tone",restored.games.get(0).tracks.get(0).title);assertTrue(first.exists());assertTrue(second.exists());assertTrue(favourites.contains(entry.tracks.get(0)));assertEquals("bank",fonts.overrideId(first.toString()));
        for(String invalid:new String[]{"   ",String.join("",java.util.Collections.nCopies(81,"x"))})try{library.rename(entry,invalid);fail();}catch(java.io.IOException expected){assertEquals("My soundtrack",entry.name);}
        assertEquals("My soundtrack",new Library(context).games.get(0).name);
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
