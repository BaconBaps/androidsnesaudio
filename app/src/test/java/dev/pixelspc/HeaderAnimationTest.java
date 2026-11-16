package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;
public class HeaderAnimationTest {
    @Test public void textStaysStillThenExitsWaitsAndReturns(){
        HeaderAnimation.Cycle c=new HeaderAnimation.Cycle(7);assertEquals(HeaderAnimation.TITLE,c.stage(0));assertEquals(HeaderAnimation.EXIT,c.stage(c.title));assertEquals(HeaderAnimation.WAIT,c.stage(c.title+c.scroll));assertEquals(HeaderAnimation.ENTER,c.stage(c.title+c.scroll+c.blank));assertEquals(HeaderAnimation.REST,c.stage(c.total-c.rest));assertTrue(c.title>c.total*.7);
    }
    @Test public void lettersEnterStrictlyOneAtATimeFromTheRight(){
        HeaderAnimation.Cycle c=new HeaderAnimation.Cycle(3);long start=c.title+c.scroll+c.blank;
        for(int i=0;i<9;i++){long mid=start+(long)i*c.letter+c.letter/2;for(int j=0;j<i;j++)assertEquals(1,c.letterProgress(j,mid),0);assertEquals(.5,c.letterProgress(i,mid),.01);for(int j=i+1;j<9;j++)assertEquals(0,c.letterProgress(j,mid),0);}
    }
    @Test public void glitchesAreRareBoundedAndDeterministic(){
        for(int seed=0;seed<100;seed++){HeaderAnimation.Cycle c=new HeaderAnimation.Cycle(seed),copy=new HeaderAnimation.Cycle(seed);assertEquals(c.title,copy.title);assertTrue(c.glitches.length>=1&&c.glitches.length<=2);assertTrue(c.title>=16000);for(int i=0;i<c.glitches.length;i++){assertTrue(c.glitches[i]>=3500);assertTrue(c.glitches[i]+c.durations[i]<c.title);assertEquals(i,c.glitch(c.glitches[i]));}assertEquals(1000,c.nextDelay(0));}
    }
    @Test public void cycleResetReturnsToCompleteTitle(){HeaderAnimation a=new HeaderAnimation(10);HeaderAnimation.Cycle old=a.cycle();assertEquals(0,a.elapsed(100));assertEquals(0,a.elapsed(100+old.total));assertNotSame(old,a.cycle());assertEquals(HeaderAnimation.TITLE,a.cycle().stage(0));}
}
