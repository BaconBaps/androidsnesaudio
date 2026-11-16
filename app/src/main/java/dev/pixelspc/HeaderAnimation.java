package dev.pixelspc;
import java.util.Random;

/** Mostly static hi-fi title: rare glitches, a downward exit, then individual letter returns. */
final class HeaderAnimation {
    static final int TITLE=0,EXIT=1,WAIT=2,ENTER=3,REST=4,COLS=94,ROWS=18,LETTERS=9;
    static final class Cycle {
        final int title,scroll,blank,letter,enter,rest,total,shift,band,bandSize;
        final int[] glitches,durations;
        Cycle(long seed){
            Random r=new Random(seed);title=16000+r.nextInt(8001);scroll=900+r.nextInt(301);blank=250;letter=320+r.nextInt(101);enter=letter*LETTERS;rest=1000;total=title+scroll+blank+enter+rest;
            shift=(r.nextBoolean()?1:-1)*(2+r.nextInt(3));band=r.nextInt(10);bandSize=1+r.nextInt(2);
            int n=1+r.nextInt(2);glitches=new int[n];durations=new int[n];for(int i=0;i<n;i++){glitches[i]=3500+i*(title-6000)/n+r.nextInt(1200);durations[i]=80+r.nextInt(61);}
        }
        int stage(long t){if(t<title)return TITLE;if(t<title+scroll)return EXIT;if(t<title+scroll+blank)return WAIT;if(t<title+scroll+blank+enter)return ENTER;return REST;}
        int glitch(long t){for(int i=0;i<glitches.length;i++)if(t>=glitches[i]&&t<glitches[i]+durations[i])return i;return -1;}
        float letterProgress(int index,long t){return ease((t-title-scroll-blank-index*letter)/(float)letter);}
        int nextDelay(long t){if(stage(t)!=TITLE)return 50;if(glitch(t)>=0)return 50;long next=title;for(int g:glitches)if(g>t){next=g;break;}return (int)Math.max(50,Math.min(1000,next-t));}
    }
    private final long seed;private long started=-1,index;private Cycle cycle;
    HeaderAnimation(long seed){this.seed=seed;cycle=new Cycle(seed);}
    Cycle cycle(){return cycle;}
    long elapsed(long now){if(started<0)started=now;long t=Math.max(0,now-started);if(t>=cycle.total){cycle=new Cycle(seed+(++index)*0x9e3779b97f4a7c15L);started=now;return 0;}return t;}
    static float ease(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
}
