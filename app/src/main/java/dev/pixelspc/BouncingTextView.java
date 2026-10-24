package dev.pixelspc;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** Overflowing titles travel to the end and back, pausing at each edge. */
public final class BouncingTextView extends TextView {
    private long started;private boolean frozen;
    void setFrozen(boolean frozen){this.frozen=frozen;invalidate();}
    public BouncingTextView(Context context) {
        super(context); setSingleLine(true); setHorizontallyScrolling(true);
        setEllipsize(null); setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
    }
    @Override public void setText(CharSequence text,BufferType type) {
        if(getText()!=null && TextUtils.equals(getText(),text)) return;
        super.setText(text,type); started=SystemClock.uptimeMillis();
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) { super.onSizeChanged(w,h,oldW,oldH); started=SystemClock.uptimeMillis(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); started=SystemClock.uptimeMillis(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); if(visibility==View.VISIBLE) { started=SystemClock.uptimeMillis(); invalidate(); } }
    @Override public void scrollTo(int x,int y) { super.scrollTo(0,0); }
    static float offsetAt(long elapsed,float overflow,float pixelsPerSecond) {
        if(overflow<=0 || pixelsPerSecond<=0) return 0;
        double travel=overflow/pixelsPerSecond*1000,phase=Math.max(0,elapsed)%(2*travel+2400);
        if(phase<1200) return 0;
        phase-=1200;
        if(phase<travel) return (float)(overflow*(1-Math.cos(Math.PI*phase/travel))/2);
        phase-=travel;
        if(phase<1200) return overflow;
        phase-=1200;
        return (float)(overflow*(1+Math.cos(Math.PI*phase/travel))/2);
    }
    float currentOffset() {
        if(frozen)return 0;
        float width=getLayout()==null?0:getLayout().getLineWidth(0);
        return offsetAt(SystemClock.uptimeMillis()-started,Math.max(0,width-getWidth()+getPaddingLeft()+getPaddingRight()),32*getResources().getDisplayMetrics().density);
    }
    @Override protected void onDraw(Canvas canvas) {
        float offset=currentOffset(); canvas.save();
        canvas.clipRect(getPaddingLeft(),0,getWidth()-getPaddingRight(),getHeight()); canvas.translate(-offset,0);
        super.onDraw(canvas); canvas.restore();
        if(!frozen && getLayout()!=null && getLayout().getLineWidth(0)>getWidth()-getPaddingLeft()-getPaddingRight() && isShown()) postInvalidateOnAnimation();
    }
}
