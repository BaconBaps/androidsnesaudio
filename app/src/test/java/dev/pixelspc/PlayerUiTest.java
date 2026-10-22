package dev.pixelspc;

import android.app.Application;
import android.app.AlertDialog;
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
    private org.robolectric.android.controller.ServiceController<PlaybackService> serviceController;
    @Before public void clear() {
        app=RuntimeEnvironment.getApplication(); new File(app.getFilesDir(),"library.json").delete();
        app.getSharedPreferences("appearance",0).edit().clear().commit();
        app.getSharedPreferences("playback",0).edit().clear().commit();
        serviceController=Robolectric.buildService(PlaybackService.class).create(); service=serviceController.get();
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(new android.content.ComponentName(app,PlaybackService.class),service.onBind(new android.content.Intent()));
    }
    @After public void releaseService() { serviceController.destroy(); }
    @Test public void emptyLibraryHasImportAndDisabledSeek() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();
            assertTrue(text(a.getWindow().getDecorView()).contains("INSERT SOUNDTRACK"));
            assertFalse(text(a.getWindow().getDecorView()).contains("SOUNDTRACKS NEVER GET OLD"));
            assertFalse(text(a.getWindow().getDecorView()).toLowerCase().contains("adventure"));
            SeekBar seek=findSeek(a.getWindow().getDecorView()); assertNotNull(seek); assertFalse(seek.isEnabled());
            View importButton=findText(a.getWindow().getDecorView(),"+  IMPORT MUSIC"); assertNotNull(importButton); importButton.performClick();
            android.content.Intent picker=Shadows.shadowOf(a).getNextStartedActivityForResult().intent;
            assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT,picker.getAction()); assertEquals("*/*",picker.getType());
            assertFalse(text(a.getWindow().getDecorView()).contains("A little player"));
            assertFalse(text(a.getWindow().getDecorView()).contains("YOUR SOUNDTRACKS"));
            assertFalse(text(a.getWindow().getDecorView()).contains("16-BIT"));
            assertNull(findDescription(a.getWindow().getDecorView(),"Toggle artwork and visualizer"));
            capture(a,"empty-library.png");
            assertTrue(((android.view.ViewGroup.MarginLayoutParams)seek.getLayoutParams()).leftMargin>=a.dp(18));
            assertTrue(((android.view.ViewGroup.MarginLayoutParams)seek.getLayoutParams()).rightMargin>=a.dp(18));
            View library=findDescription(a.getWindow().getDecorView(),"Open music library");
            assertEquals(((View)library.getParent()).getWidth()-((View)library.getParent()).getPaddingLeft()-((View)library.getParent()).getPaddingRight(),library.getWidth());
        }
    }
    @Test public void savedLibraryRestoresTrackNamesAndDurations() throws Exception {
        String[] titles={"Theme","Simian Segue","DK Island Swing","Cranky's Theme","Cave Dweller Concert","Bonus Room Blitz","Aquatic Ambiance"};
        org.json.JSONArray tracks=new org.json.JSONArray();
        for(int i=0;i<titles.length;i++) tracks.put(new org.json.JSONObject().put("path","/fixture/"+i+".spc").put("title",titles[i]).put("game","Donkey Kong Country").put("artist","David Wise").put("system","Super Nintendo").put("index",0).put("length",i==2?238000:180000).put("loopLength",i==2?120000:0));
        String json=new org.json.JSONArray().put(new org.json.JSONObject().put("name","Donkey Kong Country").put("id","fixture").put("tracks",tracks)).toString();
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),json.getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); String visible=text(a.getWindow().getDecorView());
            assertTrue(visible.contains("Donkey Kong Country")); assertTrue(visible.contains("DK Island Swing")); assertTrue(visible.contains("3:58"));
            capture(a,"soundtrack-library.png");
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();
            AlertDialog browser=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            captureView(browser.getWindow().getDecorView(),"library-browser.png",720,450); browser.dismiss();
            // A paused UI fixture exercises the real player layout without mocking sound output.
            Library saved=new Library(app); service.queue=saved.games.get(0).tracks;
            service.current=service.queue.get(2); service.index=2; service.position=73000;
            service.listener.run();
            assertTrue(findSeek(a.getWindow().getDecorView()).isEnabled()); assertEquals(73000,findSeek(a.getWindow().getDecorView()).getProgress());
            capture(a,"player-paused.png");
            assertTrue(findText(a.getWindow().getDecorView(),"DK Island Swing") instanceof BouncingTextView);
            View trackMode=findDescription(a.getWindow().getDecorView(),"Loop mode for DK Island Swing: straight");
            assertNotNull(trackMode); assertNull(findDescription(a.getWindow().getDecorView(),"Loop mode for Theme: straight"));
            trackMode.performClick(); assertTrue(service.repeat); assertTrue(service.isLooping(service.current));
            assertTrue(text(a.getWindow().getDecorView()).contains("∞ LOOP"));
            service.levels=new AudioSpectrum().analyze(AudioSpectrumTest.chord()); service.playing=true;
            service.setShuffle(true); service.listener.run(); capture(a,"player-looping.png");
            service.setShuffle(false);
            findDescription(a.getWindow().getDecorView(),"Loop mode for DK Island Swing: looping").performClick();
            assertFalse(service.repeat); assertFalse(service.isLooping(service.current));
            service.levels=new AudioSpectrum().analyze(AudioSpectrumTest.chord());
            service.playing=true;
            for(PlayerTheme palette:PlayerTheme.ALL) {
                a.selectTheme(palette.id);
                assertEquals(73000,findSeek(a.getWindow().getDecorView()).getProgress());
                assertTrue(text(a.getWindow().getDecorView()).contains("DK Island Swing"));
                assertFalse(text(a.getWindow().getDecorView()).contains("SOUNDTRACKS NEVER GET OLD"));
                capture(a,"theme-"+palette.id+".png");
            }
        }
    }
    @Test public void shuffleAndLoopShowFeedbackEvenBeforeSelectingATrack() {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            View shuffle=findDescription(c.get().getWindow().getDecorView(),"Shuffle off");
            View loop=findDescription(c.get().getWindow().getDecorView(),"Continuous loop off");
            assertNotNull(shuffle); assertNotNull(loop); assertTrue(shuffle.isEnabled()); assertTrue(loop.isEnabled());
            shuffle.performClick(); loop.performClick(); assertTrue(service.shuffle); assertTrue(service.repeat);
            assertTrue(shuffle.isSelected()); assertTrue(loop.isSelected()); assertNull(shuffle.getBackground()); assertNull(loop.getBackground());
            assertTrue(((TextView)shuffle).getText().toString().contains("ON")); assertTrue(((TextView)loop).getText().toString().contains("ON"));
            c.recreate();
            assertTrue(findDescription(c.get().getWindow().getDecorView(),"Shuffle on").isSelected());
            assertTrue(findDescription(c.get().getWindow().getDecorView(),"Continuous loop on").isSelected());
            findDescription(c.get().getWindow().getDecorView(),"Shuffle on").performClick();
            findDescription(c.get().getWindow().getDecorView(),"Continuous loop on").performClick(); assertFalse(service.shuffle); assertFalse(service.repeat);
        }
    }
    @Test public void themePickerPersistsDarkModeAcrossRecreation() {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            View picker=findDescription(c.get().getWindow().getDecorView(),"Choose color theme"); assertNotNull(picker); picker.performClick();
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            GridLayout grid=findGrid(dialog.getWindow().getDecorView()); assertNotNull(grid);
            assertEquals(2,grid.getColumnCount()); assertEquals(PlayerTheme.ALL.length,grid.getChildCount());
            assertTrue(grid.getChildAt(0).isSelected());
            try { captureView(dialog.getWindow().getDecorView(),"theme-picker.png",720,1500); } catch(Exception e) { throw new AssertionError(e); }
            grid.getChildAt(1).performClick();
            assertEquals("dark",app.getSharedPreferences("appearance",0).getString("theme",null));
            assertEquals(PlayerTheme.find("dark").background,((android.graphics.drawable.ColorDrawable)((ViewGroup)c.get().findViewById(android.R.id.content)).getChildAt(0).getBackground()).getColor());
            c.recreate();
            assertEquals(PlayerTheme.find("dark").background,((android.graphics.drawable.ColorDrawable)((ViewGroup)c.get().findViewById(android.R.id.content)).getChildAt(0).getBackground()).getColor());
            assertFalse(text(c.get().getWindow().getDecorView()).contains("SOUNDTRACKS NEVER GET OLD"));
        }
    }
    @Test public void formatsAndClockAreAccurate() {
        assertEquals("3:58",MainActivity.clock(238000)); assertEquals("0:00",MainActivity.clock(-100));
        assertTrue(ArchiveImporter.supported("TRACK.SPC")); assertTrue(ArchiveImporter.supported("game.nsf")); assertFalse(ArchiveImporter.supported("game.sfc"));
    }
    private String text(View v) { String s=v instanceof TextView?((TextView)v).getText().toString()+"\n":""; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) s+=text(((ViewGroup)v).getChildAt(i)); return s; }
    private SeekBar findSeek(View v) { if(v instanceof SeekBar) return (SeekBar)v; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) { SeekBar s=findSeek(((ViewGroup)v).getChildAt(i)); if(s!=null) return s; } return null; }
    private View findText(View v,String label) { if(v instanceof TextView && label.contentEquals(((TextView)v).getText())) return v; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) { View found=findText(((ViewGroup)v).getChildAt(i),label); if(found!=null) return found; } return null; }
    private View findDescription(View v,String label) { if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription())) return v; if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) { View found=findDescription(((ViewGroup)v).getChildAt(i),label); if(found!=null) return found; } return null; }
    private GridLayout findGrid(View view) { if(view instanceof GridLayout) return (GridLayout)view; if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) { GridLayout grid=findGrid(((ViewGroup)view).getChildAt(i)); if(grid!=null) return grid; } return null; }
    private void capture(MainActivity a,String name) throws Exception { captureView(a.getWindow().getDecorView(),name,822,1782); }
    private void captureView(View view,String name,int width,int height) throws Exception {
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY)); view.layout(0,0,width,height);
        View rowTitle=findText(view,"DK Island Swing");
        if(rowTitle!=null) assertTrue("Track name collapsed: "+rowTitle.getWidth()+"x"+rowTitle.getHeight(),rowTitle.getWidth()>100 && rowTitle.getHeight()>0);
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); view.draw(new Canvas(bitmap));
        File dir=new File(System.getProperty("user.dir"),"../docs/screenshots"); dir.mkdirs();
        try(OutputStream out=new FileOutputStream(new File(dir,name))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out)); }
    }
}
