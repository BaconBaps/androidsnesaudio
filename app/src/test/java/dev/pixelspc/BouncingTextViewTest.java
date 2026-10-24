package dev.pixelspc;

import android.os.Looper;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BouncingTextViewTest {
    @Test public void movementPausesAtEachEdgeAndReturnsWithoutJumping() {
        assertEquals(0,BouncingTextView.offsetAt(1000,100,50),.001);
        assertEquals(50,BouncingTextView.offsetAt(2200,100,50),.001);
        assertEquals(100,BouncingTextView.offsetAt(3500,100,50),.001);
        assertEquals(50,BouncingTextView.offsetAt(5400,100,50),.001);
        assertEquals(0,BouncingTextView.offsetAt(6400,100,50),.001);
        assertEquals(0,BouncingTextView.offsetAt(5000,0,50),0);
        float startingStep=BouncingTextView.offsetAt(1300,100,50)-BouncingTextView.offsetAt(1200,100,50);
        float middleStep=BouncingTextView.offsetAt(2200,100,50)-BouncingTextView.offsetAt(2100,100,50);
        float endingStep=BouncingTextView.offsetAt(3200,100,50)-BouncingTextView.offsetAt(3100,100,50);
        assertTrue(startingStep<middleStep); assertEquals(startingStep,endingStep,.001);
    }
    @Test public void titleUpdatesDoNotRestartMotionAndShortTextStaysStill() {
        BouncingTextView title=new BouncingTextView(RuntimeEnvironment.getApplication());
        title.setLayoutParams(new android.widget.FrameLayout.LayoutParams(120,40));
        title.setTextSize(18); title.setText("A long retro soundtrack title that cannot fit inside this narrow field");
        title.measure(View.MeasureSpec.makeMeasureSpec(120,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(40,View.MeasureSpec.EXACTLY)); title.layout(0,0,120,40);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2200)); float offset=title.currentOffset(); assertTrue(offset>0);
        title.setText(title.getText().toString()); assertEquals(offset,title.currentOffset(),.01);
        title.setText("Hi"); title.measure(View.MeasureSpec.makeMeasureSpec(120,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(40,View.MeasureSpec.EXACTLY)); title.layout(0,0,120,40);
        assertEquals(0,title.currentOffset(),0);
    }
    @Test public void scrollingRevealsGlyphsOriginallyOutsideTheViewport() {
        BouncingTextView title=new BouncingTextView(RuntimeEnvironment.getApplication());
        title.setTextColor(android.graphics.Color.WHITE);title.setTextSize(20);
        title.setText("                              HIDDEN END");
        title.measure(View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(48,View.MeasureSpec.EXACTLY));title.layout(0,0,100,48);
        android.graphics.Bitmap before=android.graphics.Bitmap.createBitmap(100,48,android.graphics.Bitmap.Config.ARGB_8888);
        title.draw(new android.graphics.Canvas(before));assertEquals(0,ink(before));
        float overflow=title.getLayout().getLineWidth(0)-100;
        long end=1300+(long)Math.ceil(overflow/(32*title.getResources().getDisplayMetrics().density)*1000);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(end));
        assertEquals(overflow,title.currentOffset(),.1);
        android.graphics.Bitmap after=android.graphics.Bitmap.createBitmap(100,48,android.graphics.Bitmap.Config.ARGB_8888);
        title.draw(new android.graphics.Canvas(after));assertTrue("Previously hidden suffix must actually be drawn",ink(after)>100);
    }
    private static int ink(android.graphics.Bitmap bitmap){int count=0;for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)if((bitmap.getPixel(x,y)>>>24)>0)count++;return count;}
}
