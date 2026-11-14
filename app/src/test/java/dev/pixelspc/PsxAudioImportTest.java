package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;
public class PsxAudioImportTest {
    @Test public void psxStreamExtensionsAndLargeFileBudgetsAreScoped(){
        assertTrue(ArchiveImporter.supported("track.XA"));assertTrue(ArchiveImporter.supported("sample.VAG"));assertFalse(ArchiveImporter.supported("disc.bin"));
        assertEquals(128L*1024*1024,ArchiveImporter.fileLimit("track.xa"));assertEquals(128L*1024*1024,ArchiveImporter.fileLimit("sample.vag"));assertEquals(32L*1024*1024,ArchiveImporter.fileLimit("track.spc"));
    }
    @Test public void increasedPsxLimitKeepsPerFileAndTotalBudgets()throws Exception{
        ArchiveImporter.Budget budget=new ArchiveImporter.Budget();ArchiveImporter.LimitedOutput out=new ArchiveImporter.LimitedOutput(new ArchiveImporter.Discard(),budget,ArchiveImporter.fileLimit("sample.xa"));
        out.charge(64*1024*1024);out.charge(64*1024*1024);
        try{out.charge(1);fail();}catch(java.io.IOException expected){}
        ArchiveImporter.LimitedOutput ordinary=new ArchiveImporter.LimitedOutput(new ArchiveImporter.Discard(),new ArchiveImporter.Budget());
        try{ordinary.charge(32*1024*1024+1);fail();}catch(java.io.IOException expected){}
        budget.total=ArchiveImporter.TOTAL_LIMIT;out=new ArchiveImporter.LimitedOutput(new ArchiveImporter.Discard(),budget,ArchiveImporter.fileLimit("sample.vag"));try{out.charge(1);fail();}catch(java.io.IOException expected){}
    }
}
