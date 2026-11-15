package dev.pixelspc;
import android.content.Context;
import android.graphics.*;
import android.widget.TextView;

/** Live playback buttons with dot-matrix glyphs; text and accessibility states stay intact. */
final class MatrixControl extends TextView {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int lamp;
    MatrixControl(Context c){this(c,ThemeStore.selected(c).nowPlaying().secondary);}
    MatrixControl(Context c,int lamp){super(c);this.lamp=lamp;}
    private boolean lit(String s,int x,int y){
        if(s.startsWith("Ⅱ"))return y>=5&&y<=15&&((x>=6&&x<=8)||(x>=12&&x<=14));
        if(s.startsWith("|◀"))return (x==4&&y>=5&&y<=15)||(y>=5&&y<=15&&x<=15&&x>=15-(5-Math.abs(y-10))*1.5);
        if(s.startsWith("▶|"))return (x==16&&y>=5&&y<=15)||(y>=5&&y<=15&&x>=5&&x<=5+(5-Math.abs(y-10))*1.5);
        if(s.startsWith("⇄"))return ((y==6||y==14)&&x>=4&&x<=16)||((x==y||x+y==20)&&x>=6&&x<=14)||(x>=14&&x<=17&&(Math.abs(y-6)==17-x||Math.abs(y-14)==17-x));
        if(s.startsWith("↻")){double r=Math.hypot(x-10,y-10);return (r>5.3&&r<6.8&&!(x>12&&y<7))||(x>=13&&x<=17&&y==6)||(x==17&&y>=3&&y<=6);}
        return y>=5&&y<=15&&x>=6&&x<=6+(5-Math.abs(y-10))*2;
    }
    @Override protected void onDraw(Canvas c){
        String text=getText().toString();boolean mode=text.contains("\n");float density=getResources().getDisplayMetrics().density;
        float size=Math.min(getWidth()-12*density,(getHeight()-(mode?18:10)*density));float step=Math.max(0,size/21),cx=getWidth()/2f,cy=(getHeight()-(mode?15*density:0))/2;
        int color=(text.equals("▶")||text.equals("Ⅱ"))?lamp:getCurrentTextColor();
        for(int y=0;y<=20;y++)for(int x=0;x<=20;x++)if(lit(text,x,y)){
            paint.setColor(color);paint.setAlpha(35);c.drawCircle(cx+(x-10)*step,cy+(y-10)*step,step*.75f,paint);paint.setAlpha(255);c.drawCircle(cx+(x-10)*step,cy+(y-10)*step,step*.36f,paint);
        }
        if(mode){paint.setTypeface(UiFonts.body(getContext()));paint.setTextSize(9*getResources().getDisplayMetrics().scaledDensity);paint.setColor(getCurrentTextColor());paint.setAlpha(255);String state=text.substring(text.indexOf('\n')+1);c.drawText(state,cx-paint.measureText(state)/2,getHeight()-5*density,paint);}
    }
}
