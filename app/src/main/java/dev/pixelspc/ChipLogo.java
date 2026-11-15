package dev.pixelspc;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Two-line Doto hi-fi display. Preview cards stay static; the live header scrolls gently. */
final class ChipLogo extends View {
    static final String SUBTITLE="Native format videogame music player";
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    private final RectF viewport=new RectF();
    private long started;private boolean frozen;
    ChipLogo(Context c){this(c,ThemeStore.selected(c));}
    ChipLogo(Context c,PlayerTheme theme){super(c);color=theme.headerInk();paint.setTypeface(new Typeface.Builder(c.getAssets(),"fonts/doto.ttf").setFontVariationSettings("'ROND' 100, 'wght' 800").build());setContentDescription(c.getString(R.string.app_name));setTooltipText(SUBTITLE);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void setFrozen(boolean value){frozen=value;invalidate();}
    private static float ease(float value){return value*value*(3-2*value);}
    static float subtitleOffset(long elapsed,float width,float viewport,float speed){
        if(speed<=0||width<=0||viewport<=0)return 0;
        double exit=width/speed*1000,enter=viewport/speed*1000,phase=Math.max(0,elapsed)%(4500+exit+enter);
        if(phase<4500)return 0;
        phase-=4500;
        if(phase<exit)return -width*ease((float)(phase/exit));
        return viewport*(1-ease((float)((phase-exit)/enter)));
    }
    void drawInto(Canvas c,RectF box){drawInto(c,box,0,false);}
    private void drawInto(Canvas c,RectF box,long elapsed,boolean animate){
        if(box.width()<=0||box.height()<=0)return;
        float inset=box.height()*.10f,left=box.left+inset,available=Math.max(1,box.width()-2*inset);
        c.save();c.clipRect(box);
        paint.setTextSize(box.height()*.56f);float width=paint.measureText("ChipAmp9x");if(width>available)paint.setTextSize(paint.getTextSize()*available/width);
        Paint.FontMetrics fm=paint.getFontMetrics();float titleY=box.top+box.height()*.34f-(fm.ascent+fm.descent)/2;
        c.save();if(animate&&elapsed<900)c.clipRect(box.left,box.top,box.left+box.width()*ease(Math.min(1,elapsed/900f)),box.bottom);
        paint.setColor(color);c.drawText("ChipAmp9x",left,titleY,paint);c.restore();
        paint.setTextSize(box.height()*.205f);if(!animate&&paint.measureText(SUBTITLE)>available)paint.setTextSize(paint.getTextSize()*available/paint.measureText(SUBTITLE));fm=paint.getFontMetrics();float subtitleY=box.top+box.height()*.80f-(fm.ascent+fm.descent)/2;
        float shift=animate?subtitleOffset(elapsed,paint.measureText(SUBTITLE),available,18*getResources().getDisplayMetrics().density):0;
        c.save();c.clipRect(left,box.top+box.height()*.60f,left+available,box.bottom);paint.setColor(PlayerTheme.blend(color,0xff000000,.08f));c.drawText(SUBTITLE,left+shift,subtitleY,paint);c.restore();c.restore();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();started=SystemClock.uptimeMillis();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)invalidate();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);viewport.set(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getHeight()-getPaddingBottom());
        boolean animate=!frozen&&hasWindowFocus()&&isShown()&&ValueAnimator.areAnimatorsEnabled();
        drawInto(c,viewport,Math.max(0,SystemClock.uptimeMillis()-started),animate);if(animate)postInvalidateDelayed(50);
    }
}
