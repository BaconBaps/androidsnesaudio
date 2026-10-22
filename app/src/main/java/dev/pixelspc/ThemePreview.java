package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A small rendered image of the player palette, independent of live playback. */
public final class ThemePreview extends View {
    private final PlayerTheme theme;
    private final Paint paint=new Paint();
    private final android.graphics.drawable.Drawable chassis,well,key;
    private static final int[] BARS={3,5,8,6,4,7,9,5,3,6,8,4};
    public ThemePreview(Context context) { this(context,PlayerTheme.ALL[0],false); }
    public ThemePreview(Context context,PlayerTheme theme,boolean selected) {
        super(context); this.theme=theme; PlayerSkin skin=new PlayerSkin(context); chassis=skin.chassis(theme.background); well=skin.well(theme.surface); key=skin.button(theme.accent); setSelected(selected); setClickable(true); setFocusable(true);
        setContentDescription("Select "+theme.name+" theme"+(selected?", selected":""));
    }
    private void box(Canvas c,int color,float x,float y,float w,float h) { paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint); }
    private void text(Canvas c,String text,int color,float x,float y,float size,boolean bold) {
        paint.setColor(color); paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); paint.setTextSize(size); c.drawText(text,x,y,paint);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); canvas.scale(getWidth()/160f,getHeight()/150f);
        box(canvas,theme.background,0,0,160,150);
        chassis.setBounds(8,8,152,113); chassis.draw(canvas);
        text(canvas,"PixelSPC",theme.text,15,20,8,true);
        box(canvas,theme.border,15,26,130,9); text(canvas,"LIBRARY",theme.accent,66,33,5,true);
        well.setBounds(13,37,147,75); well.draw(canvas);
        for(int i=0;i<BARS.length;i++) for(int j=0;j<9;j++)
            box(canvas,j<BARS[i]?(j>6?theme.secondary:theme.accent):theme.border,17+i*11,70-j*3,7,2);
        for(int row=0;row<2;row++) { box(canvas,row==0?theme.accent:theme.muted,17,80+row*9,64-row*13,2); box(canvas,theme.border,17,85+row*9,126,1); }
        box(canvas,theme.accent,17,101,91,1); key.setBounds(123,96,146,110); key.draw(canvas);
        text(canvas,"▶",theme.onAccent,131,105,7,true);
        text(canvas,theme.name,theme.text,9,134,11,true);
        paint.setColor(isSelected()?theme.accent:theme.border); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(isSelected()?3:1);
        canvas.drawRect(1.5f,1.5f,158.5f,148.5f,paint); paint.setStyle(Paint.Style.FILL);
        if(isSelected()) { box(canvas,theme.accent,133,8,19,16); text(canvas,"✓",theme.onAccent,137,20,12,true); }
        canvas.restore();
    }
}
