package dev.pixelspc;

import org.junit.Test;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import static org.junit.Assert.*;

public class SpcTimingTest {
    private File file(byte[] tags,int declaredSize) throws Exception {
        File f=File.createTempFile("timing-",".spc");
        ByteBuffer data=ByteBuffer.allocate(66056+tags.length).order(ByteOrder.LITTLE_ENDIAN);
        data.position(66048); data.put(new byte[]{'x','i','d','6'}).putInt(declaredSize).put(tags); Files.write(f.toPath(),data.array()); return f;
    }
    @Test public void readsExplicitLoopLengthPastPaddedStringFields() throws Exception {
        byte[] tags=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).put(new byte[]{1,1,3,0,'A','B',0,0,0x31,4,4,0}).putInt(64000*12).array();
        File f=file(tags,tags.length); try { assertEquals(12000,SpcTiming.loopLength(f)); } finally { f.delete(); }
    }
    @Test public void durationAndIntroTagsDoNotPretendToDeclareALoop() throws Exception {
        byte[] tags=ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(new byte[]{0x30,4,4,0}).putInt(64000*12).array();
        File f=file(tags,tags.length); try { assertEquals(0,SpcTiming.loopLength(f)); } finally { f.delete(); }
    }
    @Test public void rejectsTruncatedLoopDataAndOversizedDeclarations() throws Exception {
        File f=file(new byte[]{0x31,4,4,0,1,2},6);
        try { assertEquals(0,SpcTiming.loopLength(f)); } finally { f.delete(); }
        f=file(new byte[0],Integer.MAX_VALUE);
        try { assertEquals(0,SpcTiming.loopLength(f)); } finally { f.delete(); }
    }
}
