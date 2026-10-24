package dev.pixelspc;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.util.*;

/** SoundFont files are shared across MIDI tracks; preferences store stable IDs. */
final class SoundFonts {
    static final String BUILTIN="pixel-gm";
    static final long LIMIT=64L*1024*1024;
    static final class Bank {
        final String id,name; final File file; final boolean bundled;
        Bank(String id,String name,File file,boolean bundled) { this.id=id;this.name=name;this.file=file;this.bundled=bundled; }
    }
    private final Context context;
    private final SharedPreferences prefs;
    SoundFonts(Context context) { this.context=context.getApplicationContext();prefs=context.getSharedPreferences("soundfonts",Context.MODE_PRIVATE); }
    static boolean midi(String path) { String name=path.toLowerCase(Locale.ROOT);return name.endsWith(".mid")||name.endsWith(".midi"); }
    private File folder() throws IOException { File dir=new File(context.getFilesDir(),"soundfonts");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create SoundFont folder");return dir; }
    private Bank builtin() throws IOException { synchronized(SoundFonts.class) {
        File file=new File(folder(),"pixel-gm.sf2");
        if(!file.isFile()) {
            AtomicFile target=new AtomicFile(file);FileOutputStream output=target.startWrite();
            try(InputStream input=context.getAssets().open("soundfonts/Pixel-GM.sf2")) { byte[] buffer=new byte[16384];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);target.finishWrite(output); }
            catch(IOException e) { target.failWrite(output);throw e; }
        }
        return new Bank(BUILTIN,"Pixel GM · built in",file,true);
    }}
    synchronized List<Bank> banks() throws IOException {
        List<Bank> result=new ArrayList<>();result.add(builtin());File[] files=folder().listFiles((dir,name)->name.endsWith(".sf2")&&!name.equals("pixel-gm.sf2"));
        if(files!=null)for(File file:files) {String id=file.getName().replaceFirst("\\.sf2$","");result.add(new Bank(id,prefs.getString("name:"+id,"Imported SoundFont"),file,false));}
        result.subList(1,result.size()).sort(Comparator.comparing(b->b.name,String.CASE_INSENSITIVE_ORDER));return result;
    }
    String nowPlayingTitle(Library.Track track) {
        if(!midi(track.path))return track.title;String id=overrideId(track.path);if(id.isEmpty())return track.title;
        String name=BUILTIN.equals(id)?"Pixel GM":prefs.getString("name:"+id,null);
        if(name==null||(!BUILTIN.equals(id)&&!new File(context.getFilesDir(),"soundfonts/"+id+".sf2").isFile()))return track.title;
        return track.title+" (with "+name+")";
    }
    String defaultId() { return prefs.getString("default",BUILTIN); }
    String overrideId(String path) { return prefs.getString("track:"+path,""); }
    void setDefault(String id) { prefs.edit().putString("default",id).apply(); }
    void setTrack(String path,String id) { SharedPreferences.Editor edit=prefs.edit();if(id.isEmpty())edit.remove("track:"+path);else edit.putString("track:"+path,id);edit.apply(); }
    String pathFor(String track) throws IOException {
        String id=overrideId(track);if(id.isEmpty())id=defaultId();
        for(Bank bank:banks())if(bank.id.equals(id))return bank.file.getAbsolutePath();return builtin().file.getAbsolutePath();
    }
    void rename(Bank bank,String name) throws IOException {
        name=name.trim();if(bank.bundled)throw new IOException("The built-in bank cannot be renamed");if(name.isEmpty()||name.length()>80)throw new IOException("Use a name between 1 and 80 characters");prefs.edit().putString("name:"+bank.id,name).apply();
    }
    synchronized void delete(Bank bank) throws IOException {
        if(bank.bundled)throw new IOException("The built-in bank is always available");
        if(!bank.file.delete())throw new IOException("Cannot delete SoundFont");SharedPreferences.Editor edit=prefs.edit().remove("name:"+bank.id);
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet())if(bank.id.equals(entry.getValue())&&(entry.getKey().equals("default")||entry.getKey().startsWith("track:")))edit.remove(entry.getKey());edit.apply();
    }
    void forget(List<Library.Track> tracks) {SharedPreferences.Editor edit=prefs.edit();for(Library.Track track:tracks)edit.remove("track:"+track.path);edit.apply();}
    Bank importBank(Uri uri) throws Exception {
        String name="SoundFont.sf2";
        try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
        if(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".sf2"))throw new IOException("Choose an uncompressed .sf2 SoundFont (up to 64 MiB)");
        String id=UUID.randomUUID().toString();File temp=new File(folder(),id+".tmp"),file=new File(folder(),id+".sf2");
        try {
            try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)) {
                if(in==null)throw new IOException("Cannot open selected SoundFont");byte[] bytes=new byte[32768];int n;long total=0;
                while((n=in.read(bytes))!=-1){total+=n;if(total>LIMIT)throw new IOException("SoundFonts are limited to 64 MiB in this version");out.write(bytes,0,n);}
            }
            Engine.validateSoundFont(temp.getAbsolutePath());
            name=name.replaceFirst("(?i)\\.sf2$","");if(name.length()>80)name=name.substring(0,80);
            prefs.edit().putString("name:"+id,name).apply();if(!temp.renameTo(file)){prefs.edit().remove("name:"+id).apply();throw new IOException("Cannot save SoundFont");}
            return new Bank(id,name,file,false);
        } finally { temp.delete(); }
    }
}
