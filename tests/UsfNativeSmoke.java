package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class UsfNativeSmoke {
    private static void audible(short[] pcm) { long energy=0; for(short v:pcm)energy+=Math.abs((int)v); if(energy<100) throw new AssertionError("USF audio is silent"); }
    public static void main(String[] args) throws Exception {
        File dir=new File(args[0]);
        for(String file:new String[]{"tone.usf","mini.miniusf","unicode.usf","aux.miniusf","zlib-empty.usf","cp1252.usf"}) {
            long h=Engine.open(new File(dir,file).getPath());
            try {
                if(Engine.count(h)!=1 || Engine.length(h,0)!=3000) throw new AssertionError("USF metadata/timing");
                String[] tags=Engine.info(h,0);if(!tags[3].equals("Nintendo 64")) throw new AssertionError("Platform metadata");
                if(file.equals("cp1252.usf")&&!tags[0].equals("Forêt"))throw new AssertionError("CP1252 metadata");
                if(file.equals("unicode.usf")&&!tags[0].equals("Forêt 🎵")) throw new AssertionError("UTF-8 metadata: "+tags[0]);
                short[] pcm=new short[4096]; Engine.start(h,0,3000);Engine.setLoop(h,true,3000);
                for(int i=0;i<30;i++){Engine.render(h,pcm);try { audible(pcm); } catch(AssertionError e){throw new AssertionError("USF silent at buffer "+i+" position "+Engine.tell(h),e);}}
                long reference=Engine.open(new File(dir,file).getPath());
                try {
                    Engine.start(reference,0,3000);Engine.setLoop(reference,true,3000);
                    Engine.seek(h,1000);Engine.seek(reference,1000);
                    short[] expected=new short[4096];
                    Engine.render(h,pcm);Engine.render(reference,expected);
                    if(!Arrays.equals(pcm,expected))throw new AssertionError("USF instance state is not independent: "+Arrays.toString(Arrays.copyOf(pcm,16))+" vs "+Arrays.toString(Arrays.copyOf(expected,16)));
                } finally {Engine.close(reference);}
                long other=Engine.open(new File(dir,"tone.usf").getPath());
                int before=Engine.tell(h);
                try { Engine.start(other,0,3000);Engine.render(other,pcm);audible(pcm);if(Engine.tell(h)!=before)throw new AssertionError("Separate USF decoder moved playback"); } finally { Engine.close(other); }
                for(boolean loop:new boolean[]{false,true}) {
                    Engine.setLoop(h,loop,3000);
                    for(int target:new int[]{1234,503,0,2399}) { Engine.seek(h,target);if(Math.abs(Engine.tell(h)-target)>1)throw new AssertionError("USF seek");Engine.render(h,pcm);audible(pcm); }
                }
                Engine.setLoop(h,false,3000);Engine.seek(h,3300);if(!Engine.render(h,pcm))throw new AssertionError("USF end/fade");
                Engine.setLoop(h,true,3000);if(Engine.render(h,pcm))throw new AssertionError("USF loop ended");audible(pcm);
                ByteArrayOutputStream wav=new ByteArrayOutputStream();before=Engine.tell(h);
                WavExporter.write(WavExporter.nativeDecoder(new File(dir,file).getPath(),0,3000),wav,new WavExporter.Options(1234,1033,true),p->{});
                if(wav.size()!=44+(1033L*44100/1000)*4 || Engine.tell(h)!=before)throw new AssertionError("USF WAV export/isolation");
                System.out.println("PASS "+file+" · metadata, sustained PCM, seeks, fade, loop, isolated export");
            } finally {Engine.close(h);}
        }
        long unknownTime=Engine.open(new File(dir,"bad-time.usf").getPath());try{if(Engine.length(unknownTime,0)!=185000)throw new AssertionError("Optional USF timing fallback");}finally{Engine.close(unknownTime);}System.out.println("PASS USF optional timing fallback");
        File zip=File.createTempFile("usf-import-",".zip"),folder=Files.createTempDirectory("usf-import-").toFile();
        try {
            try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new FileOutputStream(zip))) {
                String[] names={"Game/BASE.USFLIB","GAME/Mini.MINIUSF"};String[] sources={"base.usflib","mini.miniusf"};
                for(int n=0;n<names.length;n++){out.putNextEntry(new java.util.zip.ZipEntry(names[n]));Files.copy(new File(dir,sources[n]).toPath(),out);out.closeEntry();}
            }
            java.util.List<ArchiveImporter.Entry> entries=ArchiveImporter.extract(zip,"game.zip",folder);
            if(entries.size()!=1)throw new AssertionError("Library became a playable track");
            long h=Engine.open(entries.get(0).file.getPath());try{Engine.start(h,0,3000);short[] pcm=new short[4096];Engine.render(h,pcm);audible(pcm);}finally{Engine.close(h);}
            System.out.println("PASS USF ZIP dependency import, case-folded paths and library filtering");
        } finally { zip.delete();ArchiveImporter.removeTree(folder); }
        for(String file:new String[]{"missing.miniusf","cycle.miniusf","escape.miniusf","bad.usf","truncated.usf","empty.usf","bad-ram.usf","bad-crc.usf"}) {
            try {long h=Engine.open(new File(dir,file).getPath());Engine.close(h);throw new AssertionError("Invalid USF accepted: "+file);} catch(IOException expected){System.out.println("PASS rejected "+file+": "+expected.getMessage());}
        }
        long invalidDma=Engine.open(new File(dir,"bad-dma.usf").getPath());
        try {Engine.start(invalidDma,0,3000);Engine.render(invalidDma,new short[4096]);throw new AssertionError("Invalid DMA accepted");}
        catch(IOException expected){if(!expected.getMessage().contains("outside RDRAM"))throw expected;System.out.println("PASS rejected invalid N64 audio DMA");}finally{Engine.close(invalidDma);}
        java.util.concurrent.ExecutorService parallel=java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<short[]> render=()->{long h=Engine.open(new File(dir,"tone.usf").getPath());try{Engine.start(h,0,3000);Engine.seek(h,1234);short[] out=new short[4096];Engine.render(h,out);return out;}finally{Engine.close(h);}};
        try {java.util.concurrent.Future<short[]> a=parallel.submit(render),b=parallel.submit(render);if(!Arrays.equals(a.get(),b.get()))throw new AssertionError("Concurrent USF decoder states diverged");}finally{parallel.shutdownNow();}
        System.out.println("PASS concurrent N64 decoders produce identical independent output");
        long stuck=Engine.open(new File(dir,"stuck.usf").getPath());long before=System.nanoTime();
        try {Engine.start(stuck,0,3000);Engine.render(stuck,new short[4096]);throw new AssertionError("No-audio CPU loop did not fail");}
        catch(IOException expected){if(!expected.getMessage().contains("execution limit"))throw expected;System.out.println("PASS no-audio watchdog: "+expected.getMessage());}
        finally{Engine.close(stuck);}
        if((System.nanoTime()-before)>10_000_000_000L)throw new AssertionError("USF watchdog was too slow");
        long recovered=Engine.open(new File(dir,"tone.usf").getPath());try{Engine.start(recovered,0,3000);short[] pcm=new short[4096];Engine.render(recovered,pcm);audible(pcm);}finally{Engine.close(recovered);}
        System.out.println("PASS independent decoder recovery after invalid N64 program");

    }
}
