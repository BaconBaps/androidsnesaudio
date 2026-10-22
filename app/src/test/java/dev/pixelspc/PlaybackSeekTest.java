package dev.pixelspc;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class PlaybackSeekTest {
    private static class Decoder implements PlaybackSeek.Decoder {
        int position,calls,restores,largestStep;
        public int tell() { return position; }
        public void seek(int ms) { largestStep=Math.max(largestStep,ms-position); position=ms; calls++; }
        public void restoreMode() { restores++; }
    }
    @Test public void longSeekYieldsAndCanBeCancelledOrReplaced() throws Exception {
        PlaybackSeek seek=new PlaybackSeek(); Decoder decoder=new Decoder();
        seek.begin(180000); seek.step(decoder);
        assertTrue(seek.active()); assertEquals(250,decoder.position);
        seek.cancel(); seek.step(decoder); assertEquals(1,decoder.calls);
        seek.begin(1000); while(seek.active()) seek.step(decoder);
        assertEquals(1000,decoder.position); assertEquals(250,decoder.largestStep);
        assertEquals(decoder.calls,decoder.restores);
    }
    @Test public void backwardSeekRestoresModeBeforeAdvancing() throws Exception {
        PlaybackSeek seek=new PlaybackSeek(); Decoder decoder=new Decoder(); decoder.position=20000;
        seek.begin(1000); seek.step(decoder);
        assertEquals(0,decoder.position); assertEquals(1,decoder.restores);
        while(seek.active()) seek.step(decoder);
        assertEquals(1000,decoder.position); assertEquals(250,decoder.largestStep);
        seek.begin(0); seek.step(decoder); assertFalse(seek.active()); assertEquals(0,decoder.position);
    }
    @Test public void sampleRoundingCompletesArbitraryTargetsInsteadOfFailing() throws Exception {
        PlaybackSeek seek=new PlaybackSeek();
        Decoder decoder=new Decoder() {
            @Override public void seek(int ms) {
                super.seek(ms);
                // Same 44.1 kHz sample conversion and truncated tell() as GME.
                position=(int)((long)ms*44100/1000*1000/44100);
            }
        };
        for(int target:new int[]{12347,5033,0,9981,9982}) {
            seek.begin(target); int steps=0;
            while(seek.active()) { seek.step(decoder); assertTrue(++steps<100); }
            assertTrue(Math.abs(target-decoder.position)<=1);
        }
    }
    @Test public void stalledDecoderFailsInsteadOfSpinningForever() throws Exception {
        PlaybackSeek seek=new PlaybackSeek(); seek.begin(1000);
        try {
            seek.step(new Decoder() { @Override public void seek(int ms) {} });
            fail("Expected stalled decoder to fail");
        } catch(IOException expected) { assertTrue(expected.getMessage().contains("no progress")); }
    }
}
