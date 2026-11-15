package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;

public class HeaderAnimationTest {
    @Test public void cycleStagesAndSmoothTransitionsHaveExactBoundaries(){
        HeaderAnimation.Cycle c=new HeaderAnimation.Cycle(7);
        assertEquals(HeaderAnimation.TITLE,c.stage(0));assertEquals(HeaderAnimation.BREAK,c.stage(c.title));
        assertEquals(HeaderAnimation.WAVE,c.stage(c.title+c.scatter));assertEquals(HeaderAnimation.REFORM,c.stage(c.title+c.scatter+c.wave));
        assertEquals(HeaderAnimation.REST,c.stage(c.total-c.rest));assertEquals(0,HeaderAnimation.ease(-1),0);assertEquals(1,HeaderAnimation.ease(2),0);assertEquals(.5,HeaderAnimation.ease(.5f),.001);
    }
    @Test public void choicesAreStableWithinFramesAndChangeAtCycleBoundaries(){
        HeaderAnimation a=new HeaderAnimation(10),b=new HeaderAnimation(10);assertEquals(a.cycle().frequency,b.cycle().frequency,0);assertEquals(a.cycle().speed,b.cycle().speed,0);
        HeaderAnimation.Cycle original=a.cycle();assertEquals(0,a.elapsed(100));assertEquals(original.total-1,a.elapsed(100+original.total-1));assertSame(original,a.cycle());
        assertEquals(0,a.elapsed(100+original.total));assertNotSame(original,a.cycle());assertTrue(original.speed!=a.cycle().speed||original.frequency!=a.cycle().frequency);
        float y=a.cycle().waveY(25,3);assertEquals(y,a.cycle().waveY(25,3),0);
    }
    @Test public void randomTimingsAndGlitchesStayWithinBoundedReadableRanges(){
        for(int seed=0;seed<100;seed++){
            HeaderAnimation.Cycle c=new HeaderAnimation.Cycle(seed);assertTrue(c.total>=7000&&c.total<=12000);assertTrue(c.glitches.length>=3&&c.glitches.length<=5);
            for(int i=0;i<c.glitches.length;i++){assertTrue(c.glitches[i]>0);assertTrue(c.glitches[i]+c.durations[i]<c.title);assertEquals(i,c.glitch(c.glitches[i]));if(i>0)assertTrue(c.glitches[i]>c.glitches[i-1]+c.durations[i-1]);}
        }
    }
    @Test public void waveformRandomizesBothSpeedAndFrequencyWhileStayingOnScreen(){
        float minSpeed=10,maxSpeed=0,minFrequency=1,maxFrequency=0;boolean forward=false,backward=false;
        for(int seed=0;seed<60;seed++){
            HeaderAnimation.Cycle c=new HeaderAnimation.Cycle(seed);minSpeed=Math.min(minSpeed,c.speed);maxSpeed=Math.max(maxSpeed,c.speed);minFrequency=Math.min(minFrequency,c.frequency);maxFrequency=Math.max(maxFrequency,c.frequency);forward|=c.direction==1;backward|=c.direction==-1;
            for(int x=0;x<94;x++)for(int t=0;t<12;t++){float y=c.waveY(x,t);assertTrue(Float.isFinite(y));assertTrue(y>=0&&y<HeaderAnimation.ROWS);}
        }
        assertTrue(maxSpeed-minSpeed>1);assertTrue(maxFrequency-minFrequency>.08f);assertTrue(forward&&backward);
    }
}
