package dev.pixelspc;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Original generated headers, with no game samples. */
public class VgmValidationSmoke {
 static void put(byte[] b,int at,int n){for(int i=0;i<4;i++)b[at+i]=(byte)(n>>>(8*i));}
 static byte[] file(byte[] commands)throws Exception{
  byte[] tag="曲 🎵\0\0Game\0\0Arcade\0\0Artist\0\0Date\0Dumper\0Notes\0".getBytes(StandardCharsets.UTF_16LE);
  int gd3=256+commands.length;byte[] b=new byte[gd3+12+tag.length];b[0]='V';b[1]='g';b[2]='m';b[3]=' ';put(b,4,b.length-4);put(b,8,0x170);put(b,0x14,gd3-0x14);put(b,0x18,44100);put(b,0x30,3579545);put(b,0x34,256-0x34);
  System.arraycopy(commands,0,b,256,commands.length);b[gd3]='G';b[gd3+1]='d';b[gd3+2]='3';b[gd3+3]=' ';put(b,gd3+4,0x100);put(b,gd3+8,tag.length);System.arraycopy(tag,0,b,gd3+12,tag.length);return b;
 }
 static void reject(Path path,byte[] bytes,String message)throws Exception{Files.write(path,bytes);try{long h=Engine.open(path.toString());Engine.close(h);throw new AssertionError("Accepted "+message);}catch(IOException expected){if(!expected.getMessage().toLowerCase().contains(message.toLowerCase()))throw expected;}}
 public static void main(String[] args)throws Exception{
  Path p=Files.createTempFile("chipamp-vgm-validation-",".vgm");try{
   reject(p,file(new byte[]{0,0x66}),"Unsupported VGM command");
   reject(p,file(new byte[]{0x67,0x66,(byte)0x80,0x7f,(byte)0xff,(byte)0xff,0x7f,0x66}),"Truncated VGM sample");
   byte[] badLoop=file(new byte[]{0x54,0x20,(byte)0xc7,0x61,0x44,(byte)0xac,0x66});put(badLoop,0x1c,257-0x1c);reject(p,badLoop,"Invalid VGM loop offset");
   byte[] opl=file(new byte[]{0x66});put(opl,0x60,33868800);reject(p,opl,"OPL4");
   Files.write(p,file(new byte[]{0x54,0x20,(byte)0xc7,0x61,0x44,(byte)0xac,0x66}));long h=Engine.open(p.toString());try{String[] info=Engine.info(h,0);if(!info[0].equals("曲 🎵")||!info[1].equals("Game")||!info[2].equals("Artist"))throw new AssertionError("Unicode GD3");}finally{Engine.close(h);}
   System.out.println("PASS: unsupported commands, truncated sample blocks, unsupported OPL4, Unicode GD3 and recovery");
  }finally{Files.deleteIfExists(p);}
 }
}
