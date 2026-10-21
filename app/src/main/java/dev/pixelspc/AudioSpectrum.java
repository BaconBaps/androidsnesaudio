package dev.pixelspc;

/** Hann-windowed FFT of the actual stereo PCM sent to AudioTrack. No microphone. */
public final class AudioSpectrum {
    public static final int BANDS=24;
    private static final int N=2048;
    private final double[] real=new double[N],imag=new double[N],window=new double[N];
    private final int[] reversed=new int[N];
    public AudioSpectrum() {
        for(int i=0;i<N;i++) { window[i]=.5-.5*Math.cos(2*Math.PI*i/(N-1)); reversed[i]=Integer.reverse(i) >>> 21; }
    }
    public float[] analyze(short[] pcm) {
        if(pcm.length!=N*2) throw new IllegalArgumentException("Expected 2048 stereo frames");
        // Pack left/right as a complex signal; combine mirrored bins below.
        // This preserves energy even for right-only or opposite-phase stereo.
        for(int i=0;i<N;i++) { real[reversed[i]]=pcm[i*2]/32768.0*window[i]; imag[reversed[i]]=pcm[i*2+1]/32768.0*window[i]; }
        for(int size=2;size<=N;size*=2) {
            double angle=-2*Math.PI/size,stepR=Math.cos(angle),stepI=Math.sin(angle);
            for(int base=0;base<N;base+=size) {
                double wr=1,wi=0;
                for(int j=0;j<size/2;j++) {
                    int a=base+j,b=a+size/2;
                    double tr=wr*real[b]-wi*imag[b],ti=wr*imag[b]+wi*real[b];
                    real[b]=real[a]-tr; imag[b]=imag[a]-ti; real[a]+=tr; imag[a]+=ti;
                    double next=wr*stepR-wi*stepI; wi=wr*stepI+wi*stepR; wr=next;
                }
            }
        }
        float[] levels=new float[BANDS];
        for(int band=0;band<BANDS;band++) {
            int from=Math.max(1,(int)(40*Math.pow(350,band/(double)BANDS)*N/44100));
            int to=Math.min(N/2,Math.max(from+1,(int)(40*Math.pow(350,(band+1)/(double)BANDS)*N/44100)));
            double power=0; for(int k=from;k<to;k++) {
                int mirror=N-k;
                power=Math.max(power,(real[k]*real[k]+imag[k]*imag[k]+real[mirror]*real[mirror]+imag[mirror]*imag[mirror])*.25);
            }
            double amplitude=Math.sqrt(power)/(N*.25);
            levels[band]=(float)Math.max(0,Math.min(1,(20*Math.log10(Math.max(amplitude,1e-10))+65)/65));
        }
        return levels;
    }
}
