package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Generated original NES fixtures: metadata and playlist mapping through the real JNI decoder. */
public class NesMetadataSmoke {
    private static void put(ByteArrayOutputStream out,int n){for(int i=0;i<4;i++)out.write(n>>>(i*8));}
    private static void chunk(ByteArrayOutputStream out,String tag,byte[] data)throws Exception{put(out,data.length);out.write(tag.getBytes(StandardCharsets.US_ASCII));out.write(data);}
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args)throws Exception{
        Path nsfe=Files.createTempFile("chipamp-nsfe-",".nsfe"),nsf=Files.createTempFile("chipamp-nsf-",".nsf");
        long h=0;
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("NSFE".getBytes(StandardCharsets.US_ASCII));
            chunk(out,"INFO",new byte[]{0,(byte)0x80,0,(byte)0x80,1,(byte)0x80,0,0,3,0});
            chunk(out,"DATA",new byte[]{0x60,0x60});
            chunk(out,"auth","NES Fixture\0ChipAmp9x\0Original fixture\0Test\0".getBytes(StandardCharsets.UTF_8));
            chunk(out,"tlbl","Menu\0Overworld\0Boss\0".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream times=new ByteArrayOutputStream();put(times,1000);put(times,2000);put(times,3000);chunk(out,"time",times.toByteArray());
            chunk(out,"plst",new byte[]{2,0});chunk(out,"NEND",new byte[0]);Files.write(nsfe,out.toByteArray());
            h=Engine.open(nsfe.toString());check(Engine.count(h)==2,"NSFE playlist count");
            check(Engine.info(h,0)[0].equals("Boss")&&Engine.info(h,1)[0].equals("Menu"),"NSFE labels in playlist order");
            check(Engine.info(h,0)[1].equals("NES Fixture")&&Engine.info(h,0)[2].equals("ChipAmp9x"),"NSFE game/artist");
            check(Engine.length(h,0)==3000&&Engine.length(h,1)==1000,"NSFE timing in playlist order");Engine.close(h);h=0;
            byte[] plain=new byte[130];System.arraycopy(new byte[]{'N','E','S','M',0x1a,1,2,1,0,(byte)0x80,0,(byte)0x80,1,(byte)0x80},0,plain,0,14);System.arraycopy("NES Fixture".getBytes(StandardCharsets.US_ASCII),0,plain,14,11);plain[128]=0x60;plain[129]=0x60;Files.write(nsf,plain);
            h=Engine.open(nsf.toString());check(Engine.count(h)==2,"NSF count");check(Engine.info(h,0)[0].isEmpty(),"Standard NSF has no per-track title");check(Engine.info(h,0)[1].equals("NES Fixture"),"NSF game title");
            System.out.println("PASS: NSFE track labels, auth, timings, playlist order; standard NSF metadata limits");
        }finally{if(h!=0)Engine.close(h);Files.deleteIfExists(nsfe);Files.deleteIfExists(nsf);}
    }
}
