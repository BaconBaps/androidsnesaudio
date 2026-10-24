package dev.pixelspc;
import org.junit.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
public class ArchiveImporterTest {
    private File root;
    @Before public void before() throws Exception { root=Files.createTempDirectory("archive-test").toFile(); }
    @After public void after() throws Exception { Files.walk(root.toPath()).sorted(Comparator.reverseOrder()).forEach(p->{try { Files.delete(p); } catch(IOException e) { throw new UncheckedIOException(e); } }); }
    @Test public void archivePathsAreFlattenedAndNamesArePreserved() throws Exception {
        File zip=new File(root,"set.zip");
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(zip))) {
            for(String n:new String[]{"../../B.spc","nested/A.SPC"}) { z.putNextEntry(new ZipEntry(n)); z.write(new byte[]{1,2,3}); z.closeEntry(); }
        }
        File dest=new File(root,"set"); List<ArchiveImporter.Entry> entries=ArchiveImporter.extract(zip,"set.ZIP",dest);
        assertEquals(2,entries.size()); assertEquals("A.SPC",entries.get(0).name);
        for(ArchiveImporter.Entry e:entries) assertEquals(dest.getCanonicalFile(),e.file.getCanonicalFile().getParentFile());
        assertFalse(new File(root,"B.spc").exists());
    }
    @Test public void psfTreesPreserveLibrariesAndOnlyReturnPlayableTracks() throws Exception {
        File zip=new File(root,"psf.zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))) {
            for(String name:new String[]{"Game/LIB/Base.PSFLIB","GAME/Tracks/One.MINIPSF","other.spc"}) {
                out.putNextEntry(new ZipEntry(name)); out.write(new byte[]{1,2,3}); out.closeEntry();
            }
        }
        File dest=new File(root,"set"); List<ArchiveImporter.Entry> tracks=ArchiveImporter.extract(zip,"psf.zip",dest);
        assertEquals(2,tracks.size()); assertTrue(new File(dest,"psf/game/lib/base.psflib").isFile());
        ArchiveImporter.Entry mini=tracks.stream().filter(e->e.name.equals("One.MINIPSF")).findFirst().get();
        assertTrue(mini.file.getPath().endsWith("psf/game/tracks/one.minipsf"));
        assertFalse(ArchiveImporter.supported("base.psflib")); assertTrue(ArchiveImporter.supported("track.MINIPSF"));
    }
    @Test public void usfLibrariesKeepRelativePathsAndAreNotPlayableEntries() throws Exception {
        File zip=new File(root,"n64.zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){for(String name:new String[]{"Game/LIB/Base.USFLIB","GAME/Tracks/One.MINIUSF","Two.USF"}){out.putNextEntry(new ZipEntry(name));out.write(1);out.closeEntry();}}
        File dest=new File(root,"set");List<ArchiveImporter.Entry> tracks=ArchiveImporter.extract(zip,"n64.zip",dest);
        assertEquals(2,tracks.size());assertTrue(new File(dest,"psf/game/lib/base.usflib").isFile());assertTrue(new File(dest,"psf/game/tracks/one.miniusf").isFile());
        assertFalse(ArchiveImporter.supported("base.USFLIB"));assertTrue(ArchiveImporter.dependency("base.USFLIB"));assertTrue(ArchiveImporter.supported("track.USF"));assertTrue(ArchiveImporter.supported("track.MINIUSF"));
        File bad=new File(root,"bad-usf.zip");try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(bad))){out.putNextEntry(new ZipEntry("../Base.USFLIB"));out.write(1);out.closeEntry();}
        File rejected=new File(root,"rejected");try{ArchiveImporter.extract(bad,"bad-usf.zip",rejected);fail();}catch(IOException expected){}assertFalse(rejected.exists());
    }
    @Test public void psfTraversalAndCaseCollisionsRollBackTheWholeTree() throws Exception {
        for(String bad:new String[]{"../../outside.psflib","/outside.psf","C:/outside.minipsf","Game/A.PSF"}) {
            File zip=new File(root,"bad.zip");
            try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("game/a.psf"));out.write(1);out.closeEntry();
                out.putNextEntry(new ZipEntry(bad));out.write(1);out.closeEntry();
            }
            File dest=new File(root,"set");
            try { ArchiveImporter.extract(zip,"bad.zip",dest); fail(bad); } catch(IOException expected) {}
            assertFalse(dest.exists());
        }
        assertFalse(new File(root,"outside.psflib").exists());
    }
    @Test public void aLibraryOnlyZipIsNotImportedAsPlayableMusic() throws Exception {
        File zip=new File(root,"library.zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))) { out.putNextEntry(new ZipEntry("folder/base.psflib"));out.write(1);out.closeEntry(); }
        File dest=new File(root,"set");
        try { ArchiveImporter.extract(zip,"library.zip",dest); fail(); } catch(IOException expected) { assertTrue(expected.getMessage().contains("No supported music")); }
        assertFalse(dest.exists());
    }
    @Test public void trackLimitRollsBackPartialImport() throws Exception {
        File zip=new File(root,"many.zip");
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(zip))) { for(int i=0;i<1001;i++) { z.putNextEntry(new ZipEntry(i+".spc")); z.write(0); z.closeEntry(); } }
        File dest=new File(root,"set");
        try { ArchiveImporter.extract(zip,"many.zip",dest); fail("Track limit must reject"); } catch(IOException expected) { assertTrue(expected.getMessage().contains("Too many tracks")); }
        assertFalse(dest.exists());
    }
    @Test public void emptyAndUnsupportedArchivesFailClearly() throws Exception {
        File zip=new File(root,"empty.zip"); try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(zip))) { z.putNextEntry(new ZipEntry("readme.txt")); z.write("hello".getBytes("UTF-8")); z.closeEntry(); }
        File dest=new File(root,"set");
        try { ArchiveImporter.extract(zip,"empty.zip",dest); fail("No music should reject"); } catch(IOException expected) { assertTrue(expected.getMessage().contains("No supported music")); }
        assertFalse(dest.exists());
    }
}
