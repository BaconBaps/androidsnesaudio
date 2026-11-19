package dev.pixelspc;

/** Bounded stereo PCM handoff. The audio worker never waits on OpenGL. */
final class VisualizerPcm {
    private final short[] ring=new short[16384];
    private int start,size;
    private boolean enabled;
    synchronized void enabled(boolean value){if(enabled!=value){enabled=value;start=size=0;}}
    synchronized void clear(){start=size=0;}
    synchronized void offer(short[] pcm,int offset,int count){
        if(!enabled)return;
        count-=count%2;
        if(count>ring.length){offset+=count-ring.length;count=ring.length;}
        int drop=Math.max(0,size+count-ring.length);start=(start+drop)%ring.length;size-=drop;
        int end=(start+size)%ring.length,first=Math.min(count,ring.length-end);
        System.arraycopy(pcm,offset,ring,end,first);System.arraycopy(pcm,offset+first,ring,0,count-first);size+=count;
    }
    synchronized int drain(short[] out){
        int n=Math.min(size,out.length);n-=n%2;int first=Math.min(n,ring.length-start);
        System.arraycopy(ring,start,out,0,first);System.arraycopy(ring,0,out,first,n-first);
        start=(start+n)%ring.length;size-=n;return n;
    }
}
