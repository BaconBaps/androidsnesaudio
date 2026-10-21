package dev.pixelspc;

import java.io.*;

/** Reads explicit xid6 loop-length tags; a total duration alone is not a loop tag. */
public final class SpcTiming {
    public static int loopLength(File file) {
        try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
            if(in.length()<66056) return 0;
            in.seek(66048); if(in.readInt()!=0x78696436) return 0; // "xid6"
            long size=uint32(in); if(size>1024*1024 || size>in.length()-66056) return 0;
            long end=66056+size;
            while(in.getFilePointer()+4<=end) {
                int id=in.readUnsignedByte(),type=in.readUnsignedByte();
                int value=in.readUnsignedByte() | in.readUnsignedByte()<<8;
                int length=type==0?0:value; long data=in.getFilePointer();
                if(data+length>end) return 0;
                if(id==0x31 && type==4 && length==4) return (int)(uint32(in)/64); // 64,000 ticks/second
                in.seek(data+length);
                long unaligned=in.getFilePointer();
                while((in.getFilePointer()&3)!=0 && in.getFilePointer()<end) {
                    if(in.readUnsignedByte()!=0) { in.seek(unaligned); break; }
                }
            }
        } catch(IOException ignored) { /* Missing/legacy/truncated tags are unknown, not guessed. */ }
        return 0;
    }
    private static long uint32(RandomAccessFile in) throws IOException { return Integer.toUnsignedLong(Integer.reverseBytes(in.readInt())); }
}
