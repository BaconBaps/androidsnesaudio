package dev.pixelspc;

import java.io.IOException;

/** Optional native renderer. Only accessed while its EGL context is current. */
final class ProjectMEngine {
    static { System.loadLibrary("projectM-4"); System.loadLibrary("projectm_jni"); }
    static native long create(int width,int height) throws IOException;
    static native void load(long handle,String preset,String[] texturePaths,boolean smooth) throws IOException;
    static native void render(long handle,short[] pcm,int samples) throws IOException;
    static native void destroy(long handle);
}
