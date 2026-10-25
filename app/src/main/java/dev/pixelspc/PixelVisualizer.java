package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Four pixel displays, all driven by decoded audio. Tap cycles; hold resets. */
public class PixelVisualizer extends View {
    static final String[] MODES={"SPECTRUM","WAVEFORM","STEREO SCOPE","ORBIT"};
    private final Paint paint=new Paint();private final Path path=new Path();
    private final Typeface pixel;
    private PlayerTheme theme=PlayerTheme.ALL[0];
    private android.graphics.drawable.Drawable screen;
    private int displayColor,mode;
    private final float[] heights=new float[AudioSpectrum.BANDS],peaks=new float[AudioSpectrum.BANDS];
    private float[] target=new float[AudioSpectrum.BANDS],trace=new float[AudioWaveform.SAMPLES];
    private float traceGain=1,envelope;
    private boolean playing,loaded,frozen;
    public PixelVisualizer(Context c) {
        super(c);pixel=Typeface.createFromAsset(c.getAssets(),"fonts/pixel.ttf");
        mode=Math.max(0,Math.min(MODES.length-1,c.getSharedPreferences("appearance",0).getInt("visualizer",0)));
        setFocusable(true);setOnClickListener(v->{if(!frozen)selectMode((mode+1)%MODES.length);});
        setOnLongClickListener(v->{if(frozen)return false;selectMode(0);return true;});
        describe();setPlayerTheme(theme);
    }
    int mode(){return mode;}
    private void selectMode(int value){mode=value;getContext().getSharedPreferences("appearance",0).edit().putInt("visualizer",mode).apply();describe();invalidate();}
    private void describe(){setContentDescription(MODES[mode]+" visualizer. Tap for "+MODES[(mode+1)%MODES.length]+". Hold to return to spectrum.");}
    public void setPlayerTheme(PlayerTheme t){theme=t;displayColor=t.spectrumBackground;screen=new PlayerSkin(getContext()).display(displayColor);invalidate();}
    public void setFrame(float[] levels,boolean playing,boolean loaded){setFrame(levels,new float[AudioWaveform.SAMPLES],playing,loaded);}
    public void setFrame(float[] levels,float[] waveform,boolean playing,boolean loaded){
        if(levels.length!=AudioSpectrum.BANDS||waveform.length!=AudioWaveform.SAMPLES)throw new IllegalArgumentException("Invalid visualizer frame");
        frozen=false;target=levels;trace=waveform;this.playing=playing;this.loaded=loaded;gain();invalidate();
    }
    private void gain(){float peak=.25f;for(float value:trace)peak=Math.max(peak,Math.abs(value));traceGain=.85f/peak;}
    void setFrozenFrame(float[] levels){setFrozenFrame(levels,new float[AudioWaveform.SAMPLES]);}
    void setFrozenFrame(float[] levels,float[] waveform){frozen=true;playing=false;loaded=true;trace=waveform;gain();envelope=1;for(int i=0;i<heights.length;i++){heights[i]=levels[i];peaks[i]=levels[i];}invalidate();}
    private void box(Canvas c,int color,float x,float y,float w,float h){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRect(x,y,x+w,y+h,paint);}
    private void label(Canvas c,String s,int color,float x,float y,float size){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(pixel);paint.setTextSize(size);c.drawText(s,x,y,paint);}
    private int bandColor(float value){return value<.5f?PlayerTheme.blend(theme.spectrumLow,theme.spectrumMid,value*2):PlayerTheme.blend(theme.spectrumMid,theme.spectrumHigh,(value-.5f)*2);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);screen.setBounds(0,0,getWidth(),getHeight());screen.draw(canvas);
        if(getWidth()==0)return;
        canvas.save();float scale=getWidth()/320f;canvas.scale(scale,scale);
        float height=getHeight()/scale,base=height-25,step=(base-30)/16f;
        label(canvas,MODES[mode],theme.heading,14,17,7);
        label(canvas,frozen?"EDIT":playing?"LIVE":loaded?"PAUSED":"READY",theme.muted,262,17,6);
        int grid=PlayerTheme.blend(displayColor,theme.text,.09f);boolean moving=false;
        for(int i=0;i<heights.length;i++){
            if(!frozen){heights[i]=Math.max(playing?target[i]:0,heights[i]*.84f);peaks[i]=Math.max(heights[i],peaks[i]-.012f);}
            moving|=peaks[i]>.01f;
        }
        envelope=frozen||playing?1:envelope*.84f;if(envelope<.005f)envelope=0;
        if(mode==0){
            for(int row=0;row<=4;row++)box(canvas,grid,14,30+(base-30)*row/4,292,1);
            spectrum(canvas,base,step,grid);
            label(canvas,"40Hz",theme.muted,14,height-8,5);label(canvas,"250",theme.muted,91,height-8,5);label(canvas,"1k",theme.muted,160,height-8,5);label(canvas,"4k",theme.muted,225,height-8,5);label(canvas,"14k",theme.muted,289,height-8,5);
        }else{
            if(mode==1)waveform(canvas,30,base+2,grid);
            else if(mode==2)scope(canvas,30,base+2,grid);
            else orbit(canvas,30,base+2,grid);
            label(canvas,mode==1?"LEFT / RIGHT":mode==2?"STEREO PHASE":"24 FREQUENCY BANDS",theme.muted,14,height-8,5);
            label(canvas,mode==3?"FFT":"PCM",theme.muted,289,height-8,5);
        }
        canvas.restore();if(!frozen&&(playing||moving||envelope>0)&&isShown())postInvalidateOnAnimation();
    }
    private void spectrum(Canvas canvas,float base,float step,int grid){
        for(int i=0;i<heights.length;i++) {
            int segments=Math.round(heights[i]*16); float x=16+i*12;
            for(int j=0;j<16;j++) {
                int lit=theme.spectrumGradient()?(j<8?PlayerTheme.blend(theme.spectrumLow,theme.spectrumMid,j/7f):PlayerTheme.blend(theme.spectrumMid,theme.spectrumHigh,(j-8)/7f)):(j>=12?theme.spectrumHigh:theme.spectrumLow);
                int color=j<segments?lit:grid;
                box(canvas,color,x,base-j*step,8,Math.max(1,step-1.5f));
            }
            if(peaks[i]>.02f) box(canvas,theme.spectrumPeak,x,base-Math.round(peaks[i]*16)*step,8,1.5f);
        }
    }
    private void stroke(Canvas c,int color,float width){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setColor(color);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
    private void waveform(Canvas c,float top,float bottom,int grid){
        float height=bottom-top;
        for(int channel=0;channel<2;channel++){
            float center=top+height*(channel==0?.25f:.75f);
            box(c,grid,14,center,292,1);path.rewind();
            for(int i=0;i<AudioWaveform.POINTS;i++){
                float x=14+i*292f/(AudioWaveform.POINTS-1),y=center-trace[i*2+channel]*traceGain*envelope*height*.22f;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            stroke(c,channel==0?theme.spectrumLow:theme.spectrumHigh,1.5f);
        }
    }
    private void scope(Canvas c,float top,float bottom,int grid){
        float center=(top+bottom)/2,radius=(bottom-top)/2-3;
        box(c,grid,160,top,1,bottom-top);box(c,grid,160-radius,center,radius*2,1);
        path.rewind();for(int i=0;i<=24;i++){double angle=i*Math.PI/12;float x=160+(float)Math.cos(angle)*radius,y=center+(float)Math.sin(angle)*radius;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}stroke(c,grid,1);
        path.rewind();for(int i=0;i<AudioWaveform.POINTS;i++){
            float left=trace[i*2],right=trace[i*2+1];float x=160+(left-right)*.5f*traceGain*envelope*radius,y=center-(left+right)*.5f*traceGain*envelope*radius;
            if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
        }
        stroke(c,theme.spectrumLow,1.5f);box(c,theme.spectrumPeak,159,center-1,2,2);
    }
    private void orbit(Canvas c,float top,float bottom,int grid){
        float center=(top+bottom)/2,outer=(bottom-top)/2-2;
        for(int i=0;i<heights.length;i++){
            double angle=i*Math.PI*2/heights.length-Math.PI/2;int lit=Math.round(heights[i]*8);
            for(int j=0;j<8;j++){float radius=10+(outer-10)*j/7;float x=160+(float)Math.cos(angle)*radius,y=center+(float)Math.sin(angle)*radius;box(c,j<lit?bandColor(j/7f):grid,x-1,y-1,2,2);}
            if(peaks[i]>.02f){float radius=10+(outer-10)*peaks[i];box(c,theme.spectrumPeak,160+(float)Math.cos(angle)*radius-1,center+(float)Math.sin(angle)*radius-1,2,2);}
        }
        label(c,"9x",theme.heading,153,center+3,7);
    }
}
