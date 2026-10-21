package dev.pixelspc;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Flat, bounded extraction: archive paths never become filesystem paths. */
public final class ArchiveImporter {
    static final long FILE_LIMIT=32L*1024*1024, TOTAL_LIMIT=256L*1024*1024;
    public static final int TRACK_LIMIT=1000;
    static final Set<String> FORMATS=new HashSet<>(Arrays.asList("spc","nsf","nsfe","gbs","vgm","vgz","gym","hes","kss","ay","sap"));
    public static boolean supported(String name) { return FORMATS.contains(extension(name)); }
    static String extension(String name) { int p=name.lastIndexOf('.'); return p<0?"":name.substring(p+1).toLowerCase(Locale.ROOT); }
    public static final class Entry {
        public final File file; public final String name;
        Entry(File f,String n) { file=f; name=n; }
    }
    public static List<Entry> extract(File input,String name,File destination) throws Exception {
        if(!destination.isDirectory() && !destination.mkdirs()) throw new IOException("Cannot create library folder");
        List<Entry> result=new ArrayList<>(); Budget budget=new Budget();
        try {
            String ext=extension(name);
            if(ext.equals("zip")) {
                try(ZipInputStream zip=new ZipInputStream(new FileInputStream(input))) {
                    ZipEntry e; int entries=0;
                    while((e=zip.getNextEntry())!=null) {
                        if(++entries>3000) throw new IOException("Too many archive entries");
                        if(!e.isDirectory() && supported(e.getName())) {
                            Entry item=entry(destination,e.getName(),result.size());
                            try(OutputStream out=new FileOutputStream(item.file)) { copy(zip,out,budget); }
                            result.add(item); if(result.size()>TRACK_LIMIT) throw new IOException("Too many tracks");
                        } else { copy(zip,new Discard(),budget); }
                        zip.closeEntry();
                    }
                }
            } else if(ext.equals("rsn") || ext.equals("rar")) {
                try(Archive rar=new Archive(input)) {
                    if(rar.isEncrypted() || rar.isPasswordProtected()) throw new IOException("Password protected sets are not supported");
                    int entries=0;
                    for(FileHeader h:rar.getFileHeaders()) {
                        if(++entries>3000) throw new IOException("Too many archive entries");
                        if(h.isDirectory()) continue;
                        if(h.getFullUnpackSize()>FILE_LIMIT || h.getFullUnpackSize()<0) throw new IOException("Archive entry is too large");
                        boolean music=supported(h.getFileName());
                        Entry item=music?entry(destination,h.getFileName(),result.size()):null;
                        OutputStream target=music?new FileOutputStream(item.file):new Discard();
                        try(OutputStream out=new LimitedOutput(target,budget)) { rar.extractFile(h,out); }
                        if(music) { result.add(item); if(result.size()>TRACK_LIMIT) throw new IOException("Too many tracks"); }
                    }
                }
            } else if(supported(name)) {
                Entry item=entry(destination,name,0);
                try(InputStream in=new FileInputStream(input); OutputStream out=new FileOutputStream(item.file)) { copy(in,out,budget); }
                result.add(item);
            } else throw new IOException("Choose an SPC, RSN, ZIP, or supported chiptune file");
            if(result.isEmpty()) throw new IOException("No supported music files found in this set");
            result.sort(Comparator.comparing(e->e.name.toLowerCase(Locale.ROOT)));
            return result;
        } catch(Exception e) {
            File[] files=destination.listFiles(); if(files!=null) for(File f:files) f.delete();
            destination.delete(); throw e;
        }
    }
    static Entry entry(File dir,String raw,int index) throws IOException {
        if(index>=TRACK_LIMIT) throw new IOException("Too many tracks");
        String base=raw.replace('\\','/'); base=base.substring(base.lastIndexOf('/')+1);
        return new Entry(new File(dir,String.format(Locale.ROOT,"%04d.%s",index,extension(base))),base);
    }
    static class Budget { long total; }
    static class Discard extends OutputStream { public void write(int b) {} public void write(byte[] b,int o,int n) {} }
    static class LimitedOutput extends FilterOutputStream {
        final Budget budget; long size;
        LimitedOutput(OutputStream out,Budget b) { super(out); budget=b; }
        void charge(int n) throws IOException { size+=n; budget.total+=n; if(size>FILE_LIMIT || budget.total>TOTAL_LIMIT) throw new IOException("Set exceeds safe extraction limits"); }
        public void write(int b) throws IOException { charge(1); out.write(b); }
        public void write(byte[] b,int o,int n) throws IOException { charge(n); out.write(b,o,n); }
    }
    static void copy(InputStream in,OutputStream out,Budget budget) throws IOException {
        LimitedOutput limited=new LimitedOutput(out,budget); byte[] b=new byte[16384]; int n;
        while((n=in.read(b))!=-1) limited.write(b,0,n);
    }
}
