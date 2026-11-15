package dev.pixelspc;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original dot-grid chip and gear symbols, drawn at the same pitch. */
final class DotGlyph extends Drawable {
    private final boolean gear;private final int color;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private int alpha=255;
    DotGlyph(boolean gear,int color){this.gear=gear;this.color=color;}
    private boolean lit(int x,int y){
        if(gear){double dx=x-8,dy=y-8,r=Math.sqrt(dx*dx+dy*dy),a=Math.atan2(dy,dx);double rim=6.1+(.5+.5*Math.cos(a*8))*1.1;return (r>=rim-1.4&&r<=rim)||(r>=2.1&&r<=3.2);}
        if((x==3||x==13)&&y>=3&&y<=13)return true;
        if((y==3||y==13)&&x>=3&&x<=13)return true;
        if((x==0||x==1||x==15||x==16)&&y>=5&&y<=11&&y%3==2)return true;
        if((y==0||y==1||y==15||y==16)&&x>=5&&x<=11&&x%3==2)return true;
        return (x==7&&y>=7&&y<=11)||(x==11&&y>=5&&y<=9)||(y==6&&x>=7&&x<=11)||(x>=5&&x<=7&&y==11)||(x>=9&&x<=11&&y==9);
    }
    @Override public void draw(Canvas c){Rect b=getBounds();float size=Math.min(b.width(),b.height()),step=size/18f,ox=b.exactCenterX()-8*step,oy=b.exactCenterY()-8*step;
        for(int y=0;y<17;y++)for(int x=0;x<17;x++)if(lit(x,y)){paint.setColor(color);paint.setAlpha(alpha/9);c.drawCircle(ox+x*step,oy+y*step,step*.8f,paint);paint.setAlpha(alpha);c.drawCircle(ox+x*step,oy+y*step,step*.36f,paint);}
    }
    @Override public void setAlpha(int a){alpha=a;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
