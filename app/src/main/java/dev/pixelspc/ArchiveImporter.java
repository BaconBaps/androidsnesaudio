package dev.pixelspc;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;

/** Bounded extraction: GME files are flat; PSF dependency trees stay inside the import. */
public final class ArchiveImporter {
    static final long FILE_LIMIT=32L*1024*1024, TOTAL_LIMIT=256L*1024*1024;
    public static final int TRACK_LIMIT=1000;
    static final Set<String> FORMATS=new HashSet<>(Arrays.asList("spc","nsf","nsfe","gbs","vgm","vgz","gym","hes","kss","ay","sap","psf","minipsf"));
    public static boolean supported(String name) { return FORMATS.contains(extension(name)); }
    static boolean dependency(String name) { return extension(name).equals("psflib"); }
    static String extension(String name) { int p=name.lastIndexOf('.'); return p<0?"":name.substring(p+1).toLowerCase(Locale.ROOT); }
    public static final class Entry {
        public final File file; public final String name;
        Entry(File f,String n) { file=f; name=n; }
    }
    public static List<Entry> extract(File input,String name,File destination) throws Exception {
        if(!destination.isDirectory() && !destination.mkdirs()) throw new IOException("Cannot create library folder");
        List<Entry> result=new ArrayList<>(); Budget budget=new Budget(); Set<String> psfPaths=new HashSet<>();
        try {
            String ext=extension(name);
            if(ext.equals("zip")) {
                try(ZipInputStream zip=new ZipInputStream(new FileInputStream(input))) {
                    ZipEntry e; int entries=0;
                    while((e=zip.getNextEntry())!=null) {
                        if(++entries>3000) throw new IOException("Too many archive entries");
                        if(!e.isDirectory() && (supported(e.getName()) || dependency(e.getName()))) {
                            Entry item=storedEntry(destination,e.getName(),result.size(),psfPaths);
                            try(OutputStream out=new FileOutputStream(item.file)) { copy(zip,out,budget); }
                            if(!dependency(e.getName())) result.add(item); if(result.size()>TRACK_LIMIT) throw new IOException("Too many tracks");
                        } else { copy(zip,new Discard(),budget); }
                        zip.closeEntry();
                    }
                }
            } else if(ext.equals("rsn") || ext.equals("rar")) {
                try(RarReader rar=new RarReader(input)) {
                    int entries=0; String raw;
                    while((raw=rar.next())!=null) {
                        if(++entries>3000) throw new IOException("Too many archive entries");
                        if(rar.size()>FILE_LIMIT || rar.size()<0) throw new IOException("Archive entry is too large");
                        boolean music=supported(raw),library=dependency(raw);
                        Entry item=music||library?storedEntry(destination,raw,result.size(),psfPaths):null;
                        try(OutputStream out=item==null?new Discard():new FileOutputStream(item.file)) { copy(rar,out,budget); }
                        if(music) { result.add(item); if(result.size()>TRACK_LIMIT) throw new IOException("Too many tracks"); }
                    }
                }
            } else if(supported(name)) {
                Entry item=storedEntry(destination,name,0,psfPaths);
                try(InputStream in=new FileInputStream(input); OutputStream out=new FileOutputStream(item.file)) { copy(in,out,budget); }
                result.add(item);
            } else throw new IOException("Choose an SPC, RSN, PSF, ZIP, or supported chiptune file");
            if(result.isEmpty()) throw new IOException("No supported music files found in this set");
            result.sort(Comparator.comparing(e->e.name.toLowerCase(Locale.ROOT)));
            return result;
        } catch(Exception e) {
            removeTree(destination); throw e;
        }
    }
    static void removeTree(File directory) throws IOException {
        if(!directory.exists()) return;
        try(java.util.stream.Stream<Path> paths=Files.walk(directory.toPath())) {
            for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }
    private static Entry storedEntry(File directory,String raw,int index,Set<String> seen) throws IOException {
        String ext=extension(raw);
        if(!ext.equals("psf") && !ext.equals("minipsf") && !ext.equals("psflib")) return entry(directory,raw,index);
        String normalized=raw.replace('\\','/');
        if(normalized.startsWith("/") || normalized.indexOf(':')>=0 || normalized.indexOf('\0')>=0) throw new IOException("Invalid PSF archive path");
        StringBuilder relative=new StringBuilder();
        for(String part:normalized.split("/")) {
            if(part.equals("..")) throw new IOException("PSF archive path escapes its import");
            if(part.isEmpty() || part.equals(".")) continue;
            if(part.length()>240 || relative.length()>1000) throw new IOException("PSF archive path is too long");
            if(relative.length()>0) relative.append('/'); relative.append(part);
        }
        if(!seen.add(relative.toString().toLowerCase(Locale.ROOT))) throw new IOException("Duplicate PSF archive path");
        StringBuilder stored=new StringBuilder(relative);
        for(int n=0;n<stored.length();n++) { char c=stored.charAt(n); if(c>='A' && c<='Z') stored.setCharAt(n,(char)(c+32)); }
        File root=new File(directory,"psf").getCanonicalFile(),file=new File(root,stored.toString()).getCanonicalFile();
        if(!file.getPath().startsWith(root.getPath()+File.separator)) throw new IOException("PSF archive path escapes its import");
        if(!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) throw new IOException("Cannot create PSF library folder");
        return new Entry(file,normalized.substring(normalized.lastIndexOf('/')+1));
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
