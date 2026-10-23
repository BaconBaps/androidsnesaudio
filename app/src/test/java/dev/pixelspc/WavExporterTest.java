package dev.pixelspc;

import java.io.*;
import java.nio.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WavExporterTest {
    private static class Tone implements WavExporter.Decoder {
        int position,maxStep,seeks; boolean closed;
        public void start() { position=0; }
        public int tell() { return position; }
        public void seek(int ms) { maxStep=Math.max(maxStep,ms-position); position=ms; seeks++; }
        public void render(short[] pcm) { for(int i=0;i<pcm.length;i+=2) { pcm[i]=12345; pcm[i+1]=-23456; } }
        public void close() { closed=true; }
    }
    @Test public void wavHasExactHeaderDurationStereoAndLittleEndianSamples() throws Exception {
        Tone tone=new Tone(); ByteArrayOutputStream out=new ByteArrayOutputStream();
        WavExporter.write(tone,out,new WavExporter.Options(0,1033,false),p->{});
        byte[] wav=out.toByteArray(); ByteBuffer data=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals("RIFF",new String(wav,0,4,"US-ASCII")); assertEquals(wav.length-8,data.getInt(4));
        assertEquals("WAVEfmt ",new String(wav,8,8,"US-ASCII"));
        assertEquals(1,data.getShort(20)); assertEquals(2,data.getShort(22)); assertEquals(44100,data.getInt(24));
        assertEquals(176400,data.getInt(28)); assertEquals(4,data.getShort(32)); assertEquals(16,data.getShort(34));
        assertEquals((1033L*44100/1000)*4,data.getInt(40)); assertEquals(44+data.getInt(40),wav.length);
        assertEquals(12345,data.getShort(44)); assertEquals(-23456,data.getShort(46)); assertTrue(tone.closed);
    }
    @Test public void fadesReachSilenceWithoutChangingDurationOrStereoBalance() throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        WavExporter.write(new Tone(),out,new WavExporter.Options(0,2000,true),p->{});
        byte[] wav=out.toByteArray(); ByteBuffer b=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0,b.getShort(44)); assertEquals(0,b.getShort(46));
        assertEquals(12345,b.getShort(44+44100*4));
        assertEquals(0,b.getShort(wav.length-4)); assertEquals(0,b.getShort(wav.length-2));
    }
    @Test public void seekingIsBoundedAndCancellationClosesDecoderBeforeWritingAudio() throws Exception {
        Tone tone=new Tone() { @Override public void seek(int ms) { super.seek(ms); if(ms>=500) Thread.currentThread().interrupt(); } };
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            WavExporter.write(tone,out,new WavExporter.Options(60000,1000,false),p->{}); fail();
        } catch(InterruptedIOException expected) { assertTrue(tone.closed); assertTrue(tone.maxStep<=250); assertEquals(0,out.size()); }
        finally { Thread.interrupted(); }
    }
    @Test public void outputFailureAndRenderCancellationCloseDecoder() throws Exception {
        Tone tone=new Tone();
        try { WavExporter.write(tone,new OutputStream() { public void write(int b) throws IOException { throw new IOException("full"); } },new WavExporter.Options(0,1000,false),p->{}); fail(); }
        catch(IOException expected) { assertEquals("full",expected.getMessage()); assertTrue(tone.closed); }
        Tone cancelled=new Tone();
        try { WavExporter.write(cancelled,new ByteArrayOutputStream(),new WavExporter.Options(0,1000,false),p->{ if(p>0) Thread.currentThread().interrupt(); }); fail(); }
        catch(InterruptedIOException expected) { assertTrue(cancelled.closed); }
        finally { Thread.interrupted(); }
    }
    @Test public void clipOffsetCompletesInBoundedStepsAndReportsFullProgress() throws Exception {
        Tone tone=new Tone(); final int[] last={-1};
        WavExporter.write(tone,new ByteArrayOutputStream(),new WavExporter.Options(12347,1000,false),p->{ assertTrue(p>=last[0]); last[0]=p; });
        assertEquals(12347,tone.position); assertTrue(tone.maxStep<=250); assertEquals(100,last[0]);
    }
    @Test public void inputsAreBoundedAndFilenamesAreSafe() {
        assertEquals(90000,WavExporter.parseTime("1:30"));
        for(String bad:new String[]{"-1:00","1:60","30","1:2","1:00:00","NaN"}) {
            try { WavExporter.parseTime(bad); fail(bad); } catch(IllegalArgumentException expected) {}
        }
        for(int[] bad:new int[][]{{-1,1000},{3600001,1000},{0,0},{0,600001}}) {
            try { new WavExporter.Options(bad[0],bad[1],false); fail(); } catch(IllegalArgumentException expected) {}
        }
        assertEquals("game_title_track.wav",WavExporter.filename("game/title:track"));
    }
}
