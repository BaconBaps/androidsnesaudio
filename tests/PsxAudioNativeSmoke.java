package dev.pixelspc;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
public class PsxAudioNativeSmoke {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Path dir=Paths.get(args[0]);int checked=0;
        for(String line:Files.readAllLines(dir.resolve("manifest.tsv"))){
            String[] item=line.split("\t");String name=item[0];int index=Integer.parseInt(item[1]),length=Integer.parseInt(item[2]),count=Integer.parseInt(item[3]);
            byte[] reference=Files.readAllBytes(dir.resolve(name+"."+index+".pcm"));ByteBuffer expected=ByteBuffer.wrap(reference).order(ByteOrder.LITTLE_ENDIAN);
            long h=Engine.open(dir.resolve(name).toString());
            try{
                check(Engine.count(h)==count,"Stream count "+name);check(Engine.length(h,index)==length,"Duration "+name);check(Engine.info(h,index)[3].equals("Sony PlayStation"),"Platform");check(Engine.channels(h).length==0,"Stream should not expose instrument mixer");Engine.start(h,index,length);Engine.setLoop(h,false,length);
                short[] pcm=new short[4096];int n=0;boolean ended=false;
                while(!ended){ended=Engine.render(h,pcm);for(short sample:pcm){if(expected.hasRemaining()){short value=expected.getShort();check(sample==value,name+" sample "+n+": "+sample+" != "+value);n++;}}check(n<=reference.length/2,"Overrun");}
                check(!expected.hasRemaining(),"Truncated output "+name);
                for(int ms:new int[]{Math.max(0,length-100),57,0,Math.min(501,length/2)}){
                    Engine.seek(h,ms);Engine.render(h,pcm);int offset=(int)((long)ms*44100/1000)*4;
                    for(int i=0;i<Math.min(pcm.length,(reference.length-offset)/2);i++)check(pcm[i]==ByteBuffer.wrap(reference,offset+i*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort(),"Seek reference "+name+" @"+ms+" sample "+i);
                }
                int before=Engine.tell(h);ByteArrayOutputStream wav=new ByteArrayOutputStream();WavExporter.write(WavExporter.nativeDecoder(dir.resolve(name).toString(),index,length),wav,new WavExporter.Options(57,1000,false),p->{});check(wav.size()==44+44100*4,"WAV size");check(Engine.tell(h)==before,"Export disturbed playback");
                Engine.setLoop(h,true,length);Engine.seek(h,length-1);for(int i=0;i<6;i++)check(!Engine.render(h,pcm),"Loop ended "+name);
                Engine.seek(h,length);Engine.render(h,pcm);int loopOffset=name.endsWith(".vag")?7168*4:0;
                for(int i=0;i<256;i++)check(pcm[i]==ByteBuffer.wrap(reference,loopOffset+i*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort(),"Wrong loop start "+name);
                checked++;System.out.println("PASS "+name+" #"+index+" · sample-exact independent PCM, seek, duration, loop, isolated WAV export");
            }finally{Engine.close(h);}
        }
        for(String name:new String[]{"bad-vag-predictor.vag","stereo.vag","bad-mode.xa","bad-subheader.xa","bad-coding.xa","bad-xa-predictor.xa","truncated.xa","truncated.vag","not-cdxa.xa"}){
            try{long h=Engine.open(dir.resolve(name).toString());Engine.close(h);throw new AssertionError("Accepted "+name);}catch(IOException expected){System.out.println("PASS rejected "+name);}
        }
        Path archive=Files.createTempFile("psx-import-",".zip"),destination=Files.createTempDirectory("psx-extract-");
        try{
            try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))){for(String name:new String[]{"interleaved.xa","tone.vag"}){zip.putNextEntry(new java.util.zip.ZipEntry("PlayStation/"+name));Files.copy(dir.resolve(name),zip);zip.closeEntry();}}
            java.util.List<ArchiveImporter.Entry> entries=ArchiveImporter.extract(archive.toFile(),"PSX.zip",destination.toFile());check(entries.size()==2,"ZIP formats not imported");int streams=0;for(ArchiveImporter.Entry entry:entries){long h=Engine.open(entry.file.getPath());try{streams+=Engine.count(h);}finally{Engine.close(h);}}check(streams==4,"ZIP did not preserve XA subsongs");
        }finally{ArchiveImporter.removeTree(destination.toFile());Files.deleteIfExists(archive);}
        System.out.println("PASS "+checked+" PSX audio reference streams");
    }
}
