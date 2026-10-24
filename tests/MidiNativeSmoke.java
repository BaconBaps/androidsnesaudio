package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class MidiNativeSmoke {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long energy(short[] samples){long n=0;for(short v:samples)n+=Math.abs((int)v);return n;}
    public static void main(String[] args)throws Exception {
        File dir=new File(args[0]);String bank=args[1];Engine.validateSoundFont(bank);
        for(String name:new String[]{"tempo.mid","plain.midi","drums.mid","sustain.mid"}) {
            long h=Engine.open(new File(dir,name).getPath());
            try {
                check(Engine.count(h)==1&&Engine.info(h,0)[3].equals("MIDI"),"MIDI grouping");
                if(name.equals("tempo.mid"))check(Engine.length(h,0)==3000&&Engine.info(h,0)[0].equals("MIDI Fixture"),"Tempo map/metadata");
                Engine.soundFont(h,bank);Engine.start(h,0,Engine.length(h,0));short[] pcm=new short[4096];Engine.render(h,pcm);check(energy(pcm)>100,"Silent MIDI "+name);
                Engine.setLoop(h,true,Engine.length(h,0));
                for(int ms:new int[]{700,57,2005,0}){Engine.seek(h,ms);check(Math.abs(Engine.tell(h)-ms)<=1,"MIDI seek position");Engine.render(h,pcm);if(!name.equals("drums.mid"))check(energy(pcm)>100,"Silent seek "+name+" @"+ms);}
                Engine.setLoop(h,false,Engine.length(h,0));Engine.seek(h,Engine.length(h,0)+100);check(Engine.render(h,pcm),"MIDI did not end");
                Engine.start(h,0,Engine.length(h,0));Engine.setLoop(h,true,Engine.length(h,0));Engine.render(h,pcm);int before=Engine.tell(h);
                ByteArrayOutputStream wav=new ByteArrayOutputStream();WavExporter.write(WavExporter.nativeDecoder(new File(dir,name).getPath(),0,Engine.length(h,0),bank),wav,new WavExporter.Options(57,1033,true),p->{});
                check(wav.size()==44+(1033L*44100/1000)*4&&before==Engine.tell(h),"MIDI export/isolation");
                System.out.println("PASS "+name+" · metadata, PCM, tempo, seek, looping, fade/end, isolated WAV export");
            }finally{Engine.close(h);}
        }
        long a=Engine.open(new File(dir,"tempo.mid").getPath()),b=Engine.open(new File(dir,"tempo.mid").getPath());
        try {
            Engine.soundFont(a,bank);Engine.soundFont(b,new File(dir,"inverted.sf2").getPath());Engine.start(a,0,3000);Engine.start(b,0,3000);
            short[] x=new short[4096],y=new short[4096];Engine.render(a,x);Engine.render(b,y);check(energy(x)>100&&energy(y)>100&&!Arrays.equals(x,y),"Bank selection did not change audio");
            Engine.soundFont(a,new File(dir,"inverted.sf2").getPath());Engine.start(a,0,3000);Engine.render(a,x);check(Arrays.equals(x,y),"Bank replacement not independent/deterministic");
            Engine.seek(b,700);Engine.render(b,y);check(energy(y)>100,"Bank replacement broke another decoder");
        }finally{Engine.close(a);Engine.close(b);}
        for(String file:new String[]{"bad.mid","truncated.mid","smpte.mid"})try{long h=Engine.open(new File(dir,file).getPath());Engine.close(h);throw new AssertionError("Accepted "+file);}catch(IOException expected){System.out.println("PASS rejected "+file+": "+expected.getMessage());}
        for(String file:new String[]{"invalid.sf2","bad-index.sf2","bad-offset.sf2","bad-active-loop.sf2"})try{Engine.validateSoundFont(new File(dir,file).getPath());throw new AssertionError("Accepted "+file);}catch(IOException expected){System.out.println("PASS rejected "+file+": "+expected.getMessage());}
        Engine.validateSoundFont(new File(dir,"oneshot-unused-loops.sf2").getPath());
        long oneShot=Engine.open(new File(dir,"oneshot.mid").getPath());
        try {
            Engine.soundFont(oneShot,new File(dir,"oneshot-unused-loops.sf2").getPath());Engine.start(oneShot,0,4000);
            short[] pcm=new short[4096];Engine.render(oneShot,pcm);check(energy(pcm)>100,"One-shot bank became silent");
            System.out.println("PASS one-shot sample with unused reversed loop markers");
        }finally{Engine.close(oneShot);}
        if(args.length>2) {
            long h=Engine.open(args[2]);try {
                check(Engine.length(h,0)==18000,"Demo timing");Engine.soundFont(h,bank);Engine.start(h,0,18000);short[] pcm=new short[4096];long sum=0;boolean ended=false;
                for(int i=0;i<400;i++){ended=Engine.render(h,pcm);sum+=energy(pcm);}check(ended&&sum>10000,"MIDI demo playback");
                System.out.println("PASS original 16-second MIDI demo · full playback and end");
            }finally{Engine.close(h);}
        }
        System.out.println("PASS imported bank selection, replacement and independent MIDI instances");
    }
}
