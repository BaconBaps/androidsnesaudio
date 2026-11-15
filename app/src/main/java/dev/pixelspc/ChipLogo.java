package dev.pixelspc;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Programmatic pinball display: retro sans title, restrained glitches and a randomized wave. */
final class ChipLogo extends View {
    private static int[] titleX,titleY;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    private final RectF viewport=new RectF();
    private final HeaderAnimation animation=new HeaderAnimation(new java.util.Random().nextLong());
    private boolean frozen;
    ChipLogo(Context c){this(c,ThemeStore.selected(c));}
    ChipLogo(Context c,PlayerTheme theme){super(c);color=theme.headerInk();loadTitle(c);setContentDescription(c.getString(R.string.app_name));setTooltipText("Open settings");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    private static synchronized void loadTitle(Context c){
        if(titleX!=null)return;
        Bitmap mask=Bitmap.createBitmap(HeaderAnimation.COLS,HeaderAnimation.ROWS,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(mask);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(UiFonts.body(c));p.setTextSize(17);p.setColor(Color.WHITE);
        float width=p.measureText("ChipAmp9x");if(width>88)p.setTextSize(p.getTextSize()*88/width);
        Rect ink=new Rect();p.getTextBounds("ChipAmp9x",0,9,ink);if(ink.height()>16){p.setTextSize(p.getTextSize()*16/ink.height());p.getTextBounds("ChipAmp9x",0,9,ink);}
        canvas.drawText("ChipAmp9x",(HeaderAnimation.COLS-p.measureText("ChipAmp9x"))/2,(HeaderAnimation.ROWS-ink.height())/2f-ink.top,p);
        int[] xs=new int[HeaderAnimation.COLS*HeaderAnimation.ROWS],ys=new int[xs.length];int count=0;
        for(int y=0;y<HeaderAnimation.ROWS;y++)for(int x=0;x<HeaderAnimation.COLS;x++)if(Color.alpha(mask.getPixel(x,y))>95){xs[count]=x;ys[count++]=y;}
        titleX=java.util.Arrays.copyOf(xs,count);titleY=java.util.Arrays.copyOf(ys,count);mask.recycle();
    }
    void setFrozen(boolean value){frozen=value;invalidate();}
    void drawInto(Canvas c,RectF box){renderFrame(c,box,0,false);}
    private void dot(Canvas c,float x,float y,float step,float ox,float oy,int alpha){
        if(x<0||x>=HeaderAnimation.COLS||y<0||y>=HeaderAnimation.ROWS)return;
        float px=ox+Math.round(x)*step,py=oy+Math.round(y)*step;
        paint.setColor(color);paint.setAlpha(alpha/8);c.drawCircle(px,py,step*.72f,paint);paint.setAlpha(alpha);c.drawCircle(px,py,step*.34f,paint);
    }
    void renderFrame(Canvas c,RectF box,long elapsed,boolean moving){
        if(box.width()<=0||box.height()<=0)return;
        float step=Math.min(box.width()/(HeaderAnimation.COLS+2),box.height()/(HeaderAnimation.ROWS+2)),ox=box.centerX()-(HeaderAnimation.COLS-1)*step/2,oy=box.centerY()-(HeaderAnimation.ROWS-1)*step/2;
        HeaderAnimation.Cycle cycle=animation.cycle();int stage=moving?cycle.stage(elapsed):HeaderAnimation.TITLE,glitch=moving?cycle.glitch(elapsed):-1;
        c.save();c.clipRect(box);
        if(stage==HeaderAnimation.WAVE){for(int x=0;x<HeaderAnimation.COLS;x++)dot(c,x,cycle.waveY(x,elapsed/1000f),step,ox,oy,255);}
        else for(int i=0;i<titleX.length;i++){
            float x=titleX[i],y=titleY[i];
            if(stage==HeaderAnimation.TITLE&&glitch>=0){
                int band=3+(cycle.band+glitch*3)%10;
                if(y>=band&&y<band+cycle.bandSize)x+=glitch%2==0?cycle.shift:-cycle.shift;
                if(glitch==cycle.glitches.length-1&&((titleX[i]+glitch)%13==0))continue;
            }else if(stage==HeaderAnimation.BREAK||stage==HeaderAnimation.REFORM){
                boolean reform=stage==HeaderAnimation.REFORM;
                float q=HeaderAnimation.ease(reform?(elapsed-cycle.title-cycle.scatter-cycle.wave)/(float)cycle.reform:(elapsed-cycle.title)/(float)cycle.scatter);
                int order=cycle.reverse?titleX.length-1-i:i;float wx=order%HeaderAnimation.COLS;
                float seconds=reform?(cycle.title+cycle.scatter+cycle.wave)/1000f:elapsed/1000f;
                float wy=cycle.waveY(wx,seconds),drift=(float)Math.sin(i*2.7+cycle.drift)*(float)Math.sin(q*Math.PI)*3;
                float blend=reform?1-q:q;x=x*(1-blend)+wx*blend+drift;y=y*(1-blend)+wy*blend;
            }
            dot(c,x,y,step,ox,oy,255);
        }
        c.restore();
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)invalidate();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);viewport.set(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getHeight()-getPaddingBottom());
        boolean moving=!frozen&&getWindowVisibility()==VISIBLE&&hasWindowFocus()&&isShown()&&ValueAnimator.areAnimatorsEnabled();long elapsed=moving?animation.elapsed(SystemClock.uptimeMillis()):0;
        renderFrame(c,viewport,elapsed,moving);if(moving)postInvalidateDelayed(50);
    }
}
