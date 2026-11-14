package dev.pixelspc;
import java.nio.file.*;

/** Original looping PSG tone: real JNI muting, seek preservation and decoder isolation. */
public class NativeMixerSmoke {
    private static void put(byte[] b,int at,int value){for(int i=0;i<4;i++)b[at+i]=(byte)(value>>>(8*i));}
    private static boolean audible(long h,int buffers)throws Exception{short[] pcm=new short[4096];boolean sound=false;for(int i=0;i<buffers;i++){if(Engine.render(h,pcm))throw new AssertionError("Muted playback ended early");if(i==buffers-1)for(short s:pcm)sound|=s!=0;}return sound;}
    public static void main(String[] args)throws Exception{
        byte[] commands={0x50,(byte)0x80,0x50,0x10,0x50,(byte)0x90,0x61,0x44,(byte)0xac,0x66};
        byte[] file=new byte[64+commands.length];file[0]='V';file[1]='g';file[2]='m';file[3]=' ';put(file,4,file.length-4);put(file,8,0x150);put(file,12,3579545);put(file,0x18,44100);put(file,0x1c,64+6-0x1c);put(file,0x20,44100);System.arraycopy(commands,0,file,64,commands.length);
        Path path=Files.createTempFile("chipamp-mixer-",".vgm");Files.write(path,file);long h=0,next=0;
        try{
            h=Engine.open(path.toString());Engine.start(h,0,180000);Engine.setLoop(h,false,180000);
            String[] names=Engine.channels(h);if(names.length<1||names.length>32)throw new AssertionError("No voice names");for(String name:names)if(name==null||name.isEmpty())throw new AssertionError("Empty voice name");
            if(!audible(h,8))throw new AssertionError("Original tone silent");
            Engine.channelMask(h,~1);if(!audible(h,20))throw new AssertionError("Solo first voice lost the tone");
            Engine.channelMask(h,1);if(audible(h,20))throw new AssertionError("Individual mute leaked the tone");
            Engine.channelMask(h,-1);if(audible(h,200))throw new AssertionError("Mute all leaked audio");
            Engine.seek(h,1234);if(audible(h,20))throw new AssertionError("Seek lost mute state");
            Engine.channelMask(h,0);if(!audible(h,20))throw new AssertionError("Reset failed to restore audio");
            Engine.channelMask(h,-1);next=Engine.open(path.toString());Engine.start(next,0,180000);if(!audible(next,8))throw new AssertionError("Mute leaked into another decoder/export");
            System.out.println("PASS: named GME voices, individual mute/solo, all-mute >9 seconds without early end, muted seek, reset and independent decoder");
        }finally{if(h!=0)Engine.close(h);if(next!=0)Engine.close(next);Files.deleteIfExists(path);}
    }
}
