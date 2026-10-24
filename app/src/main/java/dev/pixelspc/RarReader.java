package dev.pixelspc;

import java.io.*;

/** Sequential LGPL unarr reader. All entries are drained for solid RAR sets. */
final class RarReader extends InputStream {
    static { System.loadLibrary("gme"); System.loadLibrary("unarr"); System.loadLibrary("player"); }
    private long handle;
    RarReader(File file) throws IOException { handle=open(file.getAbsolutePath()); }
    String next() throws IOException { check(); byte[] name=nextBytes(handle); return name==null?null:new String(name,java.nio.charset.StandardCharsets.UTF_8); }
    long size() throws IOException { check(); return size(handle); }
    private void check() throws IOException { if(handle==0) throw new IOException("Archive is closed"); }
    @Override public int read(byte[] bytes,int offset,int length) throws IOException { check(); if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException(); return read(handle,bytes,offset,length); }
    @Override public int read() throws IOException { byte[] b=new byte[1]; return read(b,0,1)<0?-1:b[0]&255; }
    @Override public void close() { if(handle!=0) { close(handle); handle=0; } }
    private static native long open(String path) throws IOException;
    private static native byte[] nextBytes(long handle) throws IOException;
    private static native long size(long handle) throws IOException;
    private static native int read(long handle,byte[] bytes,int offset,int length) throws IOException;
    private static native void close(long handle);
}
