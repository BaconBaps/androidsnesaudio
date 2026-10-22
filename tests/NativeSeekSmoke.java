package dev.pixelspc;

import java.nio.file.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;

/** Generated, original PSG tone in a Mega Drive VGM container, plus optional real VGZ. */
public class NativeSeekSmoke {
    private static void put(byte[] b,int offset,int value) {
        for(int i=0;i<4;i++) b[offset+i]=(byte)(value >>> (i*8));
    }
    private static void check(Path file,boolean loop) throws Exception {
        long h=Engine.open(file.toString());
        try {
            int length=Math.max(120000,Engine.length(h,0));
            Engine.start(h,0,length); Engine.setLoop(h,loop,length);
            short[] pcm=new short[4096]; Engine.render(h,pcm);
            PlaybackSeek seek=new PlaybackSeek();
            PlaybackSeek.Decoder decoder=new PlaybackSeek.Decoder() {
                public int tell() { return Engine.tell(h); }
                public void seek(int ms) throws IOException { Engine.seek(h,ms); }
                public void restoreMode() { Engine.setLoop(h,loop,length); }
            };
            for(int target:new int[]{60000,15000,0,30000}) {
                seek.begin(target); int steps=0;
                while(seek.active()) { seek.step(decoder); if(++steps>1000) throw new AssertionError("Seek never finished"); }
                if(Math.abs(Engine.tell(h)-target)>2) throw new AssertionError("Wrong seek position");
                boolean ended=Engine.render(h,pcm); boolean audible=false;
                for(short sample:pcm) audible|=sample!=0;
                if(ended || !audible) throw new AssertionError("Playback did not resume after VGZ seek to "+target);
            }
            // An interrupted seek must permit a new track/decoder to render normally.
            seek.begin(180000); seek.step(decoder); seek.cancel();
            long next=Engine.open(file.toString());
            try { Engine.start(next,0,length); Engine.render(next,pcm); if(Engine.tell(next)<=0) throw new AssertionError("New track stalled"); }
            finally { Engine.close(next); }
            System.out.println("PASS: "+file.getFileName()+" loop="+loop+" forward/backward bounded seeks, audible resume and new decoder after cancellation");
        } finally { Engine.close(h); }
    }
    public static void main(String[] args) throws Exception {
        byte[] commands={0x50,(byte)0x80,0x50,0x10,0x50,(byte)0x90,0x61,0x44,(byte)0xac,0x66};
        byte[] vgm=new byte[64+commands.length];
        vgm[0]='V';vgm[1]='g';vgm[2]='m';vgm[3]=' ';
        put(vgm,4,vgm.length-4); put(vgm,8,0x150); put(vgm,12,3579545);
        put(vgm,0x18,44100); put(vgm,0x1c,64+6-0x1c); put(vgm,0x20,44100);
        put(vgm,0x2c,7670454); put(vgm,0x34,12);
        System.arraycopy(commands,0,vgm,64,commands.length);
        Path fixture=Files.createTempFile("pixelspc-seek-", ".vgz");
        try {
            try(OutputStream out=new GZIPOutputStream(Files.newOutputStream(fixture))) { out.write(vgm); }
            check(fixture,false); check(fixture,true);
            for(String path:args) { check(Paths.get(path),false); check(Paths.get(path),true); }
        } finally { Files.delete(fixture); }
    }
}
