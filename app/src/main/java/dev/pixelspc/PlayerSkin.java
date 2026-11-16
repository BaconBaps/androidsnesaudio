package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.Gravity;

/** Original raster chrome: tiled grain and compiled nine-patches, never stretched textures. */
final class PlayerSkin {
    private final Context context;
    private final boolean chipAmp5;
    PlayerSkin(Context context) { this(context,PlayerTheme.ALL[0]); }
    PlayerSkin(Context context,PlayerTheme theme) { this.context=context;chipAmp5=theme.chipAmp5(); }
    private Drawable patch(int id) {
        Drawable d=context.getDrawable(id).mutate();
        if(d instanceof NinePatchDrawable) ((NinePatchDrawable)d).setFilterBitmap(false);
        return d;
    }
    private Drawable edge(int id,int color) {
        Drawable d=patch(id);
        if(PlayerTheme.luminance(color)>.35) {
            // Bake edge opacity into the pixels: Android button state containers
            // reapply alpha and color filters to their child drawables.
            Bitmap source=BitmapFactory.decodeResource(context.getResources(),id);
            Bitmap soft=source.copy(Bitmap.Config.ARGB_8888,true);
            int[] pixels=new int[soft.getWidth()*soft.getHeight()];
            soft.getPixels(pixels,0,soft.getWidth(),0,0,soft.getWidth(),soft.getHeight());
            for(int i=0;i<pixels.length;i++) pixels[i]=(pixels[i]&0xffffff)|(Math.round(Color.alpha(pixels[i])*.35f)<<24);
            soft.setPixels(pixels,0,soft.getWidth(),0,0,soft.getWidth(),soft.getHeight());
            NinePatchDrawable softened=new NinePatchDrawable(context.getResources(),soft,source.getNinePatchChunk(),new Rect(),null);
            softened.setFilterBitmap(false); return softened;
        }
        return d;
    }
    private BitmapDrawable tile(int id) {
        BitmapDrawable d=(BitmapDrawable)context.getDrawable(id).mutate();
        d.setTileModeXY(Shader.TileMode.REPEAT,Shader.TileMode.REPEAT); d.setFilterBitmap(false); return d;
    }
    private Drawable face(int color,int edge,boolean grain) {
        if(chipAmp5)return new PlasticSurface(context,color,edge==R.drawable.skin_well||edge==R.drawable.skin_key_down?PlasticSurface.RECESSED:PlasticSurface.RAISED,false);
        BitmapDrawable brushed=grain?tile(R.drawable.skin_brushed):null;
        // Light surfaces need gentler grain to retain their text contrast.
        if(brushed!=null && PlayerTheme.luminance(color)>.35) {
            brushed.setAlpha(64); brushed.setColorFilter(0xffffffff,PorterDuff.Mode.SRC_IN);
        }
        Drawable[] parts=grain?new Drawable[]{new ColorDrawable(color),brushed,edge(edge,color)}:new Drawable[]{new ColorDrawable(color),edge(edge,color)};
        LayerDrawable d=new LayerDrawable(parts); d.setPaddingMode(LayerDrawable.PADDING_MODE_STACK); return d;
    }
    Drawable chassis(int color) { return face(color,R.drawable.skin_chassis,true); }
    Drawable header(int color){return chipAmp5?new PlasticSurface(context,color,PlasticSurface.RECESSED,true):display(color);}
    Drawable background(int color) { if(chipAmp5)return new PlasticSurface(context,color,PlasticSurface.PLAIN,false);return new ColorDrawable(color); }
    Drawable panel(int color) { return face(color,R.drawable.skin_panel,PlayerTheme.luminance(color)<.35); }
    Drawable selection(int color) { return face(color,R.drawable.skin_panel,false); }
    Drawable well(int color) { return face(color,R.drawable.skin_well,false); }
    Drawable display(int color) {
        if(chipAmp5)return new PlasticSurface(context,color,PlasticSurface.RECESSED,true);
        BitmapDrawable lines=tile(R.drawable.skin_scanlines);
        if(PlayerTheme.luminance(color)>.35) lines.setAlpha(0);
        return new LayerDrawable(new Drawable[]{new ColorDrawable(color),lines,edge(R.drawable.skin_well,color)});
    }
    Drawable button(int color) {
        StateListDrawable d=new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed},face(color,R.drawable.skin_key_down,false));
        d.addState(new int[]{android.R.attr.state_focused},face(PlayerTheme.blend(color,0xffffffff,.10f),R.drawable.skin_key,false));
        d.addState(new int[]{},face(color,R.drawable.skin_key,false)); return d;
    }
    Drawable rail(int color) {
        if(chipAmp5)return new PlasticSurface(context,color,PlasticSurface.RAISED,false);
        Drawable d=new LayerDrawable(new Drawable[]{new ColorDrawable(color),edge(R.drawable.skin_rail,color)}); return d;
    }
    Drawable thumb(int color) {
        if(chipAmp5){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{PlayerTheme.blend(color,0xffffffff,.15f),PlayerTheme.blend(color,0xff000000,.25f)});float density=context.getResources().getDisplayMetrics().density;d.setCornerRadius(4*density);d.setStroke(Math.max(1,Math.round(density)),0xff080b0c);d.setSize(Math.round(12*density),Math.round(20*density));return d;}
        Drawable d=context.getDrawable(R.drawable.skin_thumb).mutate();
        if(d instanceof BitmapDrawable) ((BitmapDrawable)d).setFilterBitmap(false);
        d.setColorFilter(color,PorterDuff.Mode.MULTIPLY); return d;
    }
    Drawable progress(PlayerTheme theme) {
        Drawable track=chipAmp5?new DotRail(context,PlayerTheme.blend(theme.background,theme.text,.16f)):well(PlayerTheme.blend(theme.background,0xff000000,.18f));
        ClipDrawable fill=new ClipDrawable(chipAmp5?new DotRail(context,theme.secondary):rail(theme.accent),Gravity.START,ClipDrawable.HORIZONTAL);
        LayerDrawable layers=new LayerDrawable(new Drawable[]{track,fill}) {
            // ProgressBar adopts a drawable's minimum height on Android 8 too.
            // Frame source dimensions must not turn the seek track into a tall box.
            @Override public int getIntrinsicHeight() { return Math.round(10*context.getResources().getDisplayMetrics().density); }
            @Override public int getMinimumHeight() { return getIntrinsicHeight(); }
        };
        layers.setId(0,android.R.id.background); layers.setId(1,android.R.id.progress); return layers;
    }
    private static final class DotRail extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final float density;
        DotRail(Context c,int color){density=c.getResources().getDisplayMetrics().density;paint.setColor(color);}
        @Override public void draw(Canvas c){Rect b=getBounds();for(float x=b.left+2*density;x<b.right;x+=5*density)c.drawCircle(x,b.exactCenterY(),1.6f*density,paint);}
        @Override public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
