package dev.pixelspc;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Programmatic pinball display: retro sans title, restrained glitches and a randomized wave. */
final class ChipLogo extends View {
    private static int[] titleX,titleY,titleLetter;private static final int[] letterLeft=new int[9];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    private final RectF viewport=new RectF();
    private final HeaderAnimation animation=new HeaderAnimation(new java.util.Random().nextLong());
    private boolean frozen;
    ChipLogo(Context c){this(c,ThemeStore.selected(c));}
    ChipLogo(Context c,PlayerTheme theme){super(c);color=theme.headerInk();loadTitle(c);setContentDescription(c.getString(R.string.app_name));setTooltipText("Open player menu");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    private static synchronized void loadTitle(Context c){
        if(titleX!=null)return;
        Bitmap mask=Bitmap.createBitmap(HeaderAnimation.COLS,HeaderAnimation.ROWS,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(mask);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(UiFonts.brand(c));p.setTextSize(17);p.setColor(Color.WHITE);
        float width=p.measureText("ChipAmp9x");if(width>88)p.setTextSize(p.getTextSize()*88/width);
        Rect ink=new Rect();p.getTextBounds("ChipAmp9x",0,9,ink);if(ink.height()>16){p.setTextSize(p.getTextSize()*16/ink.height());p.getTextBounds("ChipAmp9x",0,9,ink);}
        float baseline=(HeaderAnimation.ROWS-ink.height())/2f-ink.top;float start=(HeaderAnimation.COLS-p.measureText("ChipAmp9x"))/2;
        int[] xs=new int[HeaderAnimation.COLS*HeaderAnimation.ROWS*9],ys=new int[xs.length],letters=new int[xs.length];int count=0;
        String text="ChipAmp9x";
        for(int letter=0;letter<9;letter++){
            mask.eraseColor(Color.TRANSPARENT);canvas.drawText(text,letter,letter+1,start+p.measureText(text,0,letter),baseline,p);letterLeft[letter]=HeaderAnimation.COLS;
            for(int y=0;y<HeaderAnimation.ROWS;y++)for(int x=0;x<HeaderAnimation.COLS;x++)if(Color.alpha(mask.getPixel(x,y))>95){xs[count]=x;ys[count]=y;letters[count++]=letter;letterLeft[letter]=Math.min(letterLeft[letter],x);}
        }
        titleX=java.util.Arrays.copyOf(xs,count);titleY=java.util.Arrays.copyOf(ys,count);titleLetter=java.util.Arrays.copyOf(letters,count);mask.recycle();
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
        if(stage==HeaderAnimation.WAIT){c.restore();return;}
        for(int i=0;i<titleX.length;i++){
            float x=titleX[i],y=titleY[i];
            if(stage==HeaderAnimation.TITLE&&glitch>=0){int band=3+(cycle.band+glitch*3)%10;if(y>=band&&y<band+cycle.bandSize)x+=cycle.shift;}
            else if(stage==HeaderAnimation.EXIT)y+=(HeaderAnimation.ROWS+2)*HeaderAnimation.ease((elapsed-cycle.title)/(float)cycle.scroll);
            else if(stage==HeaderAnimation.ENTER){int letter=titleLetter[i];x+=(HeaderAnimation.COLS+2-letterLeft[letter])*(1-cycle.letterProgress(letter,elapsed));}
            dot(c,x,y,step,ox,oy,255);
        }
        c.restore();
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)invalidate();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);viewport.set(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getHeight()-getPaddingBottom());
        boolean moving=!frozen&&getWindowVisibility()==VISIBLE&&hasWindowFocus()&&isShown()&&ValueAnimator.areAnimatorsEnabled();long elapsed=moving?animation.elapsed(SystemClock.uptimeMillis()):0;
        renderFrame(c,viewport,elapsed,moving);if(moving)postInvalidateDelayed(animation.cycle().nextDelay(elapsed));
    }
}
