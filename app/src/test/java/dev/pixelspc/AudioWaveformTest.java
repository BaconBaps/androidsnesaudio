package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;
public class AudioWaveformTest {
    @Test public void silenceIsFlatAndStereoPhaseIsPreserved(){
        for(float value:AudioWaveform.analyze(new short[4096]))assertEquals(0,value,0);
        float[] trace=AudioWaveform.analyze(AudioSpectrumTest.tone(440,.6,true));float peak=0;
        for(int i=0;i<AudioWaveform.POINTS;i++){assertEquals(trace[i*2],-trace[i*2+1],.0001);peak=Math.max(peak,Math.abs(trace[i*2]));}assertTrue(peak>.5);
    }
    @Test public void rightOnlyTraceIsVisibleAndInputIsNotChanged(){
        short[] pcm=AudioSpectrumTest.tone(440,.6,false);for(int i=0;i<2048;i++)pcm[i*2]=0;short[] original=pcm.clone();
        float[] trace=AudioWaveform.analyze(pcm);float peak=0;for(int i=0;i<AudioWaveform.POINTS;i++){assertEquals(0,trace[i*2],0);peak=Math.max(peak,Math.abs(trace[i*2+1]));}assertTrue(peak>.5);assertArrayEquals(original,pcm);
    }
}
