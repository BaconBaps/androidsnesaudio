package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Transparent sound-chip wordmark, fitted to its ink rather than its canvas. */
final class ChipLogo extends View {
    private static Bitmap bitmap;
    private static Rect ink;
    private final Paint paint=new Paint();
    private final RectF viewport=new RectF(),destination=new RectF();
    ChipLogo(Context context) {
        super(context); load(context);
        paint.setFilterBitmap(false); paint.setAntiAlias(false);
        setContentDescription(context.getString(R.string.app_name));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    private static synchronized void load(Context context) {
        if(bitmap!=null) return;
        bitmap=BitmapFactory.decodeResource(context.getResources(),R.drawable.chipamp9x_logo);
        int left=bitmap.getWidth(),top=bitmap.getHeight(),right=-1,bottom=-1;
        int[] row=new int[bitmap.getWidth()];
        for(int y=0;y<bitmap.getHeight();y++) {
            bitmap.getPixels(row,0,row.length,0,y,row.length,1);
            for(int x=0;x<row.length;x++) if(Color.alpha(row[x])>32) {
                left=Math.min(left,x); top=Math.min(top,y); right=Math.max(right,x); bottom=Math.max(bottom,y);
            }
        }
        ink=right<left?new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()):new Rect(left,top,right+1,bottom+1);
    }
    void drawInto(Canvas canvas,RectF box) {
        float scale=Math.min(box.width()/ink.width(),box.height()/ink.height());
        float width=ink.width()*scale,height=ink.height()*scale;
        float top=box.top+(box.height()-height)/2;
        destination.set(box.left,top,box.left+width,top+height);
        canvas.drawBitmap(bitmap,ink,destination,paint);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        viewport.set(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getHeight()-getPaddingBottom());
        drawInto(canvas,viewport);
    }
}
