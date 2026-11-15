package dev.pixelspc;

import java.io.*;
import java.util.Locale;

/** Import status sink. Byte updates are throttled; stages and filenames are immediate. */
public interface ImportProgress {
    void update(String message);
    ImportProgress NONE=message->{};
    static String inArchive(String name,String message){
        int split=message.indexOf(" · ");if(split<0)return message+" · "+name;
        String stage=message.substring(0,split),detail=message.substring(split+3);
        return stage+" · "+name+(detail.equals(name)?"":" › "+detail);
    }
    static OutputStream reporting(OutputStream out,ImportProgress progress,String label){
        if(progress==NONE)return out;
        return new FilterOutputStream(out){
            long bytes,last;
            private void report(int n){bytes+=n;long now=System.nanoTime();if(last==0||now-last>=200_000_000L){last=now;progress.update(label+String.format(Locale.ROOT," · %.1f MiB",bytes/(1024.0*1024)));}}
            @Override public void write(int value)throws IOException{out.write(value);report(1);}
            @Override public void write(byte[] data,int at,int n)throws IOException{out.write(data,at,n);report(n);}
        };
    }
}
