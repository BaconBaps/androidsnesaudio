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
