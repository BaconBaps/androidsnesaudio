package dev.pixelspc;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Groups loose tracks and PSF dependencies, imports archives intact, rolls back failures. */
final class ImportBatch {
    static final long BYTE_LIMIT=128L*1024*1024;
    static final class Budget {long bytes;}
    static List<Library.Game> run(Context context,List<ImportDocuments.Item> all,Set<String> selected,String label)throws Exception{
        Library library=new Library(context);List<Library.Game> added=new ArrayList<>();List<ImportDocuments.Item> loose=new ArrayList<>(),archives=new ArrayList<>();boolean needsLibraries=false;
        for(ImportDocuments.Item item:all)if(!item.dependency()&&selected.contains(item.key())){if(item.archive())archives.add(item);else{loose.add(item);String ext=ArchiveImporter.extension(item.path);needsLibraries|=ext.equals("psf")||ext.equals("minipsf");}}
        if(loose.isEmpty()&&archives.isEmpty())throw new IOException("Select at least one music file");
        if(needsLibraries)for(ImportDocuments.Item item:all)if(item.dependency())loose.add(item);
        Budget budget=new Budget();File stage=null;int tracks=0;long stored=0;
        try {
            if(!loose.isEmpty()){
                stage=File.createTempFile("music-selection-",".zip",context.getCacheDir());Set<String> paths=new HashSet<>();
                try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(stage)))){
                    for(ImportDocuments.Item item:loose){ImportDocuments.checkCancelled();if(!paths.add(item.path.toLowerCase(Locale.ROOT)))throw new IOException("Two selected files have the same path. Import their containing folder instead.");
                        zip.putNextEntry(new ZipEntry(item.path));try(InputStream in=context.getContentResolver().openInputStream(item.uri)){copy(in,zip,budget);}zip.closeEntry();}
                }
                Library.Game game=library.importFile(Uri.fromFile(stage),(label==null||label.trim().isEmpty()?"Selected music":label)+".zip");added.add(game);tracks+=game.tracks.size();stored+=storedBytes(context,game);if(stored>ArchiveImporter.TOTAL_LIMIT)throw new IOException("Selected music expands beyond 256 MiB. Import a smaller selection.");
            }
            for(ImportDocuments.Item item:archives){
                ImportDocuments.checkCancelled();if(stage!=null){stage.delete();stage=null;}stage=File.createTempFile("music-archive-",".tmp",context.getCacheDir());
                try(InputStream in=context.getContentResolver().openInputStream(item.uri);OutputStream out=new BufferedOutputStream(new FileOutputStream(stage))){copy(in,out,budget);}
                Library.Game game=library.importFile(Uri.fromFile(stage),item.path.substring(item.path.lastIndexOf('/')+1));added.add(game);tracks+=game.tracks.size();stored+=storedBytes(context,game);if(stored>ArchiveImporter.TOTAL_LIMIT)throw new IOException("Selected music expands beyond 256 MiB. Import a smaller selection.");
                if(tracks>1000)throw new IOException("A selection may contain at most 1000 tracks");
            }
            ImportDocuments.checkCancelled();return added;
        }catch(Exception failure){
            for(int i=added.size()-1;i>=0;i--)try{library.remove(added.get(i));}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw failure;
        }finally{if(stage!=null)stage.delete();}
    }
    private static long storedBytes(Context context,Library.Game game)throws IOException {
        try(java.util.stream.Stream<java.nio.file.Path> paths=java.nio.file.Files.walk(new File(context.getFilesDir(),"sets/"+game.id).toPath())) {
            long size=0;for(java.nio.file.Path path:(Iterable<java.nio.file.Path>)paths::iterator)if(java.nio.file.Files.isRegularFile(path))size+=java.nio.file.Files.size(path);return size;
        }
    }
    private static void copy(InputStream in,OutputStream out,Budget budget)throws IOException{
        if(in==null)throw new IOException("Cannot read selected document");byte[] bytes=new byte[32768];int n;
        while((n=in.read(bytes))!=-1){ImportDocuments.checkCancelled();budget.bytes+=n;if(budget.bytes>BYTE_LIMIT)throw new IOException("A selection is limited to 128 MiB. Import a smaller selection.");out.write(bytes,0,n);}
    }
}
