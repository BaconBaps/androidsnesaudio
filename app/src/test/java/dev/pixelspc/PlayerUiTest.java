package dev.pixelspc;

import android.app.Application;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PlayerUiTest {
    private Application app;
    private PlaybackService service;
    @Before public void clear() {
        app=RuntimeEnvironment.getApplication(); new File(app.getFilesDir(),"library.json").delete();
        service=new PlaybackService();
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(new android.content.ComponentName(app,PlaybackService.class),service.onBind(new android.content.Intent()));
    }
    @Test public void emptyLibraryHasImportAndDisabledSeek() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();
            assertTrue(text(a.getWindow().getDecorView()).contains("INSERT SOUNDTRACK"));
            SeekBar seek=findSeek(a.getWindow().getDecorView()); assertNotNull(seek); assertFalse(seek.isEnabled());
            View importButton=findText(a.getWindow().getDecorView(),"+  IMPORT MUSIC"); assertNotNull(importButton); importButton.performClick();
            android.content.Intent picker=Shadows.shadowOf(a).getNextStartedActivityForResult().intent;
            assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT,picker.getAction()); assertEquals("*/*",picker.getType());
            capture(a,"empty-library.png");
        }
    }
    @Test public void savedLibraryRestoresTrackNamesAndDurations() throws Exception {
        String[] titles={"Theme","Simian Segue","DK Island Swing","Cranky's Theme","Cave Dweller Concert","Bonus Room Blitz","Aquatic Ambiance"};
        org.json.JSONArray tracks=new org.json.JSONArray();
        for(int i=0;i<titles.length;i++) tracks.put(new org.json.JSONObject().put("path","/fixture/"+i+".spc").put("title",titles[i]).put("game","Donkey Kong Country").put("artist","David Wise").put("system","Super Nintendo").put("index",0).put("length",i==2?238000:180000));
        String json=new org.json.JSONArray().put(new org.json.JSONObject().put("name","Donkey Kong Country").put("id","fixture").put("tracks",tracks)).toString();
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),json.getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); String visible=text(a.getWindow().getDecorView());
            assertTrue(visible.contains("Donkey Kong Country")); assertTrue(visible.contains("DK Island Swing")); assertTrue(visible.contains("3:58"));
            capture(a,"soundtrack-library.png");
            // A paused UI fixture exercises the real player layout without mocking sound output.
            Library saved=new Library(app); service.queue=saved.games.get(0).tracks;
            service.current=service.queue.get(2); service.index=2; service.position=73000;
            service.listener.run();
            assertTrue(findSeek(a.getWindow().getDecorView()).isEnabled()); assertEquals(73000,findSeek(a.getWindow().getDecorView()).getProgress());
            capture(a,"player-paused.png");
        }
    }
    @Test public void formatsAndClockAreAccurate() {
        assertEquals("3:58",MainActivity.clock(238000)); assertEquals("0:00",MainActivity.clock(-100));
        assertTrue(ArchiveImporter.supported("TRACK.SPC")); assertTrue(ArchiveImporter.supported("game.nsf")); assertFalse(ArchiveImporter.supported("game.sfc"));
    }
    private String text(View v) { String s=v instanceof TextView?((TextView)v).getText().toString()+"\n":""; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) s+=text(((ViewGroup)v).getChildAt(i)); return s; }
    private SeekBar findSeek(View v) { if(v instanceof SeekBar) return (SeekBar)v; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) { SeekBar s=findSeek(((ViewGroup)v).getChildAt(i)); if(s!=null) return s; } return null; }
    private View findText(View v,String label) { if(v instanceof TextView && label.contentEquals(((TextView)v).getText())) return v; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) { View found=findText(((ViewGroup)v).getChildAt(i),label); if(found!=null) return found; } return null; }
    private void capture(MainActivity a,String name) throws Exception {
        View view=a.getWindow().getDecorView(); int width=822,height=1782;
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY)); view.layout(0,0,width,height);
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); view.draw(new Canvas(bitmap));
        File dir=new File(System.getProperty("user.dir"),"../docs/screenshots"); dir.mkdirs();
        try(OutputStream out=new FileOutputStream(new File(dir,name))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out)); }
    }
}
