package dev.pixelspc;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class TrackFavoritesTest {
    @Test public void favouritesPersistAndSeparateSubtracksAndDuplicateImports() {
        android.content.Context context=RuntimeEnvironment.getApplication(); context.getSharedPreferences("favourites",0).edit().clear().commit();
        TrackFavorites store=new TrackFavorites(context);
        Library.Track first=new Library.Track(); first.path="/first/game.gbs"; first.index=0;
        Library.Track second=new Library.Track(); second.path=first.path; second.index=1;
        Library.Track duplicate=new Library.Track(); duplicate.path="/duplicate/game.gbs"; duplicate.index=0;
        store.toggle(first); assertTrue(new TrackFavorites(context).contains(first)); assertFalse(store.contains(second)); assertFalse(store.contains(duplicate));
        store.toggle(second); store.toggle(duplicate);
        Library.Game a=new Library.Game(); a.tracks.add(first); a.tracks.add(second);
        Library.Game b=new Library.Game(); b.tracks.add(duplicate);
        assertEquals(3,store.tracks(Arrays.asList(a,b)).size());
        store.remove(a.tracks); assertFalse(store.contains(first)); assertFalse(store.contains(second)); assertTrue(store.contains(duplicate));
        store.toggle(duplicate); assertTrue(store.tracks(Arrays.asList(a,b)).isEmpty());
    }
}
