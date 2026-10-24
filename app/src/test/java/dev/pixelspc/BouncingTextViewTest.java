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
}
