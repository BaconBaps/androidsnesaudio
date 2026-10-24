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
    private boolean playing,loaded,frozen;
    public PixelVisualizer(Context c) {
        super(c); pixel=Typeface.createFromAsset(c.getAssets(),"fonts/pixel.ttf");
        setContentDescription("Audio spectrum visualizer, 24 frequency bands"); setPlayerTheme(theme);
    }
    public void setPlayerTheme(PlayerTheme t) { theme=t; displayColor=t.spectrumBackground; screen=new PlayerSkin(getContext()).display(displayColor); invalidate(); }
    public void setFrame(float[] levels,boolean playing,boolean loaded) {
        if(levels.length!=AudioSpectrum.BANDS) throw new IllegalArgumentException("Expected 24 bands");
        frozen=false;target=levels; this.playing=playing; this.loaded=loaded; invalidate();
    }
    void setFrozenFrame(float[] levels){frozen=true;playing=false;loaded=true;for(int i=0;i<heights.length;i++){heights[i]=levels[i];peaks[i]=levels[i];}invalidate();}
    private void box(Canvas c,int color,float x,float y,float w,float h) { paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint); }
    private void label(Canvas c,String s,int color,float x,float y,float size) { paint.setColor(color); paint.setTypeface(pixel); paint.setTextSize(size); c.drawText(s,x,y,paint); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); screen.setBounds(0,0,getWidth(),getHeight()); screen.draw(canvas); canvas.save(); float scale=getWidth()/320f; canvas.scale(scale,scale);
        float height=getHeight()/scale,base=height-25,step=(base-30)/16f;
        label(canvas,"SPECTRUM",theme.heading,14,17,7);
        label(canvas,frozen?"EDIT":playing?"LIVE":loaded?"PAUSED":"READY",theme.muted,262,17,6);
        int grid=PlayerTheme.blend(displayColor,theme.text,.09f);
        for(int row=0;row<=4;row++) box(canvas,grid,14,30+(base-30)*row/4,292,1);
        boolean moving=false;
        for(int i=0;i<heights.length;i++) {
            if(!frozen){heights[i]=Math.max(playing?target[i]:0,heights[i]*.84f);peaks[i]=Math.max(heights[i],peaks[i]-.012f);}
            moving|=peaks[i]>.01f;
            int segments=Math.round(heights[i]*16); float x=16+i*12;
            for(int j=0;j<16;j++) {
                int lit=theme.spectrumGradient()?(j<8?PlayerTheme.blend(theme.spectrumLow,theme.spectrumMid,j/7f):PlayerTheme.blend(theme.spectrumMid,theme.spectrumHigh,(j-8)/7f)):(j>=12?theme.spectrumHigh:theme.spectrumLow);
                int color=j<segments?lit:grid;
                box(canvas,color,x,base-j*step,8,Math.max(1,step-1.5f));
            }
            if(peaks[i]>.02f) box(canvas,theme.spectrumPeak,x,base-Math.round(peaks[i]*16)*step,8,1.5f);
        }
        label(canvas,"40Hz",theme.muted,14,height-8,5); label(canvas,"250",theme.muted,91,height-8,5);
        label(canvas,"1k",theme.muted,160,height-8,5); label(canvas,"4k",theme.muted,225,height-8,5); label(canvas,"14k",theme.muted,289,height-8,5);
        canvas.restore(); if(!frozen&&(playing||moving)&&isShown()) postInvalidateOnAnimation();
    }
}
