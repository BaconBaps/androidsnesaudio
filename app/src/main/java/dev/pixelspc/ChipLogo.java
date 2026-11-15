package dev.pixelspc;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Doto wordmark and matching music-chip dots, aligned inside the shared header display. */
final class ChipLogo extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DotGlyph chip;
    private final int color;
    private final RectF viewport=new RectF();
    ChipLogo(Context c){this(c,ThemeStore.selected(c));}
    ChipLogo(Context c,PlayerTheme theme){super(c);color=theme.headerInk();chip=new DotGlyph(false,color);paint.setTypeface(new Typeface.Builder(c.getAssets(),"fonts/doto.ttf").setFontVariationSettings("'ROND' 100, 'wght' 800").build());setContentDescription(c.getString(R.string.app_name));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void drawInto(Canvas c,RectF box){
        float icon=Math.min(box.height()*.80f,box.width()*.17f),gap=icon*.25f;
        chip.setBounds(Math.round(box.left),Math.round(box.centerY()-icon/2),Math.round(box.left+icon),Math.round(box.centerY()+icon/2));chip.draw(c);
        paint.setTextSize(box.height()*.85f);float width=paint.measureText("ChipAmp9x");float available=box.width()-icon-gap;
        if(width>available)paint.setTextSize(paint.getTextSize()*Math.max(0,available)/width);
        Paint.FontMetrics fm=paint.getFontMetrics();float x=box.left+icon+gap,y=box.centerY()-(fm.ascent+fm.descent)/2;
        paint.setColor(color);paint.setAlpha(255);c.drawText("ChipAmp9x",x,y,paint);
    }
    @Override protected void onDraw(Canvas c){super.onDraw(c);viewport.set(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getHeight()-getPaddingBottom());drawInto(c,viewport);}
}
