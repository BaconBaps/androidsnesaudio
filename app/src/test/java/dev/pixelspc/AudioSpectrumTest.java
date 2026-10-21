package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;

public class AudioSpectrumTest {
    static short[] tone(int frequency,double amplitude,boolean opposite) {
        short[] pcm=new short[4096]; for(int i=0;i<2048;i++) { short n=(short)(Math.sin(2*Math.PI*frequency*i/44100)*32767*amplitude); pcm[2*i]=n; pcm[2*i+1]=opposite?(short)-n:n; } return pcm;
    }
    static short[] chord() {
        short[] pcm=new short[4096]; int[] tones={80,180,440,1000,2400,5500,11000};
        for(int i=0;i<2048;i++) { double sum=0; for(int j=0;j<tones.length;j++) sum+=Math.sin(2*Math.PI*tones[j]*i/44100)*(.16-j*.015); pcm[i*2]=pcm[i*2+1]=(short)(sum*32767); } return pcm;
    }
    static int strongest(float[] levels) { int max=0; for(int i=1;i<levels.length;i++) if(levels[i]>levels[max]) max=i; return max; }
    @Test public void silenceProducesNoActivity() { for(float level:new AudioSpectrum().analyze(new short[4096])) assertEquals(0,level,0); }
    @Test public void tonesLandInTheCorrectFrequencyBands() {
        AudioSpectrum fft=new AudioSpectrum();
        for(int hz:new int[]{100,1000,6000}) {
            int expected=(int)(Math.log(hz/40.0)/Math.log(350)*24);
            float[] levels=fft.analyze(tone(hz,.6,false));
            assertTrue(Math.abs(strongest(levels)-expected)<=1); assertTrue(levels[strongest(levels)]>.8f);
        }
    }
    @Test public void stereoPhaseDoesNotCancelAndQuieterAudioIsLower() {
        AudioSpectrum fft=new AudioSpectrum(); float[] loud=fft.analyze(tone(1000,.6,true)),quiet=fft.analyze(tone(1000,.02,true));
        assertTrue(loud[strongest(loud)]>.8f); assertTrue(quiet[strongest(quiet)]<loud[strongest(loud)]);
        short[] rightOnly=tone(1000,.6,false); for(int i=0;i<2048;i++) rightOnly[2*i]=0;
        float[] right=fft.analyze(rightOnly); assertTrue(right[strongest(right)]>.8f);
    }
}
