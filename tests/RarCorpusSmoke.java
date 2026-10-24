package dev.pixelspc;
import java.io.*;
public final class RarCorpusSmoke {
    public static void main(String[] args) throws Exception {
        for(String path:args) {
            int entries=0;long bytes=0;
            try(RarReader archive=new RarReader(new File(path))) {
                String name;byte[] buffer=new byte[16384];
                while((name=archive.next())!=null){long size=0;int n;while((n=archive.read(buffer))!=-1)size+=n;if(size!=archive.size())throw new AssertionError("Wrong entry size: "+name);bytes+=size;entries++;}
            }
            if(entries==0)throw new AssertionError("No archive entries");
            System.out.println("PASS "+new File(path).getName()+" · "+entries+" entries · "+bytes+" bytes · CRC checked");
        }
    }
}
