package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class NativeSmoke {
    private static void checkContinuousLoop(File file) throws Exception {
        long reference=Engine.open(file.getAbsolutePath()),toggled=Engine.open(file.getAbsolutePath());
        try {
            Engine.start(reference,0,1000); Engine.setLoop(reference,true,1000);
            Engine.start(toggled,0,1000);
            short[] a=new short[4096],b=new short[4096];
            for(int block=0;block<260;block++) {
                if(block==5) { int before=Engine.tell(toggled); Engine.setLoop(toggled,true,1000); if(Engine.tell(toggled)!=before) throw new AssertionError("Loop toggle changed position"); }
                boolean endedA=Engine.render(reference,a),endedB=Engine.render(toggled,b);
                if(endedA || endedB || !Arrays.equals(a,b)) throw new AssertionError("Looping interrupted or faded PCM at block "+block);
            }
            if(Engine.tell(toggled)<12000) throw new AssertionError("Did not pass tagged end and fade boundary");
            Engine.setLoop(toggled,false,1000);
            boolean ended=false; for(int i=0;i<260;i++) ended|=Engine.render(toggled,b);
            if(!ended) throw new AssertionError("Disabling loop did not restore timed ending");
            // Backwards seeks reset GME track variables; the playback service reapplies the mode.
            Engine.seek(reference,0); Engine.setLoop(reference,true,1000);
            for(int i=0;i<260;i++) if(Engine.render(reference,a)) throw new AssertionError("Seek lost continuous loop mode");
            System.out.println("PASS: "+file.getName()+" continuous PCM matches uninterrupted reference beyond tagged end; loop-off fade and seek reapplication verified");
        } finally { Engine.close(reference); Engine.close(toggled); }
    }
    public static void main(String[] args) throws Exception {
        File root=Files.createTempDirectory("pixelspc-native").toFile(); int count=0;
        try {
            for(int n=0;n<args.length;n++) {
                File source=new File(args[n]); List<ArchiveImporter.Entry> entries=ArchiveImporter.extract(source,source.getName(),new File(root,"set"+n));
                for(ArchiveImporter.Entry entry:entries) {
                    long h=Engine.open(entry.file.getAbsolutePath());
                    try {
                        if(Engine.count(h)!=1) throw new AssertionError("Expected single-song SPC");
                        String[] info=Engine.info(h,0); if(info.length!=5) throw new AssertionError("Missing metadata");
                        Engine.start(h,0,Engine.length(h,0)); short[] pcm=new short[44100*2*4]; Engine.render(h,pcm);
                        int lo=0,hi=0; for(short s:pcm) { lo=Math.min(lo,s); hi=Math.max(hi,s); }
                        if(lo>=0 || hi<=0) throw new AssertionError("Silent audio: "+entry.name);
                        Engine.seek(h,15000); if(Math.abs(Engine.tell(h)-15000)>50) throw new AssertionError("Seek failed"); Engine.render(h,new short[4096]); count++;
                    } finally { Engine.close(h); }
                }
                System.out.println("PASS: "+source.getName()+" — "+entries.size()+" songs imported, metadata read, PCM rendered and seek verified via JNI");
                checkContinuousLoop(entries.get(Math.min(2,entries.size()-1)).file);
            }
            File corrupt=new File(root,"invalid.spc"); Files.write(corrupt.toPath(),new byte[]{1,2,3});
            boolean rejected=false; try { long h=Engine.open(corrupt.getAbsolutePath()); Engine.close(h); } catch(IOException expected) { rejected=true; }
            if(!rejected) throw new AssertionError("Invalid SPC accepted");
            System.out.println("PASS: "+count+" real tracks decoded through app JNI; invalid SPC rejected");
        } finally { Files.walk(root.toPath()).sorted(Comparator.reverseOrder()).forEach(p->{try { Files.delete(p); } catch(IOException e) { throw new UncheckedIOException(e); } }); }
    }
}
