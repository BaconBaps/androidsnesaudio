package dev.pixelspc;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.*;

/** Real JNI export with independent live decoder, run on the host without Android. */
public class NativeExportSmoke {
    private static void check(Path path) throws Exception {
        long playing=Engine.open(path.toString());
        try {
            Engine.start(playing,0,180000); Engine.setLoop(playing,true,180000);
            Engine.render(playing,new short[4096]); int before=Engine.tell(playing);
            for(int offset:new int[]{0,12347}) {
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                WavExporter.write(WavExporter.nativeDecoder(path.toString(),0,180000),bytes,new WavExporter.Options(offset,2033,true),p->{});
                byte[] wav=bytes.toByteArray(); ByteBuffer b=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
                if(wav.length!=44+(2033L*44100/1000)*4 || b.getInt(40)!=wav.length-44) throw new AssertionError("WAV size");
                int audible=0; for(int i=44;i<wav.length;i+=2) if(b.getShort(i)!=0) audible++;
                if(audible<100) throw new AssertionError("Silent WAV");
                if(b.getShort(wav.length-2)!=0 || b.getShort(wav.length-4)!=0) throw new AssertionError("Missing fade");
                if(Engine.tell(playing)!=before) throw new AssertionError("Export moved playback decoder");
                System.out.println("PASS export "+path.getFileName()+" offset="+offset+" bytes="+wav.length+" audible samples="+audible);
            }
        } finally { Engine.close(playing); }
    }
    public static void main(String[] args) throws Exception {
        Path spc=Files.createTempFile("export-check-",".spc");
        try {
            boolean found=false;
            try(ZipInputStream zip=new ZipInputStream(new FileInputStream(args[0]))) {
                ZipEntry entry; while((entry=zip.getNextEntry())!=null) if(entry.getName().toLowerCase().endsWith(".spc")) { Files.copy(zip,spc,StandardCopyOption.REPLACE_EXISTING); found=true; break; }
            }
            if(!found) throw new AssertionError("No SPC sample"); check(spc);
            if(args.length>1) check(Paths.get(args[1]));
        } finally { Files.deleteIfExists(spc); }
    }
}
