package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A small rendered image of the player palette, independent of live playback. */
public final class ThemePreview extends View {
    private final PlayerTheme theme,playbackTheme,listTheme;
    private final Paint paint=new Paint();
    private final android.text.TextPaint headingPaint=new android.text.TextPaint();
    private android.text.StaticLayout heading;
    private final ChipLogo logo;
    private final RectF logoBox=new RectF(15,6,145,23);
    private final android.graphics.drawable.Drawable well,key,chassis;
    private static final int[] BARS={3,5,8,6,4,7,9,5,3,6,8,4};
    public ThemePreview(Context context) { this(context,PlayerTheme.ALL[0],false); }
    public ThemePreview(Context context,PlayerTheme theme,boolean selected) {
        super(context); logo=new ChipLogo(context); this.theme=theme; playbackTheme=theme.nowPlaying(); listTheme=theme.library(); PlayerSkin skin=new PlayerSkin(context,theme); chassis=skin.chassis(theme.background);well=skin.well(playbackTheme.background); key=skin.button(playbackTheme.buttonFace(playbackTheme.accent)); setSelected(selected); setClickable(true); setFocusable(true);
        setContentDescription("Select "+theme.name+" theme"+(selected?", selected":""));
        headingPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/pixel.ttf"));
        headingPaint.setTextSize(7*getResources().getDisplayMetrics().scaledDensity);
        headingPaint.setColor(theme.heading);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        int inset=Math.round(9*getResources().getDisplayMetrics().density);
        String label=theme.name.toUpperCase(java.util.Locale.ROOT);
        heading=android.text.StaticLayout.Builder.obtain(label,0,label.length(),headingPaint,Math.max(1,w-2*inset))
            .setIncludePad(false).setMaxLines(2).setEllipsize(android.text.TextUtils.TruncateAt.END).build();
    }
    private void box(Canvas c,int color,float x,float y,float w,float h) { paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint); }
    private void text(Canvas c,String text,int color,float x,float y,float size,boolean bold) {
        paint.setColor(color); paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); paint.setTextSize(size); c.drawText(text,x,y,paint);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); canvas.scale(getWidth()/160f,getHeight()/150f);
        box(canvas,theme.background,0,0,160,150);if(theme.reactor()){chassis.setBounds(0,0,160,150);chassis.draw(canvas);}
        logo.drawInto(canvas,logoBox);
        box(canvas,theme.libraryButtonBackground(),15,26,130,9); text(canvas,"LIBRARY",theme.accent,66,33,5,true);
        box(canvas,listTheme.background,15,38,130,20);
        for(int row=0;row<2;row++) { box(canvas,row==0?listTheme.accent:listTheme.muted,17,42+row*9,64-row*13,2); box(canvas,theme.border,17,47+row*9,126,1); }
        well.setBounds(13,61,147,111); well.draw(canvas);
        int grid=PlayerTheme.blend(playbackTheme.background,playbackTheme.text,.12f);
        for(int i=0;i<BARS.length;i++) for(int j=0;j<9;j++){
            int lit=playbackTheme.spectrumGradient()?(j<4?PlayerTheme.blend(playbackTheme.spectrumLow,playbackTheme.spectrumMid,j/3f):PlayerTheme.blend(playbackTheme.spectrumMid,playbackTheme.spectrumHigh,(j-4)/4f)):(j>6?playbackTheme.spectrumHigh:playbackTheme.spectrumLow);
            box(canvas,j<BARS[i]?lit:grid,17+i*11,88-j*2.3f,7,1.5f);}
        box(canvas,playbackTheme.accent,17,95,64,2);
        box(canvas,playbackTheme.accent,17,103,91,1); key.setBounds(123,96,146,110); key.draw(canvas);
        text(canvas,"▶",playbackTheme.buttonText(playbackTheme.onAccent),131,105,7,true);
        paint.setColor(isSelected()?theme.accent:theme.border); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(isSelected()?3:1);
        canvas.drawRect(1.5f,1.5f,158.5f,148.5f,paint); paint.setStyle(Paint.Style.FILL);
        if(isSelected()) { box(canvas,theme.accent,133,8,19,16); text(canvas,"✓",theme.onAccent,137,20,12,true); }
        canvas.restore();
        if(heading!=null){canvas.save();canvas.translate(9*getResources().getDisplayMetrics().density,getHeight()*119/150f);heading.draw(canvas);canvas.restore();}
    }
}
