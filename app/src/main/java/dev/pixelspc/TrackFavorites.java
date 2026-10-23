package dev.pixelspc;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Private-copy identity keeps duplicate imports independent. */
final class TrackFavorites {
    private final SharedPreferences prefs;
    TrackFavorites(Context context) { prefs=context.getSharedPreferences("favourites",Context.MODE_PRIVATE); }
    static String key(Library.Track track) { return track.path+":"+track.index; }
    boolean contains(Library.Track track) { return prefs.getBoolean(key(track),false); }
    void toggle(Library.Track track) {
        SharedPreferences.Editor edit=prefs.edit();
        if(contains(track)) edit.remove(key(track)); else edit.putBoolean(key(track),true);
        edit.apply();
    }
    List<Library.Track> tracks(List<Library.Game> games) {
        List<Library.Track> result=new ArrayList<>();
        for(Library.Game game:games) for(Library.Track track:game.tracks) if(contains(track)) result.add(track);
        return result;
    }
    void remove(List<Library.Track> tracks) { SharedPreferences.Editor edit=prefs.edit(); for(Library.Track track:tracks) edit.remove(key(track)); edit.apply(); }
}
