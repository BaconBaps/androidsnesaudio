package dev.pixelspc;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import org.json.*;

/** Named ordered references to library tracks; never copies or deletes audio. */
final class Playlists {
    static final int LIMIT=100,TRACK_LIMIT=1000;
    private final AtomicFile store;
    Playlists(Context c){store=new AtomicFile(new File(c.getFilesDir(),"playlists.json"));}
    static boolean saved(Library.Game game){return game!=null&&game.id!=null&&game.id.startsWith("playlist-");}
    private static String key(String path,int index){return path+'\0'+index;}
    private JSONArray read()throws Exception{
        byte[] bytes;
        if(store.getBaseFile().length()>8*1024*1024)throw new IOException("Saved playlists exceed the size limit");
        try{bytes=store.readFully();}catch(FileNotFoundException absent){return new JSONArray();}
        if(bytes.length>8*1024*1024)throw new IOException("Saved playlists exceed the size limit");
        JSONObject root=new JSONObject(new String(bytes,"UTF-8"));if(root.getInt("version")!=1)throw new IOException("Unsupported playlist version");
        JSONArray entries=root.getJSONArray("playlists");if(entries.length()>LIMIT)throw new IOException("Too many saved playlists");return entries;
    }
    List<Library.Game> all(List<Library.Game> games)throws Exception{
        synchronized(Playlists.class){
            Map<String,Library.Track> available=new HashMap<>();for(Library.Game game:games)for(Library.Track track:game.tracks)available.put(key(track.path,track.index),track);
            List<Library.Game> result=new ArrayList<>();JSONArray entries=read();
            for(int i=0;i<entries.length();i++){
                JSONObject entry=entries.getJSONObject(i);Library.Game list=new Library.Game();list.id=entry.getString("id");list.name=entry.getString("name");
                if(!saved(list))throw new IOException("Invalid saved playlist");JSONArray refs=entry.getJSONArray("tracks");if(refs.length()>TRACK_LIMIT)throw new IOException("Playlist exceeds 1000 tracks");
                for(int n=0;n<refs.length();n++){JSONObject ref=refs.getJSONObject(n);Library.Track track=available.get(key(ref.getString("path"),ref.getInt("index")));if(track!=null)list.tracks.add(track);}
                result.add(list);
            }
            result.sort(Comparator.comparing(g->g.name,String.CASE_INSENSITIVE_ORDER));return result;
        }
    }
    String save(String id,String name,List<Library.Track> tracks)throws Exception{
        name=name==null?"":name.trim();if(name.isEmpty()||name.length()>80)throw new IOException("Use a name between 1 and 80 characters");
        if(tracks.isEmpty()||tracks.size()>TRACK_LIMIT)throw new IOException("A playlist needs between 1 and 1000 tracks");
        synchronized(Playlists.class){
            JSONArray entries=read(),next=new JSONArray();boolean found=false;
            for(int i=0;i<entries.length();i++){JSONObject old=entries.getJSONObject(i);if(old.getString("id").equals(id)){found=true;continue;}if(old.getString("name").equalsIgnoreCase(name))throw new IOException("A playlist with this name already exists");next.put(old);}
            if(id!=null&&!found)throw new IOException("Playlist no longer exists");if(next.length()>=LIMIT)throw new IOException("You can save up to 100 playlists");
            if(id==null)id="playlist-"+UUID.randomUUID();JSONArray refs=new JSONArray();for(Library.Track track:tracks)refs.put(new JSONObject().put("path",track.path).put("index",track.index));
            next.put(new JSONObject().put("id",id).put("name",name).put("tracks",refs));write(next);return id;
        }
    }
    void rename(String id,String name)throws Exception{
        name=name==null?"":name.trim();if(name.isEmpty()||name.length()>80)throw new IOException("Use a name between 1 and 80 characters");
        synchronized(Playlists.class){JSONArray entries=read();JSONObject selected=null;for(int i=0;i<entries.length();i++){JSONObject item=entries.getJSONObject(i);if(item.getString("id").equals(id))selected=item;else if(item.getString("name").equalsIgnoreCase(name))throw new IOException("A playlist with this name already exists");}if(selected==null)throw new IOException("Playlist no longer exists");selected.put("name",name);write(entries);}
    }
    void delete(String id)throws Exception{
        synchronized(Playlists.class){JSONArray entries=read(),next=new JSONArray();for(int i=0;i<entries.length();i++)if(!entries.getJSONObject(i).getString("id").equals(id))next.put(entries.getJSONObject(i));write(next);}
    }
    private void write(JSONArray entries)throws Exception{
        byte[] bytes=new JSONObject().put("version",1).put("playlists",entries).toString().getBytes("UTF-8");if(bytes.length>8*1024*1024)throw new IOException("Saved playlists exceed the size limit");
        FileOutputStream out=store.startWrite();try{out.write(bytes);store.finishWrite(out);}catch(Exception failure){store.failWrite(out);throw failure;}
    }
}
