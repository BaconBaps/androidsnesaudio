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
    @Test public void stalledDecoderFailsInsteadOfSpinningForever() throws Exception {
        PlaybackSeek seek=new PlaybackSeek(); seek.begin(1000);
        try {
            seek.step(new Decoder() { @Override public void seek(int ms) {} });
            fail("Expected stalled decoder to fail");
        } catch(IOException expected) { assertTrue(expected.getMessage().contains("no progress")); }
    }
}
