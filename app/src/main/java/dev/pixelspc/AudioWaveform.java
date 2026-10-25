package dev.pixelspc;

/** A triggered stereo trace from decoded PCM; never uses microphone input. */
final class AudioWaveform {
    static final int POINTS=128,SAMPLES=POINTS*2;
    static float[] analyze(short[] pcm) {
        if(pcm.length!=4096)throw new IllegalArgumentException("Expected 2048 stereo frames");
        long left=0,right=0;for(int i=0;i<2048;i++){left+=Math.abs((int)pcm[i*2]);right+=Math.abs((int)pcm[i*2+1]);}
        int channel=right>left?1:0,start=0;
        // Trigger on the stronger channel so anti-phase stereo does not cancel.
        for(int i=1;i<1024;i++)if(pcm[(i-1)*2+channel]<=0&&pcm[i*2+channel]>0){start=i;break;}
        float[] trace=new float[SAMPLES];
        for(int i=0;i<POINTS;i++)for(int c=0;c<2;c++){
            int sum=0;for(int j=0;j<4;j++)sum+=pcm[(start+i*4+j)*2+c];
            trace[i*2+c]=sum/(4f*32768f);
        }
        return trace;
    }
}
