package dev.pixelspc;
import android.content.Context;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class PlaylistsTest {
    private Context context;private File file;
    @Before public void clear(){context=RuntimeEnvironment.getApplication();file=new File(context.getFilesDir(),"playlists.json");file.delete();new File(file+".bak").delete();}
    private Library.Track track(String path,int index){Library.Track t=new Library.Track();t.path=path;t.index=index;t.title=path;t.game="Game";t.artist="Artist";t.system="SNES";t.length=180000;return t;}
    private Library.Game game(Library.Track... tracks){Library.Game g=new Library.Game();g.id="source";g.name="Source";g.tracks.addAll(Arrays.asList(tracks));return g;}
    @Test public void orderedRepeatsAndSubsongIndexesSurviveReloadWithoutCopyingMetadata()throws Exception{
        Library.Track a=track("a.nsf",0),b=track("a.nsf",1);Playlists store=new Playlists(context);String id=store.save(null," Night Mix ",Arrays.asList(b,a,b));a.title="Updated metadata";
        Library.Game saved=new Playlists(context).all(Collections.singletonList(game(a,b))).get(0);assertEquals(id,saved.id);assertEquals("Night Mix",saved.name);assertEquals(Arrays.asList(b,a,b),saved.tracks);assertEquals("Updated metadata",saved.tracks.get(1).title);
        String json=new String(Files.readAllBytes(file.toPath()),"UTF-8");assertFalse(json.contains("Updated metadata"));assertFalse(json.contains("Artist"));
    }
    @Test public void renameReplaceAndDeleteLeaveMusicAndOtherPlaylistsAlone()throws Exception{
        File audio=new File(context.getCacheDir(),"playlist-original.spc");Files.write(audio.toPath(),new byte[]{1,2,3});Library.Track a=track(audio.getPath(),0),b=track("b.gbs",0);Library.Game library=game(a,b);
        Playlists first=new Playlists(context),second=new Playlists(context);String id=first.save(null,"First",Collections.singletonList(a));second.save(null,"Second",Collections.singletonList(b));
        first.rename(id,"Renamed");first.save(id,"Renamed",Arrays.asList(b,a,b));assertEquals(2,second.all(Collections.singletonList(library)).size());first.delete(id);
        assertEquals("Second",second.all(Collections.singletonList(library)).get(0).name);assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(audio.toPath()));assertEquals(2,library.tracks.size());
    }
    @Test public void unavailableImportsAreSkippedWithoutLosingOtherTracksOrNames()throws Exception{
        Library.Track removed=track("old.spc",0),kept=track("other.gbs",0);Playlists store=new Playlists(context);String id=store.save(null,"Mixed",Arrays.asList(removed,kept,removed));
        Library.Game partial=store.all(Collections.singletonList(game(kept))).get(0);assertEquals(Collections.singletonList(kept),partial.tracks);assertEquals(id,partial.id);
        Library.Game empty=store.all(Collections.emptyList()).get(0);assertTrue(empty.tracks.isEmpty());store.rename(id,"Keep this name");assertEquals("Keep this name",store.all(Collections.emptyList()).get(0).name);
    }
    @Test public void invalidNamesEmptyListsAndDuplicatesCannotOverwriteExistingData()throws Exception{
        Library.Track a=track("a.spc",0);Playlists store=new Playlists(context);store.save(null,"Mix",Collections.singletonList(a));byte[] before=Files.readAllBytes(file.toPath());
        for(String name:new String[]{" ","mix",String.join("",Collections.nCopies(81,"a"))})try{store.save(null,name,Collections.singletonList(a));fail(name);}catch(IOException expected){}
        try{store.save(null,"Empty",Collections.emptyList());fail();}catch(IOException expected){}
        try{store.save(null,"Too many",Collections.nCopies(1001,a));fail();}catch(IOException expected){}
        assertArrayEquals(before,Files.readAllBytes(file.toPath()));
    }
}
