package dev.pixelspc;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
/** Real user sets remain outside the repository; exercise audio, seeks, loops and export input. */
public class ArcadeVgmSmoke {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static long energy(long h,int ms)throws Exception{short[] pcm=new short[2048];long sum=0;for(int n=0;n<(ms*441L/10+1023)/1024;n++){Engine.render(h,pcm);for(short v:pcm)sum+=Math.abs((int)v);}return sum;}
    public static void main(String[] args)throws Exception{
        for(String file:args){long h=Engine.open(file);try{
            check(Engine.count(h)==1,"VGM count");check(Engine.channels(h).length==0,"Do not expose a false GME channel mixer");String[] tags=Engine.info(h,0);check(tags.length==5&&!tags[0].isEmpty(),"GD3 title");int length=Engine.length(h,0);Engine.start(h,0,length);Engine.setLoop(h,true,length);
            long sound=energy(h,3000);check(sound>100000,"Silent audio: "+file);int before=Engine.tell(h);check(before>=3000,"Progress");
            for(int ms=before+250;ms<=10000;ms+=250)Engine.seek(h,ms);Engine.setLoop(h,true,length);check(energy(h,500)>10000,"Silent after forward seek");
            Engine.seek(h,0);for(int ms=250;ms<=2000;ms+=250)Engine.seek(h,ms);check(energy(h,500)>10000,"Silent after backward seek");
            try(InputStream input=new GZIPInputStream(Files.newInputStream(Paths.get(file)))){byte[] header=new byte[64];int at=0,n;while(at<header.length&&(n=input.read(header,at,header.length-at))>0)at+=n;long ticks=0;for(int b=0;b<4;b++)ticks|=(header[0x18+b]&255L)<<(8*b);int boundary=(int)(ticks*1000/44100);
                for(int ms=Engine.tell(h)+250;ms<boundary-200;ms+=250)Engine.seek(h,ms);check(energy(h,1000)>10000,"Silent across native loop boundary");check(Engine.tell(h)>boundary,"Loop did not advance");}
            Engine.setLoop(h,false,Engine.tell(h));check(energy(h,500)>10000,"Silent after loop toggle");boolean ended=false;short[] tail=new short[4096];for(int block=0;block<180;block++)ended|=Engine.render(h,tail);check(ended,"Loop off should fade and end");
            Path raw=Files.createTempFile("chipamp-arcade-", ".vgm");try{try(InputStream gz=new GZIPInputStream(Files.newInputStream(Paths.get(file)))){Files.copy(gz,raw,StandardCopyOption.REPLACE_EXISTING);}long plain=Engine.open(raw.toString());try{Engine.start(h,0,length);Engine.start(plain,0,length);Engine.setLoop(h,true,length);Engine.setLoop(plain,true,length);short[] a=new short[4096],b=new short[4096];for(int block=0;block<8;block++){Engine.render(h,a);Engine.render(plain,b);check(Arrays.equals(a,b),"Raw VGM / VGZ PCM mismatch");}}finally{Engine.close(plain);}}finally{Files.deleteIfExists(raw);}
            System.out.println("PASS "+tags[0]+" | "+tags[3]+" | "+length+"ms | energy="+sound);
        }finally{Engine.close(h);}}
    }
}
