package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Rounded molded-plastic bezels; fixed-width geometry never stretches corner detail. */
final class PlasticSurface extends Drawable {
    static final int RAISED=0,RECESSED=1,PLAIN=2;
    private static Bitmap grain,dots;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final float density;
    private final int color,mode;
    private final boolean grid;
    private final BitmapShader texture,pattern;
    private int alpha=255;private ColorFilter filter;private LinearGradient rim,sheen;
    PlasticSurface(Context c,int color,int mode,boolean grid){
        this.color=color;this.mode=mode;this.grid=grid;density=c.getResources().getDisplayMetrics().density;
        synchronized(PlasticSurface.class){
            if(grain==null){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;grain=BitmapFactory.decodeResource(c.getResources(),R.drawable.phosphor_plastic,options);}
            if(dots==null){dots=Bitmap.createBitmap(6,6,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(dots);Paint dot=new Paint(Paint.ANTI_ALIAS_FLAG);dot.setColor(0x3489ae9e);canvas.drawCircle(3,3,1.35f,dot);}
        }
        texture=new BitmapShader(grain,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);pattern=new BitmapShader(dots,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
        Matrix matrix=new Matrix();matrix.setScale(density,density);texture.setLocalMatrix(matrix);pattern.setLocalMatrix(matrix);
    }
    @Override protected void onBoundsChange(Rect bounds){
        rim=new LinearGradient(0,bounds.top,0,Math.max(bounds.top+1,bounds.bottom),PlayerTheme.blend(color,mode==RECESSED?0xff000000:0xffffffff,mode==RECESSED?.8f:.38f),PlayerTheme.blend(color,mode==RECESSED?0xffffffff:0xff000000,mode==RECESSED?.23f:.75f),Shader.TileMode.CLAMP);
        sheen=new LinearGradient(0,bounds.top,0,Math.max(bounds.top+1,bounds.bottom),PlayerTheme.blend(color,0xffffffff,PlayerTheme.luminance(color)>.35?0:.035f),PlayerTheme.blend(color,0xff000000,PlayerTheme.luminance(color)>.35?0:.08f),Shader.TileMode.CLAMP);
    }
    private void layer(Canvas c,float inset,float radius,int fill){rect.set(getBounds());rect.inset(inset,inset);if(rect.width()<=0||rect.height()<=0)return;paint.setShader(null);float u=Math.min(density,Math.min(getBounds().width(),getBounds().height())/24f);if(inset==u)paint.setShader(rim);else if(inset==5*u&&mode==RAISED)paint.setShader(sheen);paint.setColor(fill);paint.setAlpha(Color.alpha(fill)*alpha/255);paint.setColorFilter(filter);c.drawRoundRect(rect,Math.max(0,radius-inset),Math.max(0,radius-inset),paint);}
    @Override public void draw(Canvas c){
        float size=Math.min(getBounds().width(),getBounds().height()),u=Math.min(density,size/24f),radius=Math.min(10*density,size/4);
        if(mode==PLAIN){paint.setShader(null);paint.setColor(color);paint.setAlpha(alpha);c.drawRect(getBounds(),paint);rect.set(getBounds());radius=0;}
        else{
            layer(c,0,radius,0xff050808);
            layer(c,u,radius,PlayerTheme.blend(color,mode==RECESSED?0xff000000:0xffffffff,mode==RECESSED?.65f:.28f));
            layer(c,2*u,radius,PlayerTheme.blend(color,mode==RECESSED?0xffffffff:0xff000000,mode==RECESSED?.20f:.50f));
            layer(c,3*u,radius,PlayerTheme.blend(color,0xff000000,.50f));
            layer(c,4*u,radius,PlayerTheme.blend(color,0xffffffff,mode==RAISED?.10f:0));
            layer(c,5*u,radius,color);
        }
        paint.setColor(Color.WHITE);paint.setShader(grid?pattern:texture);paint.setAlpha(alpha*(grid?90:PlayerTheme.luminance(color)>.35?0:38)/255);paint.setColorFilter(filter);
        if(mode==PLAIN)c.drawRect(rect,paint);else c.drawRoundRect(rect,Math.max(0,radius-5*u),Math.max(0,radius-5*u),paint);
        paint.setShader(null);
    }
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter value){filter=value;invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
