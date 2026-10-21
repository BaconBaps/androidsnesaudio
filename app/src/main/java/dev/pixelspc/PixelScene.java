package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original low-resolution artwork drawn with hard edges, never scaled with filtering. */
public class PixelScene extends View {
    private final Paint paint=new Paint();
    private static final int[][] CLOUDS={{23,22,39},{174,37,28},{270,65,33},{53,59,23}};
    private static final int[] TREES={100,204,219}, FLOWERS={91,115,185,230};
    public PixelScene(Context c) { super(c); setContentDescription("Pixel art: a floating green island and a castle beneath a lavender sunset"); }
    private void box(Canvas c,int color,float x,float y,float w,float h) { paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint); }
    @Override protected void onDraw(Canvas screen) {
        super.onDraw(screen); screen.save(); screen.scale(getWidth()/320f,getHeight()/154f);
        Canvas c=screen;
        box(c,0xffc8b7ed,0,0,320,154); box(c,0xffdbc8ee,0,56,320,98); box(c,0xffeed9df,0,100,320,54);
        box(c,0xffffeab7,235,20,28,28); box(c,0xffffeab7,229,26,40,16);
        for(int[] p:CLOUDS) { box(c,0xffeee4f7,p[0],p[1],p[2],7); box(c,0xffeee4f7,p[0]+8,p[1]-5,p[2]-16,5); }
        for(int x=0;x<320;x+=8) { int h=(int)(Math.sin(x*.035)*12+Math.sin(x*.083)*9+27); box(c,0xffb4a0d4,x,154-h-18,8,h+18); box(c,0xffa28bc5,x,154-h+1,8,h); }
        box(c,0xff6e539a,81,113,153,9); box(c,0xff6e539a,94,122,125,9); box(c,0xff6e539a,107,131,99,7); box(c,0xff6e539a,120,138,72,7); box(c,0xff6e539a,140,145,34,6);
        box(c,0xff9470ad,94,117,27,9); box(c,0xff9470ad,130,127,25,7); box(c,0xff9470ad,176,120,29,8);
        box(c,0xff3d6458,76,107,164,9); box(c,0xff6ba66b,76,101,164,9); box(c,0xff98c776,88,97,140,6);
        box(c,0xfff0dba0,137,98,34,6); box(c,0xfff0dba0,152,92,12,11);
        // Castle: crenellated towers, violet roofs, little golden windows.
        box(c,0xfff3e5cd,130,61,48,33); box(c,0xffe3ceb9,132,83,44,11);
        box(c,0xfff3e5cd,122,52,15,44); box(c,0xfff3e5cd,172,52,15,44);
        box(c,0xff76529b,119,49,21,5); box(c,0xff76529b,123,44,13,5); box(c,0xff76529b,127,39,5,5);
        box(c,0xff76529b,169,49,21,5); box(c,0xff76529b,173,44,13,5); box(c,0xff76529b,177,39,5,5);
        box(c,0xfff3e5cd,147,47,16,22); box(c,0xff76529b,144,43,22,5); box(c,0xff76529b,149,38,12,5); box(c,0xff76529b,153,33,4,5);
        box(c,0xff694d83,150,80,10,17); box(c,0xff694d83,153,76,4,5);
        box(c,0xff9776ab,127,63,5,8); box(c,0xff9776ab,177,63,5,8); box(c,0xffe2af69,153,55,4,7);
        box(c,0xff694d83,155,22,2,12); box(c,0xffe093a2,157,22,13,6);
        // Trees, flowers and tiny save-point sparkles.
        for(int x:TREES) { box(c,0xff6e539a,x,88,4,12); box(c,0xff3d6458,x-6,78,16,13); box(c,0xff6ba66b,x-2,73,8,9); }
        for(int x:FLOWERS) { box(c,0xffffe7aa,x,100,3,3); }
        box(c,0xfffaf3d7,64,80,2,10); box(c,0xfffaf3d7,60,84,10,2);
        box(c,0xfffaf3d7,251,92,2,10); box(c,0xfffaf3d7,247,96,10,2);
        screen.restore();
    }
}
