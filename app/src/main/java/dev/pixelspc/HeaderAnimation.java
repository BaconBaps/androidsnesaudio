package dev.pixelspc;
import java.util.Random;

/** Seeded, bounded pinball choreography. Random choices happen once per cycle, never per frame. */
final class HeaderAnimation {
    static final int TITLE=0,BREAK=1,WAVE=2,REFORM=3,REST=4,COLS=94,ROWS=18;
    static final class Cycle {
        final int title,scatter,wave,reform,rest,total,direction,shift,band,bandSize;
        final int[] glitches,durations;
        final float frequency,frequency2,speed,amplitude,phase,drift;
        final boolean reverse;
        Cycle(long seed){
            Random r=new Random(seed);title=2600+r.nextInt(701);scatter=950+r.nextInt(501);wave=1900+r.nextInt(1401);reform=950+r.nextInt(501);rest=650+r.nextInt(451);total=title+scatter+wave+reform+rest;
            direction=r.nextBoolean()?1:-1;shift=(r.nextBoolean()?1:-1)*(2+r.nextInt(4));band=r.nextInt(10);bandSize=1+r.nextInt(3);reverse=r.nextBoolean();
            frequency=.11f+r.nextFloat()*.17f;frequency2=.25f+r.nextFloat()*.20f;speed=1.8f+r.nextFloat()*3.6f;amplitude=2.6f+r.nextFloat()*1.8f;phase=r.nextFloat()*6.283185f;drift=r.nextFloat()*6.283185f;
            int n=3+r.nextInt(3);glitches=new int[n];durations=new int[n];
            for(int i=0;i<n;i++){glitches[i]=450+i*(title-850)/n+r.nextInt(100);durations[i]=90+r.nextInt(91);}
        }
        int stage(long elapsed){if(elapsed<title)return TITLE;if(elapsed<title+scatter)return BREAK;if(elapsed<title+scatter+wave)return WAVE;if(elapsed<title+scatter+wave+reform)return REFORM;return REST;}
        int glitch(long elapsed){for(int i=0;i<glitches.length;i++)if(elapsed>=glitches[i]&&elapsed<glitches[i]+durations[i])return i;return -1;}
        float waveY(float x,float seconds){return 8.5f+amplitude*(float)Math.sin(x*frequency+seconds*speed*direction+phase)+1.1f*(float)Math.sin(x*frequency2-seconds*speed*.7f*direction+drift);}
    }
    private final long seed;private long started=-1,index;private Cycle cycle;
    HeaderAnimation(long seed){this.seed=seed;cycle=new Cycle(seed);}
    Cycle cycle(){return cycle;}
    long elapsed(long now){if(started<0)started=now;long elapsed=Math.max(0,now-started);if(elapsed>=cycle.total){index++;cycle=new Cycle(seed+index*0x9e3779b97f4a7c15L);started=now;elapsed=0;}return elapsed;}
    static float ease(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
}
