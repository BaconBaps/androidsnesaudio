package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.util.LruCache;
import android.widget.ImageView;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Bundled, offline SNES catalog. Other systems deliberately never use this pack. */
public final class ArtworkCatalog {
    public static final class Item {
        public final String name,file;
        Item(String name,String file) { this.name=name; this.file=file; }
    }
    private static final ExecutorService DECODER=Executors.newFixedThreadPool(2);
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(8*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    public final List<Item> items;
    private final Map<String,Item> byName=new HashMap<>(),byFile=new HashMap<>();
    private final Context context;
    public ArtworkCatalog(Context c) throws Exception {
        context=c.getApplicationContext(); List<Item> list=new ArrayList<>();
        try(InputStream in=c.getAssets().open("snes-artwork.json"); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] bytes=new byte[8192]; int n; while((n=in.read(bytes))!=-1) out.write(bytes,0,n);
            JSONArray array=new JSONArray(out.toString("UTF-8"));
            for(int i=0;i<array.length();i++) {
                JSONObject o=array.getJSONObject(i); String file=o.getString("file");
                if(!file.matches("snes-artwork/[0-9]{4}\\.(png|jpe?g)")) throw new IOException("Invalid artwork path");
                Item item=new Item(o.getString("name"),file); list.add(item);
                byName.putIfAbsent(ArtworkNames.key(item.name),item); byFile.putIfAbsent(item.file,item);
            }
        }
        items=Collections.unmodifiableList(list);
    }
    public Item match(String game) { return game==null?null:byName.get(ArtworkNames.key(game)); }
    public static boolean isSnes(Library.Game game) {
        if(game==null || game.tracks.isEmpty()) return false;
        for(Library.Track t:game.tracks) if(t.path!=null && t.path.toLowerCase(Locale.ROOT).endsWith(".spc")) return true;
        return false;
    }
    private String preferenceKey(Library.Game game) { return "snes:"+ArtworkNames.key(game.name); }
    public Item find(Library.Game game) {
        if(!isSnes(game)) return null;
        String override=context.getSharedPreferences("artwork",0).getString(preferenceKey(game),null);
        if(override!=null && byFile.containsKey(override)) return byFile.get(override);
        for(Library.Track track:game.tracks) if(track.path.toLowerCase(Locale.ROOT).endsWith(".spc")) {
            Item found=match(track.game); if(found!=null) return found;
        }
        return match(game.name);
    }
    public void choose(Library.Game game,Item item) {
        if(!isSnes(game)) return;
        android.content.SharedPreferences.Editor edit=context.getSharedPreferences("artwork",0).edit();
        if(item==null) edit.remove(preferenceKey(game)); else edit.putString(preferenceKey(game),item.file);
        edit.apply();
    }
    Bitmap load(Item item,int size) {
        String key=item.file+":"+size; Bitmap cached=CACHE.get(key); if(cached!=null) return cached;
        try {
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            try(InputStream in=context.getAssets().open(item.file)) { BitmapFactory.decodeStream(in,null,options); }
            if(options.outWidth<1 || options.outHeight<1 || (long)options.outWidth*options.outHeight>80_000_000) return null;
            options.inJustDecodeBounds=false; options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>size*2) options.inSampleSize*=2;
            Bitmap bitmap; try(InputStream in=context.getAssets().open(item.file)) { bitmap=BitmapFactory.decodeStream(in,null,options); }
            if(bitmap!=null) CACHE.put(key,bitmap); return bitmap;
        } catch(IOException e) { return null; }
    }
    public void bind(ImageView view,Item item,int size) {
        String key=item==null?"":item.file+":"+size; view.setTag(key); view.setImageDrawable(null);
        if(item==null) return;
        Bitmap cached=CACHE.get(key); if(cached!=null) { view.setImageBitmap(cached); return; }
        DECODER.execute(()->{ Bitmap bitmap=load(item,size); view.post(()->{ if(key.equals(view.getTag())) view.setImageBitmap(bitmap); }); });
    }
}
