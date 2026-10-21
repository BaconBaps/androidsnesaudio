package dev.pixelspc;
import java.io.IOException;
public final class Engine {
    static { System.loadLibrary("gme"); System.loadLibrary("player"); }
    public static native long open(String path) throws IOException;
    public static native void close(long handle);
    public static native int count(long handle);
    public static native String[] info(long handle, int index) throws IOException;
    public static native int length(long handle, int index);
    public static native void start(long handle,int index,int length) throws IOException;
    public static native void setLoop(long handle,boolean enabled,int length);
    public static native boolean render(long handle,short[] pcm) throws IOException;
    public static native void seek(long handle,int ms) throws IOException;
    public static native int tell(long handle);
}
