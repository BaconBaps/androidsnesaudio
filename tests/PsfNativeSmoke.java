package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class PsfNativeSmoke {
    private static void audible(short[] pcm) { long energy=0; for(short v:pcm)energy+=Math.abs((int)v); if(energy<100) throw new AssertionError("PSF audio is silent"); }
    public static void main(String[] args) throws Exception {
        File dir=new File(args[0]);
        for(String file:new String[]{"tone.psf","mini.minipsf","unicode.psf"}) {
            long h=Engine.open(new File(dir,file).getPath());
            try {
                if(Engine.count(h)!=1 || Engine.length(h,0)!=3000) throw new AssertionError("PSF metadata/timing");
                String[] tags=Engine.info(h,0);if(!tags[3].equals("Sony PlayStation")) throw new AssertionError("Platform metadata");
                if(file.equals("unicode.psf")&&!tags[0].equals("Forêt 🎵")) throw new AssertionError("UTF-8 metadata: "+tags[0]);
                short[] pcm=new short[4096]; Engine.start(h,0,3000);Engine.setLoop(h,true,3000);
                for(int i=0;i<30;i++){Engine.render(h,pcm);try { audible(pcm); } catch(AssertionError e){throw new AssertionError("PSF silent at buffer "+i+" position "+Engine.tell(h),e);}}
                long reference=Engine.open(new File(dir,file).getPath());
                try {
                    Engine.start(reference,0,3000);Engine.setLoop(reference,true,3000);
                    Engine.seek(h,1000);Engine.seek(reference,1000);
                    short[] expected=new short[4096];
                    Engine.render(h,pcm);Engine.render(reference,expected);
                    if(!Arrays.equals(pcm,expected))throw new AssertionError("PSF instance state is not independent: "+Arrays.toString(Arrays.copyOf(pcm,16))+" vs "+Arrays.toString(Arrays.copyOf(expected,16)));
                } finally {Engine.close(reference);}
                long other=Engine.open(new File(dir,"tone.psf").getPath());
                int before=Engine.tell(h);
                try { Engine.start(other,0,3000);Engine.render(other,pcm);audible(pcm);if(Engine.tell(h)!=before)throw new AssertionError("Separate PSF decoder moved playback"); } finally { Engine.close(other); }
                for(boolean loop:new boolean[]{false,true}) {
                    Engine.setLoop(h,loop,3000);
                    for(int target:new int[]{1234,503,0,2399}) { Engine.seek(h,target);if(Math.abs(Engine.tell(h)-target)>1)throw new AssertionError("PSF seek");Engine.render(h,pcm);audible(pcm); }
                }
                Engine.setLoop(h,false,3000);Engine.seek(h,3300);if(!Engine.render(h,pcm))throw new AssertionError("PSF end/fade");
                Engine.setLoop(h,true,3000);if(Engine.render(h,pcm))throw new AssertionError("PSF loop ended");audible(pcm);
                ByteArrayOutputStream wav=new ByteArrayOutputStream();before=Engine.tell(h);
                WavExporter.write(WavExporter.nativeDecoder(new File(dir,file).getPath(),0,3000),wav,new WavExporter.Options(1234,1033,true),p->{});
                if(wav.size()!=44+(1033L*44100/1000)*4 || Engine.tell(h)!=before)throw new AssertionError("PSF WAV export/isolation");
                System.out.println("PASS "+file+" · metadata, sustained PCM, seeks, fade, loop, isolated export");
            } finally {Engine.close(h);}
        }
        File zip=File.createTempFile("psf-import-",".zip"),folder=Files.createTempDirectory("psf-import-").toFile();
        try {
            try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new FileOutputStream(zip))) {
                String[] names={"Game/BASE.PSFLIB","GAME/Mini.MINIPSF"};String[] sources={"base.psflib","mini.minipsf"};
                for(int n=0;n<names.length;n++){out.putNextEntry(new java.util.zip.ZipEntry(names[n]));Files.copy(new File(dir,sources[n]).toPath(),out);out.closeEntry();}
            }
            java.util.List<ArchiveImporter.Entry> entries=ArchiveImporter.extract(zip,"game.zip",folder);
            if(entries.size()!=1)throw new AssertionError("Library became a playable track");
            long h=Engine.open(entries.get(0).file.getPath());try{Engine.start(h,0,3000);short[] pcm=new short[4096];Engine.render(h,pcm);audible(pcm);}finally{Engine.close(h);}
            System.out.println("PASS PSF ZIP dependency import, case-folded paths and library filtering");
        } finally { zip.delete();ArchiveImporter.removeTree(folder); }
        for(String file:new String[]{"missing.minipsf","cycle.minipsf","escape.minipsf","bad.psf"}) {
            try {long h=Engine.open(new File(dir,file).getPath());Engine.close(h);throw new AssertionError("Invalid PSF accepted: "+file);} catch(IOException expected){System.out.println("PASS rejected "+file+": "+expected.getMessage());}
        }
    }
}
