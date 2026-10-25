package dev.pixelspc;

import java.io.*;

/** Bounded-memory offline rendering, independent of the playback decoder/audio device. */
final class WavExporter {
    static final int RATE=44100,MAX_DURATION_MS=600000,MAX_START_MS=3600000;
    interface Decoder extends AutoCloseable {
        void start() throws IOException;
        int tell();
        void seek(int ms) throws IOException;
        void render(short[] pcm) throws IOException;
        @Override void close();
    }
    interface Progress { void update(int percent); }
    static final class Options {
        final int startMs,durationMs;
        final boolean fade;
        Options(int startMs,int durationMs,boolean fade) {
            if(startMs<0 || startMs>MAX_START_MS) throw new IllegalArgumentException("Start must be between 0:00 and 60:00.");
            if(durationMs<1000 || durationMs>MAX_DURATION_MS) throw new IllegalArgumentException("Duration must be between 0:01 and 10:00.");
            this.startMs=startMs; this.durationMs=durationMs; this.fade=fade;
        }
    }
    static Decoder nativeDecoder(String path,int index,int length) throws IOException {
        return nativeDecoder(path,index,length,null);
    }
    static Decoder nativeDecoder(String path,int index,int length,String soundFont) throws IOException {
        final long handle=Engine.open(path);
        try { if(soundFont!=null) Engine.soundFont(handle,soundFont); } catch(IOException e) { Engine.close(handle);throw e; }
        return new Decoder() {
            public void start() throws IOException { Engine.start(handle,index,length); Engine.setLoop(handle,true,length); }
            public int tell() { return Engine.tell(handle); }
            public void seek(int ms) throws IOException { Engine.seek(handle,ms); Engine.setLoop(handle,true,length); }
            public void render(short[] pcm) throws IOException { Engine.render(handle,pcm); }
            public void close() { Engine.close(handle); }
        };
    }
    static void checkCancelled() throws InterruptedIOException {
        if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Export cancelled");
    }
    static long dataSize(Options options) { return (long)options.durationMs*RATE/1000*4; }
    static void write(Decoder decoder,OutputStream output,Options options,Progress progress) throws IOException {
        try(Decoder owned=decoder) {
            checkCancelled(); owned.start(); progress.update(0);
            PlaybackSeek seek=new PlaybackSeek(); seek.begin(options.startMs);
            while(seek.active()) {
                checkCancelled(); seek.step(new PlaybackSeek.Decoder() {
                    public int tell() { return owned.tell(); }
                    public void seek(int ms) throws IOException { owned.seek(ms); }
                    public void restoreMode() { /* Native adapter restores fade/silence settings. */ }
                });
            }
            checkCancelled();
            long frames=(long)options.durationMs*RATE/1000;
            header(output,dataSize(options));
            short[] pcm=new short[4096]; byte[] bytes=new byte[8192];
            int previous=-1;
            for(long frame=0;frame<frames;) {
                checkCancelled(); owned.render(pcm);
                int count=(int)Math.min(pcm.length/2,frames-frame);
                for(int i=0;i<count;i++) {
                    long at=frame+i;
                    double gain=options.fade?Math.min(1,Math.min(at/(RATE*.05),(frames-1-at)/(double)Math.min(RATE,frames/2))):1;
                    for(int channel=0;channel<2;channel++) {
                        int sample=(int)Math.round(pcm[i*2+channel]*gain),offset=i*4+channel*2;
                        bytes[offset]=(byte)sample; bytes[offset+1]=(byte)(sample>>8);
                    }
                }
                output.write(bytes,0,count*4); frame+=count;
                int percent=(int)(frame*100/frames);
                if(percent!=previous) { progress.update(percent); previous=percent; }
            }
        }
    }
    private static void ascii(OutputStream out,String value) throws IOException { out.write(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII)); }
    private static void le(OutputStream out,long value,int count) throws IOException { for(int i=0;i<count;i++) out.write((int)(value>>(i*8))&255); }
    private static void header(OutputStream out,long size) throws IOException {
        ascii(out,"RIFF"); le(out,size+36,4); ascii(out,"WAVEfmt "); le(out,16,4);
        le(out,1,2); le(out,2,2); le(out,RATE,4); le(out,RATE*4,4); le(out,4,2); le(out,16,2);
        ascii(out,"data"); le(out,size,4);
    }
    static int parseTime(String value) {
        String[] parts=value.trim().split(":",-1);
        try {
            if(parts.length!=2 || !parts[0].matches("[0-9]{1,2}") || !parts[1].matches("[0-9]{2}")) throw new NumberFormatException();
            int seconds=Integer.parseInt(parts[1]);
            if(seconds>59) throw new NumberFormatException();
            return (Integer.parseInt(parts[0])*60+seconds)*1000;
        } catch(NumberFormatException e) { throw new IllegalArgumentException("Use minutes:seconds, for example 0:30."); }
    }
    static String filename(String title) {
        String clean=title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").trim();
        if(clean.length()>80) clean=clean.substring(0,80);
        return (clean.isEmpty()?"ChipAmp9x export":clean)+".wav";
    }
}
