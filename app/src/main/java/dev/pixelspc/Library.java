package dev.pixelspc;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.util.*;

public final class Library {
    public static final class Track {
        public String path,title,game,artist,system;
        public int index,length,loopLength;public boolean renamed;
        JSONObject json() throws JSONException { return new JSONObject().put("path",path).put("title",title).put("game",game).put("artist",artist).put("system",system).put("index",index).put("length",length).put("loopLength",loopLength).put("renamed",renamed); }
        static Track from(JSONObject o) throws JSONException { Track t=new Track(); t.path=o.getString("path"); t.title=o.getString("title");t.renamed=o.optBoolean("renamed",false); t.game=o.getString("game"); t.artist=o.getString("artist"); t.system=o.getString("system"); t.index=o.getInt("index");if(!t.renamed&&ArchiveImporter.extension(t.path).equals("nsf")&&t.title.endsWith(" · "+(t.index+1)))t.title=String.format(Locale.ROOT,"Track %02d",t.index+1); t.length=o.getInt("length"); t.loopLength=o.has("loopLength")?o.optInt("loopLength",0):SpcTiming.loopLength(new File(t.path)); return t; }
    }
    public static final class Game {
        public String name,id; public List<Track> tracks=new ArrayList<>();
    }
    public final List<Game> games=new ArrayList<>();
    private final Context context;
    private final AtomicFile store;
    public Library(Context c) throws Exception {
        context=c; store=new AtomicFile(new File(c.getFilesDir(),"library.json"));
        if(store.getBaseFile().exists()) {
            JSONArray a=new JSONArray(new String(store.readFully(),"UTF-8"));
            for(int i=0;i<a.length();i++) { JSONObject o=a.getJSONObject(i); Game g=new Game(); g.name=o.getString("name"); g.id=o.getString("id"); JSONArray ts=o.getJSONArray("tracks"); for(int j=0;j<ts.length();j++) g.tracks.add(Track.from(ts.getJSONObject(j)));for(Track track:g.tracks)if(nes(track.path)&&track.game.isEmpty())track.game=g.name; games.add(g); }
        }
    }
    public Game importFile(Uri uri) throws Exception {
        String name="music";
        try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) { if(c!=null && c.moveToFirst()) name=c.getString(0); }
        return importFile(uri,name);
    }
    Game importFile(Uri uri,String name) throws Exception {return importFile(uri,name,ImportProgress.NONE);}
    Game importFile(Uri uri,String name,ImportProgress progress) throws Exception {
        progress.update("Reading file · "+name);
        File temp=File.createTempFile("import-",".tmp",context.getCacheDir());
        File dir=new File(context.getFilesDir(),"sets/"+UUID.randomUUID());
        boolean saved=false;
        try {
            try(InputStream in=context.getContentResolver().openInputStream(uri); OutputStream out=ImportProgress.reporting(new FileOutputStream(temp),progress,"Reading file · "+name)) {
                if(in==null) throw new IOException("Cannot read selected file");
                byte[] b=new byte[16384]; int n; long total=0;
                while((n=in.read(b))!=-1) { if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Import cancelled"); total+=n; if(total>128L*1024*1024) throw new IOException("Import is limited to 128 MB"); out.write(b,0,n); }
            }
            List<ArchiveImporter.Entry> entries=ArchiveImporter.extract(temp,name,dir,progress);
            Game game=new Game(); game.id=dir.getName(); game.name=name.replaceFirst("\\.[^.]+$","");
            int fileIndex=0;
            for(ArchiveImporter.Entry e:entries) {
                progress.update("Reading track "+(++fileIndex)+"/"+entries.size()+" · "+e.name);
                long h=Engine.open(e.file.getAbsolutePath());
                try {
                    int count=Engine.count(h); if(count<1 || count+game.tracks.size()>1000) throw new IOException("Set exceeds 1000 songs");
                    for(int i=0;i<count;i++) {
                        progress.update("Reading track "+fileIndex+"/"+entries.size()+" · "+e.name+(count>1?" · stream "+(i+1)+"/"+count:""));
                        String[] info=Engine.info(h,i); Track t=new Track(); t.path=e.file.getAbsolutePath(); t.index=i;
                        t.title=trackTitle(e.name,info[0],count,i);
                        if(ArchiveImporter.extension(e.name).equals("xa"))t.title=e.name.replaceFirst("\\.[^.]+$","")+(count>1?" · "+info[0]:"");
                        t.game=info[1];if(t.game.isEmpty()&&nes(e.name))t.game=e.name.replaceFirst("\\.[^.]+$",""); t.artist=info[2]; t.system=info[3]; t.length=Engine.length(h,i);
                        t.loopLength=e.file.getName().endsWith(".spc")?SpcTiming.loopLength(e.file):0;
                        game.tracks.add(t);
                    }
                } finally { Engine.close(h); }
            }
            if(!game.tracks.get(0).game.isEmpty()) game.name=game.tracks.get(0).game;
            progress.update("Saving "+game.tracks.size()+(game.tracks.size()==1?" track · ":" tracks · ")+name);
            synchronized(this) { games.add(game); try { save(); } catch(Exception e) { games.remove(game); throw e; } }
            saved=true; return game;
        } finally {
            temp.delete();
            if(!saved) ArchiveImporter.removeTree(dir);
        }
    }
    static boolean nes(String path){String ext=ArchiveImporter.extension(path);return ext.equals("nsf")||ext.equals("nsfe");}
    static String trackTitle(String name,String song,int count,int index){
        if(!SoundFonts.midi(name)&&song!=null&&!song.trim().isEmpty())return song;
        if(nes(name))return String.format(Locale.ROOT,"Track %02d",index+1);
        return name.replaceFirst("\\.[^.]+$","")+(count>1?" · "+(index+1):"");
    }
    public synchronized void renameTrack(Track target,String name)throws Exception{
        name=name==null?"":name.trim();if(name.isEmpty()||name.length()>120||name.contains("\n")||name.contains("\r"))throw new IOException("Use a track name between 1 and 120 characters");
        List<Track> matches=new ArrayList<>();List<String> titles=new ArrayList<>();List<Boolean> flags=new ArrayList<>();
        for(Game game:games)for(Track track:game.tracks)if(track.path.equals(target.path)&&track.index==target.index){matches.add(track);titles.add(track.title);flags.add(track.renamed);}
        if(matches.isEmpty())throw new IOException("This track is no longer in the library");
        for(Track track:matches){track.title=name;track.renamed=true;}
        try{save();}catch(Exception failure){for(int i=0;i<matches.size();i++){matches.get(i).title=titles.get(i);matches.get(i).renamed=flags.get(i);}throw failure;}
    }
    public synchronized void rename(Game game,String name) throws Exception {
        name=name==null?"":name.trim();
        if(name.isEmpty()||name.length()>80)throw new IOException("Use a name between 1 and 80 characters");
        if(!games.contains(game))throw new IOException("This library entry no longer exists");
        String previous=game.name;game.name=name;
        try {save();}catch(Exception failure){game.name=previous;throw failure;}
    }
    /** Removes only this import's private copy; source documents are never touched. */
    public synchronized void remove(Game game) throws Exception {
        int index=games.indexOf(game); if(index<0) return;
        File sets=new File(context.getFilesDir(),"sets").getCanonicalFile();
        File dir=new File(sets,game.id).getCanonicalFile();
        if(!sets.equals(dir.getParentFile())) throw new IOException("Invalid library folder");
        games.remove(index);
        try { save(); } catch(Exception e) { games.add(index,game); throw e; }
        if(dir.exists()) {
            try(java.util.stream.Stream<java.nio.file.Path> paths=java.nio.file.Files.walk(dir.toPath())) {
                for(java.nio.file.Path path:(Iterable<java.nio.file.Path>)paths.sorted(Comparator.reverseOrder())::iterator) java.nio.file.Files.deleteIfExists(path);
            }
        }
        android.content.SharedPreferences prefs=context.getSharedPreferences("playback",Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor edit=prefs.edit();
        for(Track track:game.tracks) edit.remove("track-loop:"+track.path+":"+track.index);
        edit.apply(); new TrackFavorites(context).remove(game.tracks); new SoundFonts(context).forget(game.tracks);
    }
    private void save() throws Exception {
        JSONArray a=new JSONArray(); for(Game g:games) { JSONArray ts=new JSONArray(); for(Track t:g.tracks) ts.put(t.json()); a.put(new JSONObject().put("id",g.id).put("name",g.name).put("tracks",ts)); }
        FileOutputStream out=store.startWrite();
        try { out.write(a.toString().getBytes("UTF-8")); store.finishWrite(out); } catch(Exception e) { store.failWrite(out); throw e; }
    }
}
