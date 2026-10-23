package dev.pixelspc;

import android.app.Application;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PlayerWidgetTest {
    private Application app;
    @Before public void setup() { app=RuntimeEnvironment.getApplication(); app.getSharedPreferences("appearance",0).edit().clear().commit(); PlaybackService.instance=null; }
    private View render(PlaybackService service) { return PlayerWidget.views(app,service).apply(app,new FrameLayout(app)); }
    @Test public void emptyWidgetOpensPlayerWithoutStartingAudio() {
        View widget=render(null);
        assertEquals("Tap to choose a track",((TextView)widget.findViewById(R.id.widget_title)).getText().toString());
        assertEquals(View.GONE,widget.findViewById(R.id.widget_time).getVisibility());
        widget.findViewById(R.id.widget_play).performClick();
        Intent open=Shadows.shadowOf(app).getNextStartedActivity(); assertEquals(MainActivity.class.getName(),open.getComponent().getClassName());
        assertNull(Shadows.shadowOf(app).getNextStartedService());
    }
    @Test public void widgetShowsPlaybackAndButtonsSendForegroundServiceActions() throws Exception {
        org.robolectric.android.controller.ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create(); PlaybackService service=c.get();
        try {
            Library.Track track=new Library.Track(); track.title="DK Island Swing"; track.game="Donkey Kong Country"; track.artist="David Wise"; track.length=238000;
            service.current=track; service.position=73000; service.playing=true;
            app.getSharedPreferences("appearance",0).edit().putString("theme","desktop-98").commit();
            View widget=render(service);
            assertEquals(track.title,((TextView)widget.findViewById(R.id.widget_title)).getText().toString());
            assertEquals(" / 3:58",((TextView)widget.findViewById(R.id.widget_duration)).getText().toString());
            assertEquals("Pause",widget.findViewById(R.id.widget_play).getContentDescription());
            assertEquals(PlayerTheme.find("desktop-98").nowPlaying().accent,((TextView)widget.findViewById(R.id.widget_title)).getCurrentTextColor());
            int[] buttons={R.id.widget_previous,R.id.widget_play,R.id.widget_next}; String[] actions={"widget:previous","widget:toggle","widget:next"};
            for(int i=0;i<buttons.length;i++) { widget.findViewById(buttons[i]).performClick(); Intent command=Shadows.shadowOf(app).getNextStartedService(); assertEquals(actions[i],command.getAction()); assertEquals(PlaybackService.class.getName(),command.getComponent().getClassName()); }
            screenshot(widget,"homescreen-widget.png");
            service.playing=false; service.repeat=true; widget=render(service);
            assertEquals("Play",widget.findViewById(R.id.widget_play).getContentDescription()); assertEquals(" / ∞",((TextView)widget.findViewById(R.id.widget_duration)).getText().toString());
            assertEquals(android.os.SystemClock.elapsedRealtime()-73000,((Chronometer)widget.findViewById(R.id.widget_time)).getBase());
        } finally { c.destroy(); }
    }
    @Test public void launcherReceivesAnEmptyUpdateAndStaleControlsStopCleanly() throws Exception {
        AppWidgetManager manager=AppWidgetManager.getInstance(app);
        int widget=Shadows.shadowOf(manager).createWidget(PlayerWidget.class,R.layout.player_widget);
        assertNotNull(Shadows.shadowOf(manager).getViewFor(widget));
        org.robolectric.android.controller.ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create(); PlaybackService service=c.get();
        try {
            service.onStartCommand(new Intent(app,PlaybackService.class).setAction("widget:toggle"),0,1);
            java.lang.reflect.Field worker=PlaybackService.class.getDeclaredField("worker"); worker.setAccessible(true);
            Shadows.shadowOf(((android.os.Handler)worker.get(service)).getLooper()).idle();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertNull(service.current); assertFalse(service.playing);
            assertEquals(0,service.getSystemService(android.app.NotificationManager.class).getActiveNotifications().length);
            View view=Shadows.shadowOf(manager).getViewFor(widget);
            assertEquals("Tap to choose a track",((TextView)view.findViewById(R.id.widget_title)).getText().toString());
        } finally { c.destroy(); }
    }
    private void screenshot(View view,String name) throws Exception {
        view.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY)); view.layout(0,0,720,440);
        Bitmap bitmap=Bitmap.createBitmap(720,440,Bitmap.Config.ARGB_8888); view.draw(new Canvas(bitmap));
        try(OutputStream out=new FileOutputStream(new File(System.getProperty("user.dir"),"../docs/screenshots/"+name))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out)); }
    }
}
