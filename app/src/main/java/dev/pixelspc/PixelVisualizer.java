package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Segmented pixel spectrum and falling peak markers, driven only by decoded PCM. */
public class PixelVisualizer extends View {
    private final Paint paint=new Paint();
    private final Typeface pixel;
    private PlayerTheme theme=PlayerTheme.ALL[0];
    private android.graphics.drawable.Drawable screen;
    private int displayColor;
    private final float[] heights=new float[AudioSpectrum.BANDS],peaks=new float[AudioSpectrum.BANDS];
    private float[] target=new float[AudioSpectrum.BANDS];
    private boolean playing,loaded;
    public PixelVisualizer(Context c) {
        super(c); pixel=Typeface.createFromAsset(c.getAssets(),"fonts/pixel.ttf");
        setContentDescription("Audio spectrum visualizer, 24 frequency bands"); setPlayerTheme(theme);
    }
    public void setPlayerTheme(PlayerTheme t) { theme=t; displayColor=t.dark?PlayerTheme.blend(t.background,0xff000000,.42f):t.surface; screen=new PlayerSkin(getContext()).display(displayColor); invalidate(); }
    public void setFrame(float[] levels,boolean playing,boolean loaded) {
        if(levels.length!=AudioSpectrum.BANDS) throw new IllegalArgumentException("Expected 24 bands");
        target=levels; this.playing=playing; this.loaded=loaded; invalidate();
    }
    private void box(Canvas c,int color,float x,float y,float w,float h) { paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint); }
    private void label(Canvas c,String s,int color,float x,float y,float size) { paint.setColor(color); paint.setTypeface(pixel); paint.setTextSize(size); c.drawText(s,x,y,paint); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); screen.setBounds(0,0,getWidth(),getHeight()); screen.draw(canvas); canvas.save(); canvas.scale(getWidth()/320f,getHeight()/154f);
        label(canvas,"SPECTRUM",theme.accent,14,17,7);
        label(canvas,playing?"LIVE":loaded?"PAUSED":"READY",theme.muted,262,17,6);
        int grid=PlayerTheme.blend(displayColor,theme.text,.09f);
        for(int y=34;y<=130;y+=24) box(canvas,grid,14,y,292,1);
        boolean moving=false;
        for(int i=0;i<heights.length;i++) {
            heights[i]=Math.max(playing?target[i]:0,heights[i]*.84f);
            peaks[i]=Math.max(heights[i],peaks[i]-.012f);
            moving|=peaks[i]>.01f;
            int segments=Math.round(heights[i]*16); float x=16+i*12;
            for(int j=0;j<16;j++) {
                int color=j<segments?(j>=12?theme.secondary:theme.accent):grid;
                box(canvas,color,x,126-j*6,8,4);
            }
            if(peaks[i]>.02f) box(canvas,theme.secondary,x,126-Math.round(peaks[i]*16)*6,8,2);
        }
        label(canvas,"40Hz",theme.muted,14,146,5); label(canvas,"250",theme.muted,91,146,5);
        label(canvas,"1k",theme.muted,160,146,5); label(canvas,"4k",theme.muted,225,146,5); label(canvas,"14k",theme.muted,289,146,5);
        canvas.restore(); if((playing||moving)&&isShown()) postInvalidateOnAnimation();
    }
}
