package dev.pixelspc;
import android.content.Context;
import android.graphics.*;
import android.widget.TextView;

/** Live playback buttons with dot-matrix glyphs; text and accessibility states stay intact. */
final class MatrixControl extends TextView {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int lamp;
    MatrixControl(Context c){this(c,ThemeStore.selected(c).nowPlaying().secondary);}
    MatrixControl(Context c,int lamp){super(c);this.lamp=lamp;}
    private static final boolean[][] SHUFFLE=pattern(new int[][]{
        {3,5,6,5},{6,5,14,15},{14,15,18,15},{3,15,6,15},{6,15,14,5},{14,5,18,5},
        {15,2,18,5},{18,5,15,8},{15,12,18,15},{18,15,15,18}});
    private static final boolean[][] LOOP=pattern(new int[][]{
        {4,10,4,7},{4,7,6,5},{6,5,17,5},{14,2,17,5},{17,5,14,8},
        {16,10,16,13},{16,13,14,15},{14,15,3,15},{6,12,3,15},{3,15,6,18}});
    private static boolean[][] pattern(int[][] lines){
        boolean[][] dots=new boolean[21][21];
        for(int[] line:lines){int x=line[0],y=line[1],dx=Math.abs(line[2]-x),dy=-Math.abs(line[3]-y),sx=x<line[2]?1:-1,sy=y<line[3]?1:-1,error=dx+dy;
            while(true){dots[y][x]=true;if(x==line[2]&&y==line[3])break;int e=2*error;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}}
        }return dots;
    }
    private boolean lit(String s,int x,int y){
        if(s.startsWith("Ⅱ"))return y>=5&&y<=15&&((x>=6&&x<=8)||(x>=12&&x<=14));
        if(s.startsWith("|◀"))return (x==4&&y>=5&&y<=15)||(y>=5&&y<=15&&x<=15&&x>=15-(5-Math.abs(y-10))*1.5);
        if(s.startsWith("▶|"))return (x==16&&y>=5&&y<=15)||(y>=5&&y<=15&&x>=5&&x<=5+(5-Math.abs(y-10))*1.5);
        if(s.startsWith("⇄"))return SHUFFLE[y][x];
        if(s.startsWith("↻"))return LOOP[y][x];
        return y>=5&&y<=15&&x>=6&&x<=6+(5-Math.abs(y-10))*2;
    }
    @Override protected void onDraw(Canvas c){
        String text=getText().toString();boolean mode=text.startsWith("⇄")||text.startsWith("↻");float density=getResources().getDisplayMetrics().density;
        float size=Math.min(getWidth()-12*density,(getHeight()-10*density));float step=Math.max(0,size*(mode?.65f:1)/21),cx=getWidth()/2f,cy=getHeight()/2f;
        int color=(text.equals("▶")||text.equals("Ⅱ"))?lamp:getCurrentTextColor();
        for(int y=0;y<=20;y++)for(int x=0;x<=20;x++)if(lit(text,x,y)){
            paint.setColor(color);paint.setAlpha(35);c.drawCircle(cx+(x-10)*step,cy+(y-10)*step,step*.75f,paint);paint.setAlpha(255);c.drawCircle(cx+(x-10)*step,cy+(y-10)*step,step*.36f,paint);
        }
        if(mode){
            float x=getWidth()-10*density,y=10*density,r=3*density;
            if(isSelected()){paint.setColor(lamp);paint.setAlpha(40);c.drawCircle(x,y,r*1.9f,paint);}
            paint.setAlpha(255);paint.setColor(0xff050809);c.drawCircle(x,y,r+1.2f*density,paint);
            paint.setColor(PlayerTheme.blend(lamp,0xff000000,isSelected()?.08f:.84f));c.drawCircle(x,y,r,paint);
            paint.setColor(PlayerTheme.blend(lamp,0xffffffff,.65f));paint.setAlpha(isSelected()?220:40);c.drawCircle(x-.7f*density,y-.8f*density,.85f*density,paint);paint.setAlpha(255);
        }
    }
}
