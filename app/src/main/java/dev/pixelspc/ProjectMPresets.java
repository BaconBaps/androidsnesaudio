package dev.pixelspc;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Private, transactional preset/texture packs. No external paths reach projectM. */
final class ProjectMPresets {
    static final long INPUT_LIMIT=64L*1024*1024,TOTAL_LIMIT=128L*1024*1024;
    static final int ENTRY_LIMIT=5000;
    static final class Preset {
        final String id,name;final File file;
        Preset(File root,File file){this.file=file;id=root.toURI().relativize(file.toURI()).getPath();name=file.getName().replaceFirst("(?i)\\.milk$","");}
    }
    static final class Result {final int presets,textures;Result(int p,int t){presets=p;textures=t;}}
    private final Context context;
    ProjectMPresets(Context c){context=c.getApplicationContext();}
    private File root(){return new File(context.getFilesDir(),"projectm");}
    synchronized List<Preset> all() throws IOException {
        File builtins=new File(root(),"builtin-v1");if(!builtins.isDirectory()){
            if(!builtins.mkdirs()&&!builtins.isDirectory())throw new IOException("Cannot prepare starter presets");
        }
        for(String name:context.getAssets().list("projectm"))if(name.endsWith(".milk")){
            File file=new File(builtins,name);if(!file.isFile())try(InputStream in=context.getAssets().open("projectm/"+name);OutputStream out=new FileOutputStream(file)){copy(in,out,2L*1024*1024);}
        }
        List<File> files=new ArrayList<>();scan(root(),files,null);List<Preset> result=new ArrayList<>();for(File file:files)result.add(new Preset(root(),file));
        result.sort(Comparator.comparing((Preset p)->p.name,String.CASE_INSENSITIVE_ORDER).thenComparing(p->p.id));return result;
    }
    String[] texturePaths(Preset selected){
        return new String[]{selected.file.getParent(),root().getAbsolutePath()};
    }
    private static void scan(File folder,List<File> presets,Set<String> paths){
        File[] files=folder.listFiles();if(files==null)return;Arrays.sort(files,Comparator.comparing(File::getName));
        for(File f:files){if(f.getName().startsWith("."))continue;if(f.isDirectory())scan(f,presets,paths);else if(f.getName().toLowerCase(Locale.ROOT).endsWith(".milk")){if(presets!=null)presets.add(f);}else if(texture(f.getName())&&paths!=null)paths.add(folder.getAbsolutePath());}
    }
    Result importFile(Uri uri) throws IOException {return importFile(uri,ImportProgress.NONE);}
    Result importFile(Uri uri,ImportProgress progress) throws IOException {
        String name=uri.getLastPathSegment();try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
        return importStream(context.getContentResolver().openInputStream(uri),name,progress);
    }
    Result importStream(InputStream source,String name) throws IOException {return importStream(source,name,ImportProgress.NONE);}
    Result importStream(InputStream source,String name,ImportProgress progress) throws IOException {
        if(source==null)throw new IOException("Cannot open preset pack");
        File stage=new File(root(),".import-"+UUID.randomUUID()),packed=new File(context.getCacheDir(),"projectm-"+UUID.randomUUID()+".tmp");
        try(InputStream input=source){
            boolean zip=name!=null&&name.toLowerCase(Locale.ROOT).endsWith(".zip");
            if(!zip&&(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".milk")))throw new IOException("Choose a .milk preset or a ZIP preset/texture pack");
            if(!stage.mkdirs())throw new IOException("Cannot prepare preset import");
            try(OutputStream out=new FileOutputStream(packed)){copy(input,ImportProgress.reporting(out,progress,"Copying projectM pack"),INPUT_LIMIT);}
            int presets=0,textures=0;long total=0;
            if(zip){Set<String> names=new HashSet<>();int entries=0;
                try(ZipInputStream in=new ZipInputStream(new BufferedInputStream(new FileInputStream(packed)))){
                    ZipEntry entry;while((entry=in.getNextEntry())!=null){
                        if(++entries>ENTRY_LIMIT)throw new IOException("Preset packs may contain at most 5000 entries");
                        String path=entry.getName().replace('\\','/');
                        if(path.startsWith("/")||path.contains(":")||Arrays.asList(path.split("/",-1)).contains(".."))throw new IOException("Invalid path in preset pack");
                        if(path.length()>240||path.split("/").length>12)throw new IOException("Preset path is too long");
                        if(entry.isDirectory())continue;
                        boolean milk=path.toLowerCase(Locale.ROOT).endsWith(".milk"),image=texture(path);
                        if(!names.add(path.toLowerCase(Locale.ROOT)))throw new IOException("Duplicate path in preset pack");
                        File target=new File(stage,path);if(!target.getCanonicalPath().startsWith(stage.getCanonicalPath()+File.separator))throw new IOException("Invalid preset path");
                        if(milk||image){if(!target.getParentFile().mkdirs()&&!target.getParentFile().isDirectory())throw new IOException("Cannot save preset pack");
                            try(OutputStream out=new FileOutputStream(target)){total+=copy(in,out,Math.min(milk?2L*1024*1024:16L*1024*1024,TOTAL_LIMIT-total));}
                            if(milk){validatePreset(target);presets++;}else{validateTexture(target);textures++;}
                        }else total+=copy(in,null,TOTAL_LIMIT-total);
                        if(entries==1||entries%64==0)progress.update("Reading pack · "+presets+" presets · "+textures+" textures");
                        in.closeEntry();
                    }
                }
            }else{File file=new File(stage,"Imported.milk");String base=name.replace('\\','/');base=base.substring(base.lastIndexOf('/')+1);if(base.length()<=160&&!base.startsWith("."))file=new File(stage,base);try(InputStream in=new FileInputStream(packed);OutputStream out=new FileOutputStream(file)){copy(in,out,2L*1024*1024);}validatePreset(file);presets=1;}
            if(presets+textures==0)throw new IOException("This pack contains no .milk presets or PNG/JPEG textures");
            progress.update("Saving pack · "+presets+" presets · "+textures+" textures");
            File saved=new File(root(),"pack-"+UUID.randomUUID());if(!stage.renameTo(saved))throw new IOException("Cannot save preset pack");
            return new Result(presets,textures);
        }finally{delete(stage);packed.delete();}
    }
    private static boolean texture(String name){String s=name.toLowerCase(Locale.ROOT);return s.endsWith(".png")||s.endsWith(".jpg")||s.endsWith(".jpeg");}
    private static void validatePreset(File file)throws IOException{
        byte[] data=java.nio.file.Files.readAllBytes(file.toPath());String text=new String(data,StandardCharsets.UTF_8);
        if(!text.toLowerCase(Locale.ROOT).contains("[preset00]")||text.indexOf('\0')>=0)throw new IOException("Not a MilkDrop preset: "+file.getName());
    }
    private static void validateTexture(File file)throws IOException{
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),o);
        if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096||(long)o.outWidth*o.outHeight>4194304)throw new IOException("Texture must be a PNG/JPEG of at most 4 megapixels: "+file.getName());
    }
    private static long copy(InputStream in,OutputStream out,long limit)throws IOException{
        long total=0;byte[] bytes=new byte[32768];int n;while((n=in.read(bytes))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("Import cancelled");total+=n;if(total>limit)throw new IOException("Preset pack exceeds its size limit (64 MiB ZIP, 128 MiB expanded)");if(out!=null)out.write(bytes,0,n);}return total;
    }
    private static void delete(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)delete(child);file.delete();}
}
