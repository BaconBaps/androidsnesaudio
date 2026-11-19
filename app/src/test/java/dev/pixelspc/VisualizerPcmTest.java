package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;
public class VisualizerPcmTest {
    @Test public void handoffDropsOldAudioWithoutReorderingStereo(){
        VisualizerPcm tap=new VisualizerPcm();short[] block=new short[4096],out=new short[16384];
        tap.offer(block,0,block.length);assertEquals(0,tap.drain(out));tap.enabled(true);
        for(int n=0;n<6;n++){for(int i=0;i<block.length;i++)block[i]=(short)(n*4096+i);tap.offer(block,0,block.length);}
        assertEquals(16384,tap.drain(out));for(int i=0;i<out.length;i++)assertEquals((short)(8192+i),out[i]);assertEquals(0,tap.drain(out));
        tap.offer(block,0,block.length);tap.enabled(false);tap.enabled(true);assertEquals(0,tap.drain(out));
    }
    @Test public void partialWritesAndSeekClearDoNotReplayStaleSamples(){
        VisualizerPcm tap=new VisualizerPcm();tap.enabled(true);short[] in={1,2,3,4,5,6},out=new short[4];tap.offer(in,2,4);assertEquals(4,tap.drain(out));assertArrayEquals(new short[]{3,4,5,6},out);tap.offer(in,0,6);tap.clear();assertEquals(0,tap.drain(out));
    }
}
