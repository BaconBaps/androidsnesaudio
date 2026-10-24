package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
public final class RarNativeSmoke {
    private static String sha(File file) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}
        StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format("%02x",b&255));return out.toString();
    }
    public static void main(String[] args) throws Exception {
        File dir=Files.createTempDirectory("rsn-check-").toFile();
        try {
            List<ArchiveImporter.Entry> tracks=ArchiveImporter.extract(new File(args[0]),"set.rsn",dir);
            if(tracks.size()!=36)throw new AssertionError("Expected all 36 supplied RSN tracks");
            if(args.length>1){Map<String,String> expected=new HashMap<>();for(String line:Files.readAllLines(Paths.get(args[1]))){String[] item=line.split("\\t");if(item[0].toLowerCase().endsWith(".spc"))expected.put(item[0],item[2]);}for(ArchiveImporter.Entry e:tracks)if(!sha(e.file).equals(expected.remove(e.name)))throw new AssertionError("RAR contents differ: "+e.name);if(!expected.isEmpty())throw new AssertionError("Missing tracks");}
            System.out.println("PASS Android JNI/unarr RSN reader: "+tracks.size()+" tracks; SHA-256 matches original Junrar extraction");
        } finally {ArchiveImporter.removeTree(dir);}
        File bad=File.createTempFile("bad-rar-",".rsn");File dest=new File(bad.getParentFile(),bad.getName()+"-set");
        try {Files.write(bad.toPath(),new byte[]{'R','a','r','!',0x1a,7,0,0});try{ArchiveImporter.extract(bad,"bad.rsn",dest);throw new AssertionError("Corrupt RAR accepted");}catch(IOException expected){if(dest.exists())throw new AssertionError("Failed import was not removed");}}finally{bad.delete();ArchiveImporter.removeTree(dest);}
    }
}
