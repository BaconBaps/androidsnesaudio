package dev.pixelspc;
import java.nio.file.*;
import java.util.*;
/** Scan private supplied sets, keeping user music outside the repository. */
public class ArcadeVgmSetScan {
 public static void main(String[] args)throws Exception{
  for(String folder:args){int tracks=0,audible=0;List<Path> files=new ArrayList<>();try(java.util.stream.Stream<Path> paths=Files.list(Paths.get(folder))){paths.sorted().forEach(files::add);}
   for(Path path:files){if(!path.toString().endsWith(".vgz"))continue;long h=Engine.open(path.toString());try{
    if(Engine.info(h,0).length!=5||Engine.length(h,0)<=0)throw new AssertionError("Metadata: "+path);Engine.start(h,0,Engine.length(h,0));Engine.setLoop(h,false,Engine.length(h,0));long power=0;short[] pcm=new short[4096];
    for(int n=0;n<44;n++){boolean end=Engine.render(h,pcm);for(short value:pcm)power+=Math.abs((int)value);if(end)break;}
    if(power>1000)audible++;tracks++;
   }finally{Engine.close(h);}}
   if(tracks==0||audible==0)throw new AssertionError("Silent set: "+folder);System.out.println("PASS "+folder+": "+tracks+" tracks opened; "+audible+" produce audio within 2 seconds");
  }
 }
}
