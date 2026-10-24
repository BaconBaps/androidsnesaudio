package dev.pixelspc;
import java.io.*;
import java.nio.file.*;
/** Optional external-bank compatibility check; banks are never bundled. */
public final class SoundFontBankSmoke {
    private static void variable(DataOutputStream out,int n)throws IOException {int buffer=n&127;while((n>>=7)>0)buffer=(buffer<<8)|((n&127)|128);while(true){out.writeByte(buffer);if((buffer&128)==0)break;buffer>>=8;}}
    public static void main(String[] banks)throws Exception {
        ByteArrayOutputStream data=new ByteArrayOutputStream();DataOutputStream events=new DataOutputStream(data);
        for(int program=0;program<128;program++){
            variable(events,0);events.write(new byte[]{(byte)0xc0,(byte)program});variable(events,0);events.write(new byte[]{(byte)0x90,60,100});
            variable(events,240);events.write(new byte[]{(byte)0x80,60,0});
        }
        variable(events,0);events.write(new byte[]{(byte)0xff,0x2f,0});
        Path midi=Files.createTempFile("soundfont-programs-",".mid");
        try {
            try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(midi))) {out.writeBytes("MThd");out.writeInt(6);out.writeShort(0);out.writeShort(1);out.writeShort(480);out.writeBytes("MTrk");out.writeInt(data.size());out.write(data.toByteArray());}
            for(String bank:banks) {
                Engine.validateSoundFont(bank);long h=Engine.open(midi.toString());
                try {
                    Engine.soundFont(h,bank);Engine.start(h,0,Engine.length(h,0));short[] pcm=new short[4096];long energy=0;boolean ended=false;
                    for(int i=0;i<800;i++){ended=Engine.render(h,pcm);for(short v:pcm)energy+=Math.abs((int)v);}
                    if(energy<100||!ended)throw new AssertionError("Bank silent or did not end: "+bank);
                    System.out.println("PASS "+new File(bank).getName()+" · validates, 128 program selections, audible PCM and end");
                } finally {Engine.close(h);}
            }
        } finally {Files.deleteIfExists(midi);}
    }
}
