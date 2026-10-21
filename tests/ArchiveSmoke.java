package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
public class ArchiveSmoke {
    public static void main(String[] args) throws Exception {
        File root=Files.createTempDirectory("pixelspc-test").toFile();
        for(int i=0;i<args.length;i++) {
            File source=new File(args[i]); File dest=new File(root,"sample"+i);
            List<ArchiveImporter.Entry> tracks=ArchiveImporter.extract(source,source.getName(),dest);
            if(tracks.size()<2) throw new AssertionError("Set should contain multiple tracks");
            for(ArchiveImporter.Entry t:tracks) {
                byte[] bytes=Files.readAllBytes(t.file.toPath());
                if(!new String(bytes,0,27,"US-ASCII").startsWith("SNES-SPC700 Sound File Data")) throw new AssertionError("Not an SPC: "+t.name);
            }
            System.out.println(source.getName()+": "+tracks.size()+" valid SPC files extracted to "+dest);
        }
        File zip=new File(root,"traversal.zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("../../escaped.spc")); out.write(new byte[]{1,2,3}); out.closeEntry();
        }
        File dir=new File(root,"safe"); List<ArchiveImporter.Entry> flat=ArchiveImporter.extract(zip,"set.zip",dir);
        if(!flat.get(0).file.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile()) || new File(root,"escaped.spc").exists()) throw new AssertionError("Path traversal");
        System.out.println("PASS: archive paths cannot escape destination");
        File bomb=new File(root,"bomb.zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(bomb))) { out.putNextEntry(new ZipEntry("huge.spc")); byte[] b=new byte[1024*1024]; for(int i=0;i<33;i++) out.write(b); out.closeEntry(); }
        File bombDest=new File(root,"bomb"); boolean rejected=false;
        try { ArchiveImporter.extract(bomb,"bomb.zip",bombDest); } catch(IOException expected) { rejected=true; }
        if(!rejected || bombDest.exists()) throw new AssertionError("Oversized entry not cleaned up");
        System.out.println("PASS: oversized extraction rejected and rolled back");
        File invalid=new File(root,"broken.rsn"); Files.write(invalid.toPath(),new byte[]{0,1,2}); rejected=false;
        try { ArchiveImporter.extract(invalid,"broken.rsn",new File(root,"invalid")); } catch(Exception expected) { rejected=true; }
        if(!rejected) throw new AssertionError("Corrupt RSN accepted");
        System.out.println("PASS: corrupt RSN rejected");
        Files.walk(root.toPath()).sorted(Comparator.reverseOrder()).forEach(p->{try { Files.delete(p); } catch(IOException e) {throw new UncheckedIOException(e);} });
    }
}
