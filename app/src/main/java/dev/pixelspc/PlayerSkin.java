package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.Gravity;

/** Original raster chrome: tiled grain and compiled nine-patches, never stretched textures. */
final class PlayerSkin {
    private final Context context;
    PlayerSkin(Context context) { this.context=context; }
    private Drawable patch(int id) {
        Drawable d=context.getDrawable(id).mutate();
        if(d instanceof NinePatchDrawable) ((NinePatchDrawable)d).setFilterBitmap(false);
        return d;
    }
    private BitmapDrawable tile(int id) {
        BitmapDrawable d=(BitmapDrawable)context.getDrawable(id).mutate();
        d.setTileModeXY(Shader.TileMode.REPEAT,Shader.TileMode.REPEAT); d.setFilterBitmap(false); return d;
    }
    private Drawable face(int color,int edge,boolean grain) {
        BitmapDrawable brushed=grain?tile(R.drawable.skin_brushed):null;
        // Light surfaces need gentler grain to retain their text contrast.
        if(brushed!=null && PlayerTheme.luminance(color)>.35) {
            brushed.setAlpha(64); brushed.setColorFilter(0xffffffff,PorterDuff.Mode.SRC_IN);
        }
        Drawable[] parts=grain?new Drawable[]{new ColorDrawable(color),brushed,patch(edge)}:new Drawable[]{new ColorDrawable(color),patch(edge)};
        LayerDrawable d=new LayerDrawable(parts); d.setPaddingMode(LayerDrawable.PADDING_MODE_STACK); return d;
    }
    Drawable chassis(int color) { return face(color,R.drawable.skin_chassis,true); }
    Drawable panel(int color) { return face(color,R.drawable.skin_panel,PlayerTheme.luminance(color)<.35); }
    Drawable selection(int color) { return face(color,R.drawable.skin_panel,false); }
    Drawable well(int color) { return face(color,R.drawable.skin_well,false); }
    Drawable display(int color) {
        BitmapDrawable lines=tile(R.drawable.skin_scanlines);
        if(PlayerTheme.luminance(color)>.35) lines.setAlpha(0);
        return new LayerDrawable(new Drawable[]{new ColorDrawable(color),lines,patch(R.drawable.skin_well)});
    }
    Drawable button(int color) {
        StateListDrawable d=new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed},face(color,R.drawable.skin_key_down,false));
        d.addState(new int[]{android.R.attr.state_focused},face(PlayerTheme.blend(color,0xffffffff,.10f),R.drawable.skin_key,false));
        d.addState(new int[]{},face(color,R.drawable.skin_key,false)); return d;
    }
    Drawable rail(int color) {
        Drawable d=new LayerDrawable(new Drawable[]{new ColorDrawable(color),patch(R.drawable.skin_rail)}); return d;
    }
    Drawable thumb(int color) {
        Drawable d=context.getDrawable(R.drawable.skin_thumb).mutate();
        if(d instanceof BitmapDrawable) ((BitmapDrawable)d).setFilterBitmap(false);
        d.setColorFilter(color,PorterDuff.Mode.MULTIPLY); return d;
    }
    Drawable progress(PlayerTheme theme) {
        Drawable track=well(PlayerTheme.blend(theme.background,0xff000000,.18f));
        ClipDrawable fill=new ClipDrawable(rail(theme.accent),Gravity.START,ClipDrawable.HORIZONTAL);
        LayerDrawable layers=new LayerDrawable(new Drawable[]{track,fill}) {
            // ProgressBar adopts a drawable's minimum height on Android 8 too.
            // Frame source dimensions must not turn the seek track into a tall box.
            @Override public int getIntrinsicHeight() { return Math.round(10*context.getResources().getDisplayMetrics().density); }
            @Override public int getMinimumHeight() { return getIntrinsicHeight(); }
        };
        layers.setId(0,android.R.id.background); layers.setId(1,android.R.id.progress); return layers;
    }
}
