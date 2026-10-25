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
        app=RuntimeEnvironment.getApplication(); new File(app.getFilesDir(),"library.json").delete();new File(app.getFilesDir(),"playlists.json").delete();new File(app.getFilesDir(),"playlists.json.bak").delete();
        app.getSharedPreferences("appearance",0).edit().clear().commit();
        app.getSharedPreferences("library-browser",0).edit().clear().commit();
        app.getSharedPreferences("playback",0).edit().clear().commit();
        app.getSharedPreferences("favourites",0).edit().clear().commit();
        app.getSharedPreferences("soundfonts",0).edit().clear().commit();
        app.getSharedPreferences("custom-themes",0).edit().clear().commit();
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
            View importButton=findText(a.getWindow().getDecorView(),"+  IMPORT MUSIC"); assertNotNull(importButton);
            assertEquals(a.dp(220),importButton.getLayoutParams().width);
            assertEquals(android.view.Gravity.CENTER,((LinearLayout)importButton.getParent()).getGravity());
            assertNull(findDescription(a.getWindow().getDecorView(),"Import music")); importButton.performClick();
            android.content.Intent picker=Shadows.shadowOf(a).getNextStartedActivityForResult().intent;
            assertEquals(ImportActivity.class.getName(),picker.getComponent().getClassName());
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
            assertFalse(visible.contains("Local files"));
            assertTrue(visible.contains("Donkey Kong Country")); assertTrue(visible.contains("DK Island Swing")); assertTrue(visible.contains("3:58"));
            assertNull(findText(a.getWindow().getDecorView(),"+  IMPORT MUSIC"));
            assertNull(findDescription(a.getWindow().getDecorView(),"Import music"));
            capture(a,"soundtrack-library.png");
            View title=findText(a.getWindow().getDecorView(),"Donkey Kong Country");
            assertTrue(title instanceof BouncingTextView); assertEquals(1,((TextView)title).getMaxLines());
            TextView now=(TextView)findText(a.getWindow().getDecorView(),"No track selected");
            assertTrue(now instanceof BouncingTextView); assertEquals(1,now.getMaxLines()); assertTrue(now.getTypeface().isBold());
            View meta=findText(a.getWindow().getDecorView(),"Super Nintendo  ·  7 tracks");
            View heading=findText(a.getWindow().getDecorView(),"TRACKLIST");
            assertNotNull(meta);
            ScrollView playlist=findScroll(a.getWindow().getDecorView());
            View frame=findDescription(a.getWindow().getDecorView(),"Tracklist frame"); assertSame(frame,playlist.getParent());
            assertTrue(frame.getHeight()>0); assertNotNull(frame.getBackground());
            assertNull(((ViewGroup)playlist).getChildAt(0).getBackground());
            Bitmap borderBefore=bottomEdge(frame);
            int[] beforeTitle=new int[2],beforeMeta=new int[2],beforeHeading=new int[2];
            title.getLocationOnScreen(beforeTitle); meta.getLocationOnScreen(beforeMeta); heading.getLocationOnScreen(beforeHeading);
            playlist.scrollTo(0,10000); assertTrue(playlist.getScrollY()>0);
            int[] after=new int[2]; title.getLocationOnScreen(after); assertArrayEquals(beforeTitle,after);
            meta.getLocationOnScreen(after); assertArrayEquals(beforeMeta,after);
            heading.getLocationOnScreen(after); assertArrayEquals(beforeHeading,after);
            assertTrue("Bottom frame must remain identical after scrolling",borderBefore.sameAs(bottomEdge(frame)));
            capture(a,"tracklist-scrolled-bottom.png");
            playlist.scrollTo(0,playlist.getScrollY()/2);
            assertTrue("Bottom frame must remain identical mid-list",borderBefore.sameAs(bottomEdge(frame)));
            capture(a,"tracklist-scrolled-middle.png");
            playlist.scrollTo(0,0);
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();
            android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            captureView(browser.getWindow().getDecorView(),"library-browser.png",774,1500); browser.dismiss();
            // A paused UI fixture exercises the real player layout without mocking sound output.
            Library saved=new Library(app); service.queue=saved.games.get(0).tracks;
            service.current=service.queue.get(2); service.index=2; service.position=73000;
            service.listener.run();
            assertTrue(findSeek(a.getWindow().getDecorView()).isEnabled()); assertEquals(73000,findSeek(a.getWindow().getDecorView()).getProgress());
            capture(a,"player-paused.png");
            service.seekTarget=123470; service.listener.run();
            assertTrue(text(a.getWindow().getDecorView()).contains("SEEKING → 2:03"));
            service.seekTarget=-1; service.listener.run();
            assertFalse(text(a.getWindow().getDecorView()).contains("SEEKING"));
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
                if(palette.reactor()){
                    PixelVisualizer reactorViz=findVisualizer(a.getWindow().getDecorView());reactorViz.performClick();
                    reactorViz.setFrame(service.levels,AudioWaveform.analyze(AudioSpectrumTest.tone(220,.7,false)),true,true);
                    capture(a,"reactor-waveform.png");reactorViz.performLongClick();
                    findDescription(a.getWindow().getDecorView(),"Open music library").performClick();
                    android.app.Dialog reactorLibrary=org.robolectric.shadows.ShadowDialog.getLatestDialog();
                    captureView(reactorLibrary.getWindow().getDecorView(),"reactor-library.png",822,1450);reactorLibrary.dismiss();
                }
            }
            a.selectTheme("desktop-98");
            findDescription(a.getWindow().getDecorView(),"Add DK Island Swing to favourites").performClick();
            findDescription(a.getWindow().getDecorView(),"App menu").performClick();
            findDescription(org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Favourites").performClick();
            assertNotNull(findText(a.getWindow().getDecorView(),"Favourite tracks"));
            assertNull(findText(a.getWindow().getDecorView(),"Theme"));
            assertNotNull(findText(a.getWindow().getDecorView(),"DK Island Swing"));
            capture(a,"favourites.png");
            c.recreate(); assertNotNull(findDescription(c.get().getWindow().getDecorView(),"Remove DK Island Swing from favourites"));
            findDescription(c.get().getWindow().getDecorView(),"Remove DK Island Swing from favourites").performClick();
            assertNotNull(findText(c.get().getWindow().getDecorView(),"No favourites yet. Tap a track’s ☆ to add it here."));
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
    @Test public void addAndQueuePreservesPlaybackShowsQueueAndLabelsMidiBank() throws Exception {
        Library.Track live=new Library.Track();live.path="/fixture/live.mid";live.title="Overworld";live.game="First game";live.artist="Composer";live.system="MIDI";live.length=180000;
        service.queue=new java.util.ArrayList<>(java.util.Collections.singletonList(live));service.current=live;service.index=0;service.position=73000;service.playing=true;
        File bank=new File(app.getFilesDir(),"soundfonts/queue-test.sf2");bank.getParentFile().mkdirs();Files.write(bank.toPath(),new byte[]{1});app.getSharedPreferences("soundfonts",0).edit().putString("name:queue-test","F-Zero").commit();SoundFonts fonts=new SoundFonts(app);fonts.setTrack(live.path,"queue-test");
        org.json.JSONObject track=new org.json.JSONObject().put("path","/fixture/added.spc").put("title","New track").put("game","Second game").put("artist","Composer").put("system","Super Nintendo").put("index",0).put("length",180000).put("loopLength",0);
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Second game").put("id","new-import").put("tracks",new org.json.JSONArray().put(track))).toString().getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();a.onActivityResult(4,android.app.Activity.RESULT_OK,new android.content.Intent().putStringArrayListExtra(ImportActivity.IDS,new java.util.ArrayList<>(java.util.Collections.singletonList("new-import"))).putExtra(ImportActivity.QUEUE,true));
            assertSame(live,service.current);assertTrue(service.playing);assertEquals(73000,service.position);assertEquals(2,service.queue.size());assertNotNull(findText(a.getWindow().getDecorView(),"Playback queue"));assertNotNull(findText(a.getWindow().getDecorView(),"New track"));
            assertTrue(findText(a.getWindow().getDecorView(),"Overworld (with F-Zero)") instanceof BouncingTextView);assertEquals("Overworld",live.title);
            service.levels=new AudioSpectrum().analyze(AudioSpectrumTest.chord());a.selectTheme("desktop-98");capture(a,"playback-queue.png");
            fonts.rename(new SoundFonts.Bank("queue-test","F-Zero",bank,false),"Renamed bank");service.listener.run();assertNotNull(findText(a.getWindow().getDecorView(),"Overworld (with Renamed bank)"));
            findDescription(a.getWindow().getDecorView(),"Play New track").performLongClick();AlertDialog actions=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();actions.getListView().performItemClick(null,0,0);assertEquals(1,service.index);assertSame(live,service.current);
            findDescription(a.getWindow().getDecorView(),"Play New track").performLongClick();actions=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();actions.getListView().performItemClick(null,1,1);assertEquals(1,service.queue.size());assertSame(live,service.current);assertEquals(0,service.index);
        } finally {bank.delete();}
    }
    @Test public void addOnlyRefreshesLibraryWithoutChangingQueue() throws Exception {
        Library.Track live=new Library.Track();live.path="/fixture/live.spc";live.title="Live";live.game="Game";live.artist="";live.system="Super Nintendo";live.length=180000;service.queue=java.util.Collections.singletonList(live);service.current=live;service.index=0;
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            org.json.JSONObject track=new org.json.JSONObject().put("path","/fixture/new.spc").put("title","Added song").put("game","Added game").put("artist","").put("system","Super Nintendo").put("index",0).put("length",180000).put("loopLength",0);
            Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Added game").put("id","added").put("tracks",new org.json.JSONArray().put(track))).toString().getBytes("UTF-8"));
            c.get().onActivityResult(4,android.app.Activity.RESULT_OK,new android.content.Intent().putStringArrayListExtra(ImportActivity.IDS,new java.util.ArrayList<>(java.util.Collections.singletonList("added"))));
            assertEquals(1,service.queue.size());assertSame(live,service.current);assertNotNull(findText(c.get().getWindow().getDecorView(),"Added song"));assertNull(findText(c.get().getWindow().getDecorView(),"Playback queue"));
        }
    }
    @Test public void midiTrackOffersPersistentBankChoiceWithoutChangingDefault() throws Exception {
        org.json.JSONObject track=new org.json.JSONObject().put("path","/fixture/song.mid").put("title","Prelude").put("game","").put("artist","").put("system","MIDI").put("index",0).put("length",3000).put("loopLength",0);
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","MIDI demo").put("id","midi-fixture").put("tracks",new org.json.JSONArray().put(track))).toString().getBytes("UTF-8"));
        SoundFonts banks=new SoundFonts(app);banks.banks();File extra=new File(app.getFilesDir(),"soundfonts/custom.sf2");Files.write(extra.toPath(),new byte[]{1});
        app.getSharedPreferences("soundfonts",0).edit().putString("name:custom","Imported test bank").commit();
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            findDescription(c.get().getWindow().getDecorView(),"Play Prelude").performLongClick();
            AlertDialog actions=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();actions.getListView().performItemClick(null,0,0);
            AlertDialog picker=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertEquals(3,picker.getListView().getCount());
            picker.getListView().performItemClick(null,2,2);org.robolectric.shadows.ShadowLooper.idleMainLooper();
            assertEquals("custom",new SoundFonts(app).overrideId("/fixture/song.mid"));assertEquals(SoundFonts.BUILTIN,banks.defaultId());
        } finally {extra.delete();}
    }
    @Test public void tapCyclesVisualizersAndSelectionSurvivesThemeEditingAndRecreation() throws Exception {
        Library.Track demo=new Library.Track();demo.path="/fixture/pixel-steps.mid";demo.title="Pixel Steps";demo.game="Visualizer Sessions";demo.artist="ChipAmp9x";demo.system="MIDI";demo.index=0;demo.length=180000;
        service.current=demo;service.queue=java.util.Collections.singletonList(demo);service.index=0;service.position=43000;
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Visualizer Sessions").put("id","visualizer-demo").put("tracks",new org.json.JSONArray().put(demo.json()))).toString().getBytes("UTF-8"));
        service.levels=new AudioSpectrum().analyze(AudioSpectrumTest.chord());
        short[] pcm=AudioSpectrumTest.tone(440,.6,false);for(int i=0;i<2048;i++)pcm[i*2+1]=(short)(Math.cos(i*2*Math.PI*440/44100)*16000);service.waveform=AudioWaveform.analyze(pcm);service.playing=true;
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();PixelVisualizer viz=findVisualizer(a.getWindow().getDecorView());
            String[] names={"spectrum","waveform","stereo-scope","orbit"};
            for(int i=0;i<4;i++){viz.setFrame(service.levels,service.waveform,true,true);assertEquals(i,viz.mode());capture(a,"visualizer-"+names[i]+".png");viz.performClick();}
            assertEquals(0,viz.mode());viz.performClick();assertEquals(1,viz.mode());c.recreate();a=c.get();viz=findVisualizer(a.getWindow().getDecorView());assertEquals(1,viz.mode());
            a.beginThemeEdit(ThemeStore.from(PlayerTheme.find("dark"),false));viz=findVisualizer(a.getWindow().getDecorView());assertEquals(1,viz.mode());viz.performClick();assertEquals(1,viz.mode());
            c.recreate();a=c.get();viz=findVisualizer(a.getWindow().getDecorView());assertEquals(1,viz.mode());assertNotNull(a.editDraft());a.cancelThemeEdit();viz=findVisualizer(a.getWindow().getDecorView());assertEquals(1,viz.mode());viz.performLongClick();assertEquals(0,viz.mode());
        }
    }
    private PixelVisualizer findVisualizer(View view){if(view instanceof PixelVisualizer)return (PixelVisualizer)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){PixelVisualizer found=findVisualizer(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
    @Test public void themeEditorFreezesThePreviewAndCancelRestoresLiveState() throws Exception {
        Library.Track first=new Library.Track();first.path="/fixture/one.spc";first.title="Frozen song";first.game="Game";first.artist="Composer";first.system="Super Nintendo";first.length=180000;service.current=first;service.queue=java.util.Collections.singletonList(first);service.index=0;service.position=12000;service.playing=true;
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();a.beginThemeEdit(ThemeStore.from(PlayerTheme.find("light"),false));View decor=a.getWindow().getDecorView();assertNotNull(findDescription(decor,"Cancel theme edit"));assertEquals("light",ThemeStore.selected(app).id);
            decor.measure(View.MeasureSpec.makeMeasureSpec(822,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1782,View.MeasureSpec.EXACTLY));decor.layout(0,0,822,1782);
            View overlay=findDescription(decor,"Frozen theme preview. Use Parts or tap an element to edit colours.");View libraryButton=findDescription(decor,"Open music library");Rect bounds=new Rect();libraryButton.getDrawingRect(bounds);((FrameLayout)overlay.getParent()).offsetDescendantRectToMyCoords(libraryButton,bounds);android.view.MotionEvent tap=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);overlay.dispatchTouchEvent(tap);tap.recycle();android.app.Dialog direct=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findText(direct.getWindow().getDecorView(),"Library button"));findDescription(direct.getWindow().getDecorView(),"Paint #FFBE55").performClick();org.robolectric.shadows.ShadowLooper.idleMainLooper(120,java.util.concurrent.TimeUnit.MILLISECONDS);assertEquals(Integer.valueOf(0xffffbe55),a.editDraft().colors.get("libraryButton"));findDescription(direct.getWindow().getDecorView(),"DONE").performClick();assertNull(Shadows.shadowOf(a).getNextStartedActivityForResult());decor=a.getWindow().getDecorView();
            findDescription(decor,"Choose theme element").performClick();android.app.Dialog parts=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(parts.getWindow().getDecorView(),"Spectrum colours").performClick();android.app.Dialog picker=org.robolectric.shadows.ShadowDialog.getLatestDialog();captureView(picker.getWindow().getDecorView(),"theme-colour-picker.png",774,1040);
            findDescription(picker.getWindow().getDecorView(),"Paint #E9556F").performClick();org.robolectric.shadows.ShadowLooper.idleMainLooper(120,java.util.concurrent.TimeUnit.MILLISECONDS);assertEquals(Integer.valueOf(0xffe9556f),a.editDraft().colors.get("player.spectrumLow"));findDescription(picker.getWindow().getDecorView(),"UNDO").performClick();assertFalse(a.editDraft().colors.containsKey("player.spectrumLow"));assertEquals(Integer.valueOf(0xffffbe55),a.editDraft().colors.get("libraryButton"));findDescription(picker.getWindow().getDecorView(),"Paint #E9556F").performClick();org.robolectric.shadows.ShadowLooper.idleMainLooper(120,java.util.concurrent.TimeUnit.MILLISECONDS);findDescription(picker.getWindow().getDecorView(),"DONE").performClick();
            capture(a,"theme-editor.png");Library.Track live=new Library.Track();live.path="/fixture/two.spc";live.title="Live song";live.game="Game";live.artist="";live.system="Super Nintendo";live.length=180000;service.current=live;service.position=90000;service.listener.run();assertNotNull(findText(a.getWindow().getDecorView(),"Frozen song"));assertEquals(12000,findSeek(a.getWindow().getDecorView()).getProgress());
            c.recreate();a=c.get();assertNotNull(a.editDraft());assertEquals(Integer.valueOf(0xffe9556f),a.editDraft().colors.get("player.spectrumLow"));assertNotNull(findText(a.getWindow().getDecorView(),"Frozen song"));findDescription(a.getWindow().getDecorView(),"Cancel theme edit").performClick();assertNull(a.editDraft());assertEquals("light",ThemeStore.selected(app).id);assertTrue(new ThemeStore(app).drafts().isEmpty());assertTrue(service.playing);assertNotNull(findText(a.getWindow().getDecorView(),"Live song"));
        }
    }
    @Test public void themeEditorOffersOneTrackEntryAndClipsItToThePlaylist() throws Exception {
        org.json.JSONArray tracks=new org.json.JSONArray();for(int i=0;i<30;i++)tracks.put(new org.json.JSONObject().put("path","/fixture/"+i+".spc").put("title","Track "+i).put("game","Game").put("artist","Composer").put("system","Super Nintendo").put("index",0).put("length",180000));
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Game").put("id","fixture").put("tracks",tracks)).toString().getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();a.beginThemeEdit(ThemeStore.from(PlayerTheme.find("dark"),false));
            c.recreate();a=c.get();View decor=a.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(822,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1782,View.MeasureSpec.EXACTLY));decor.layout(0,0,822,1782);
            java.util.List<ThemeEditor.Target> targets=org.robolectric.util.ReflectionHelpers.getField(a,"editTargets");
            assertEquals(1,targets.stream().filter(t->t.title.equals("Tracklist entry")).count());
            ThemeEditor.Target entry=targets.stream().filter(t->t.title.equals("Tracklist entry")).findFirst().get();
            View overlay=findDescription(decor,"Frozen theme preview. Use Parts or tap an element to edit colours.");FrameLayout frame=(FrameLayout)overlay.getParent();
            Rect first=ThemeEditor.visibleBounds(frame,entry.view,new Rect());assertFalse(first.isEmpty());
            ScrollView scroll=findScroll(decor);Rect viewport=ThemeEditor.visibleBounds(frame,scroll,new Rect());assertTrue(viewport.contains(first));
            scroll.scrollTo(0,10000);assertTrue(ThemeEditor.visibleBounds(frame,entry.view,new Rect()).isEmpty());
            ThemeEditor.Target title=targets.stream().filter(t->t.title.equals("Track title")).findFirst().get();Rect titleBounds=ThemeEditor.visibleBounds(frame,title.view,new Rect());assertFalse(titleBounds.isEmpty());
            android.view.MotionEvent tap=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_UP,titleBounds.centerX(),titleBounds.centerY(),0);overlay.dispatchTouchEvent(tap);tap.recycle();
            android.app.Dialog picker=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findText(picker.getWindow().getDecorView(),"Track title"));findDescription(picker.getWindow().getDecorView(),"DONE").performClick();
            scroll.scrollTo(0,0);capture(a,"theme-editor-tracklist.png");
        }
    }
    @Test public void savedCustomThemeLoadsAfterRecreationAndInOtherScreens() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();a.beginThemeEdit(ThemeStore.from(PlayerTheme.find("dark"),false));a.paintTheme("background",0xff222244);a.paintTheme("player.spectrumHigh",0xffffbbaa);a.paintTheme("player.heading",0xffffbbaa);a.repaintTheme();findDescription(a.getWindow().getDecorView(),"Save custom theme").performClick();android.app.Dialog name=org.robolectric.shadows.ShadowDialog.getLatestDialog();((EditText)findDescription(name.getWindow().getDecorView(),"Theme name")).setText("Moonlight");findDescription(name.getWindow().getDecorView(),"SAVE THEME").performClick();
            assertNull(a.editDraft());assertEquals("Moonlight",ThemeStore.selected(app).name);assertEquals(0xff222244,ThemeStore.selected(app).background);assertEquals(1,new ThemeStore(app).drafts().size());c.recreate();assertEquals(0xff222244,((android.graphics.drawable.ColorDrawable)((ViewGroup)c.get().findViewById(android.R.id.content)).getChildAt(0).getBackground()).getColor());
            try(org.robolectric.android.controller.ActivityController<ImportActivity> importer=Robolectric.buildActivity(ImportActivity.class).setup()){assertEquals(0xff222244,((android.graphics.drawable.ColorDrawable)((ViewGroup)importer.get().findViewById(android.R.id.content)).getChildAt(0).getBackground()).getColor());}
            View widget=PlayerWidget.views(app,null).apply(app,new FrameLayout(app));assertEquals(0xffffbbaa,((TextView)widget.findViewById(R.id.widget_heading)).getCurrentTextColor());
            c.get().showSavedThemes();android.app.Dialog manager=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findDescription(manager.getWindow().getDecorView(),"Manage Moonlight theme"));captureView(manager.getWindow().getDecorView(),"saved-themes.png",774,900);
        }
    }
    private void finishThemeIo(MainActivity a)throws Exception {java.lang.reflect.Field field=MainActivity.class.getDeclaredField("importer");field.setAccessible(true);((java.util.concurrent.ExecutorService)field.get(a)).submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.shadows.ShadowLooper.idleMainLooper();}
    @Test public void customThemeExportAndImportUseDocumentPickersAndCreateAnIndependentCopy()throws Exception {
        ThemeStore.Draft draft=ThemeStore.from(PlayerTheme.find("dark"),false);draft.name="Shared palette";draft.colors.put("player.spectrumLow",0xffe9556f);new ThemeStore(app).save(draft);
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();a.exportTheme(draft);android.content.Intent save=Shadows.shadowOf(a).getNextStartedActivityForResult().intent;assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT,save.getAction());assertEquals("application/json",save.getType());File file=new File(app.getCacheDir(),"theme.json");a.onActivityResult(6,android.app.Activity.RESULT_OK,new android.content.Intent().setData(android.net.Uri.fromFile(file)));finishThemeIo(a);
            ThemeStore.Draft read;try(java.io.InputStream in=new java.io.FileInputStream(file)){read=ThemeStore.read(in);}assertEquals(draft.colors,read.colors);assertEquals(draft.name,read.name);
            a.importTheme();assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT,Shadows.shadowOf(a).getNextStartedActivityForResult().intent.getAction());a.onActivityResult(5,android.app.Activity.RESULT_OK,new android.content.Intent().setData(android.net.Uri.fromFile(file)));finishThemeIo(a);assertEquals(2,new ThemeStore(app).drafts().size());assertNotEquals(draft.id,ThemeStore.selected(app).id);assertEquals("Shared palette",ThemeStore.selected(app).name);assertEquals(0xffe9556f,ThemeStore.selected(app).nowPlaying().spectrumLow);
        }
    }
    @Test public void themedMenuGroupsNavigateAndUnavailableTrackActionsStayDisabled() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();a.selectTheme("desktop-98");findDescription(a.getWindow().getDecorView(),"App menu").performClick();
            android.app.Dialog menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();View decor=menu.getWindow().getDecorView();
            for(String label:new String[]{"Import music","Library","Playback queue","Favourites","Playback tools","Settings","Exit app"})assertNotNull(findDescription(decor,label));
            assertNotNull(findText(decor,"MUSIC"));assertNotNull(findText(decor,"PREFERENCES"));captureView(decor,"app-menu.png",774,1288);
            findDescription(decor,"Playback tools").performClick();menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();decor=menu.getWindow().getDecorView();
            assertFalse(findDescription(decor,"Export current track…").isEnabled());assertFalse(findDescription(decor,"Track SoundFont…").isEnabled());assertTrue(findDescription(decor,"Sleep timer").isEnabled());captureView(decor,"playback-menu.png",774,712);
            findDescription(decor,"Back to app menu").performClick();menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(menu.getWindow().getDecorView(),"Settings").performClick();
            menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();decor=menu.getWindow().getDecorView();assertNotNull(findDescription(decor,"Color theme"));assertNotNull(findDescription(decor,"SoundFonts"));assertNotNull(findDescription(decor,"About ChipAmp9x"));captureView(decor,"settings-menu.png",774,696);
            assertTrue(menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));assertTrue(menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)));menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(menu.getWindow().getDecorView(),"Playback queue").performClick();assertNotNull(findText(a.getWindow().getDecorView(),"Playback queue"));
        }
    }
    @Test public void soundFontMenuShowsDefaultBankAndLaunchesDocumentPicker() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            findDescription(c.get().getWindow().getDecorView(),"App menu").performClick();
            findDescription(org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Settings").performClick();
            findDescription(org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView(),"SoundFonts").performClick();
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertTrue(dialog.getListView().getAdapter().getItem(0).toString().contains("Pixel GM"));
            captureView(dialog.getWindow().getDecorView(),"soundfonts.png",720,350);
            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick();
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
            android.content.Intent picker=Shadows.shadowOf(c.get()).getNextStartedActivityForResult().intent;
            assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT,picker.getAction());assertEquals("*/*",picker.getType());
        }
    }
    @Test public void themePickerPersistsDarkModeAcrossRecreation() {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            findDescription(c.get().getWindow().getDecorView(),"App menu").performClick();
            findDescription(org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Settings").performClick();
            findDescription(org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Color theme").performClick();
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            GridLayout grid=findGrid(dialog.getWindow().getDecorView()); assertNotNull(grid);
            assertEquals(2,grid.getColumnCount()); assertEquals(PlayerTheme.ALL.length,grid.getChildCount());
            assertNotNull(findDescription(grid,"Select Pixel Light theme, selected"));
            assertEquals("Select Llama theme",grid.getChildAt(0).getContentDescription());
            try { captureView(dialog.getWindow().getDecorView(),"theme-picker.png",720,1500); } catch(Exception e) { throw new AssertionError(e); }
            findDescription(grid,"Select Pixel Dark theme").performClick();
            assertEquals("dark",app.getSharedPreferences("appearance",0).getString("theme",null));
            assertEquals(PlayerTheme.find("dark").background,((android.graphics.drawable.ColorDrawable)((ViewGroup)c.get().findViewById(android.R.id.content)).getChildAt(0).getBackground()).getColor());
            c.recreate();
            assertEquals(PlayerTheme.find("dark").background,((android.graphics.drawable.ColorDrawable)((ViewGroup)c.get().findViewById(android.R.id.content)).getChildAt(0).getBackground()).getColor());
            assertFalse(text(c.get().getWindow().getDecorView()).contains("SOUNDTRACKS NEVER GET OLD"));
        }
    }
    @Test @Config(shadows={PlaybackResumeTest.Decoder.class,PlaybackResumeTest.Output.class})
    public void libraryLongPressAddsWithoutInterruptingAndPlayStartsTheSoundtrack() throws Exception {
        org.json.JSONObject t=new org.json.JSONObject().put("path","/fixture/tone.spc").put("title","Tone").put("game","Game").put("artist","").put("system","SNES").put("index",0).put("length",180000).put("loopLength",0);
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Game").put("id","fixture").put("tracks",new org.json.JSONArray().put(t))).toString().getBytes("UTF-8"));
        Library.Track live=new Library.Track();live.path="/fixture/live.spc";live.title="Live";live.game="Other game";live.artist="";live.system="SNES";live.length=180000;service.current=live;service.queue=java.util.Collections.singletonList(live);service.index=0;service.position=12000;service.playing=true;
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();findDescription(a.getWindow().getDecorView(),"Open music library").performClick();android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            ExpandableListView list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");int row=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(0,0));
            assertFalse(list.getOnItemLongClickListener().onItemLongClick(list,null,0,0));assertTrue(list.getOnItemLongClickListener().onItemLongClick(list,null,row,0));
            AlertDialog actions=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(actions.getWindow().getDecorView(),"Add Game to queue").performClick();
            assertEquals(2,service.queue.size());assertSame(live,service.current);assertEquals(12000,service.position);assertTrue(service.playing);assertNotNull(findText(a.getWindow().getDecorView(),"Playback queue"));
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");row=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(0,0));list.getOnItemLongClickListener().onItemLongClick(list,null,row,0);
            actions=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(actions.getWindow().getDecorView(),"Play soundtrack Game").performClick();assertEquals(1,service.queue.size());assertEquals("/fixture/tone.spc",service.current.path);assertNull(findText(a.getWindow().getDecorView(),"Playback queue"));
        }
    }
    @Test @Config(qualifiers="w320dp-h640dp-xhdpi")
    public void largerPlaybackControlsStaySeparatedAndInsideANarrowScreen() {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();View decor=a.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY));decor.layout(0,0,640,1280);
            View previous=findDescription(decor,"Previous track");LinearLayout controls=(LinearLayout)previous.getParent();assertEquals(5,controls.getChildCount());
            for(int i=0;i<controls.getChildCount();i++){View control=controls.getChildAt(i);assertTrue(control.getWidth()>=a.dp(48));assertTrue(control.getHeight()>=a.dp(48));assertTrue(control.getRight()<=controls.getWidth());if(i>0)assertTrue(control.getLeft()-controls.getChildAt(i-1).getRight()>=a.dp(4));}
        }
    }
    @Test public void libraryRenameMenuValidatesAndUpdatesTheDisplayedName() throws Exception {
        org.json.JSONObject track=new org.json.JSONObject().put("path","/fixture/tone.spc").put("title","Tone").put("game","Game").put("artist","").put("system","SNES").put("index",0).put("length",180000).put("loopLength",0);
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Game").put("id","fixture").put("tracks",new org.json.JSONArray().put(track))).toString().getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();findDescription(a.getWindow().getDecorView(),"Open music library").performClick();android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();ExpandableListView list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");int row=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(0,0));list.getOnItemLongClickListener().onItemLongClick(list,null,row,0);
            android.app.Dialog actions=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(actions.getWindow().getDecorView(),"Rename Game in library").performClick();AlertDialog rename=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();EditText input=(EditText)findDescription(rename.getWindow().getDecorView(),"Library name");assertEquals("Game",input.getText().toString());
            input.setText("  ");rename.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();assertTrue(rename.isShowing());assertNotNull(input.getError());input.setText("My favourites set");rename.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();assertFalse(rename.isShowing());assertEquals("My favourites set",new Library(app).games.get(0).name);assertNotNull(findText(a.getWindow().getDecorView(),"My favourites set"));assertNotNull(findText(a.getWindow().getDecorView(),"Tone"));
        }
    }
    @Test public void libraryDeleteRequiresConfirmationAndUpdatesTheEmptyView() throws Exception {
        org.json.JSONObject t=new org.json.JSONObject().put("path","/fixture/tone.spc").put("title","Tone").put("game","Game").put("artist","").put("system","SNES").put("index",0).put("length",180000).put("loopLength",0);
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),new org.json.JSONArray().put(new org.json.JSONObject().put("name","Game").put("id","fixture").put("tracks",new org.json.JSONArray().put(t))).toString().getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();
            android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            // Force row inflation just as a rendered library picker would.
            captureView(browser.getWindow().getDecorView(),"library-delete.png",774,1500);
            assertNull(findDescription(browser.getWindow().getDecorView(),"Delete Game from library"));
            ExpandableListView list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");
            int position=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(0,0));
            assertTrue(list.getOnItemLongClickListener().onItemLongClick(list,null,position,0));
            AlertDialog actions=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();captureView(actions.getWindow().getDecorView(),"library-actions.png",720,850);
            View addQueue=findDescription(actions.getWindow().getDecorView(),"Add Game to queue"),delete=findDescription(actions.getWindow().getDecorView(),"Delete Game from library");
            assertSame(addQueue.getParent(),delete.getParent());assertTrue(delete.getTop()-addQueue.getBottom()>=a.dp(32));
            delete.performClick();
            AlertDialog confirm=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertEquals(1,new Library(app).games.size());
            confirm.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(new Library(app).games.isEmpty());
            assertNotNull(findText(a.getWindow().getDecorView(),"+  IMPORT MUSIC"));
        }
    }
    @Test public void appMenuOffersExitAndClosesTask() {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); findDescription(a.getWindow().getDecorView(),"App menu").performClick();
            android.app.Dialog menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertNotNull(menu);assertNotNull(findDescription(menu.getWindow().getDecorView(),"Exit app"));
            // Selecting Exit waits for service teardown before closing the task.
            findDescription(menu.getWindow().getDecorView(),"Exit app").performClick();
            try {
                java.lang.reflect.Field worker=PlaybackService.class.getDeclaredField("worker"); worker.setAccessible(true);
                Shadows.shadowOf(((android.os.Handler)worker.get(service)).getLooper()).idle();
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            } catch(Exception e) { throw new AssertionError(e); }
            assertTrue(a.isFinishing()); assertNull(service.current);
        }
    }
    @Test public void headerIconsAreEqualAndSpectrumBelongsToThePlaybackDeck() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); capture(a,"empty-library.png"); View decor=a.getWindow().getDecorView();
            View logo=findDescription(decor,"ChipAmp9x");
            assertTrue(logo instanceof ChipLogo); assertEquals(a.dp(42),logo.getHeight()); assertTrue(logo.getWidth()>a.dp(150));
            assertEquals("ChipAmp9x",a.getApplicationInfo().loadLabel(a.getPackageManager()).toString());
            assertTrue(((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0).getBackground() instanceof android.graphics.drawable.ColorDrawable);
            int top=-1;
            for(String label:new String[]{"App menu"}) {
                View v=findDescription(decor,label); assertTrue(v instanceof ImageButton);
                assertNull(((View)v.getParent()).getBackground());
                assertNull(v.getBackground());assertNull(((ImageButton)v).getImageTintList());
                assertEquals(a.dp(40),v.getWidth()); assertEquals(a.dp(40),v.getHeight());
                if(top<0) top=v.getTop(); else assertEquals(top,v.getTop());
                assertEquals(ImageView.ScaleType.CENTER,((ImageButton)v).getScaleType());
            }
            View deck=findDescription(decor,"Now Playing panel"),spectrum=findDescription(decor,"SPECTRUM visualizer. Tap for WAVEFORM. Hold to return to spectrum.");
            assertSame(deck,spectrum.getParent());
            for(String label:new String[]{"TRACKLIST"," NOW PLAYING","♪  44.1 kHz","READY"}) {
                TextView heading=(TextView)findText(decor,label); assertNotNull(heading);
                assertFalse(heading.getIncludeFontPadding()); assertEquals(Gravity.CENTER_VERTICAL,heading.getGravity()&Gravity.VERTICAL_GRAVITY_MASK);
                LinearLayout row=(LinearLayout)heading.getParent(); assertFalse(row.isBaselineAligned());
                int[] location=new int[2],rowAt=new int[2]; heading.getLocationOnScreen(location); row.getLocationOnScreen(rowAt);
                assertTrue(Math.abs(location[1]+heading.getHeight()/2-(rowAt[1]+row.getHeight()/2))<=2);
            }
            int[] spectrumAt=new int[2],libraryAt=new int[2]; spectrum.getLocationOnScreen(spectrumAt); findDescription(decor,"Open music library").getLocationOnScreen(libraryAt);
            assertTrue(libraryAt[1]<spectrumAt[1]);
            assertTrue(((ViewGroup)deck).indexOfChild(spectrum)<((ViewGroup)deck).indexOfChild(findSeek(decor)));
        }
    }
    private Library.Track[] playlistSources()throws Exception{
        Library.Track[] tracks=new Library.Track[2];org.json.JSONArray games=new org.json.JSONArray();
        for(int i=0;i<2;i++){Library.Track t=new Library.Track();t.path="/fixture/playlist-"+i+".spc";t.title="Track "+i;t.game="Game "+i;t.artist="Composer";t.system=i==0?"Super Nintendo":"Game Boy";t.length=180000;t.index=0;tracks[i]=t;games.put(new org.json.JSONObject().put("id","source-"+i).put("name",t.game).put("tracks",new org.json.JSONArray().put(t.json())));}
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),games.toString().getBytes("UTF-8"));return tracks;
    }
    private AlertDialog playlistSavePrompt(MainActivity a){findDescription(a.getWindow().getDecorView(),"App menu").performClick();android.app.Dialog menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(menu.getWindow().getDecorView(),"Save current playlist").performClick();return (AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();}
    @Test public void savedMixedPlaylistLoadsFromItsAccordionAndSupportsActionsWithoutInterruptingPlayback()throws Exception{
        Library.Track[] sources=playlistSources();Library.Track aTrack=sources[0],bTrack=sources[1];service.current=bTrack;service.queue=new java.util.ArrayList<>(java.util.Arrays.asList(bTrack,aTrack,bTrack));service.index=0;service.position=12345;service.playing=true;
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();findDescription(a.getWindow().getDecorView(),"App menu").performClick();android.app.Dialog menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(menu.getWindow().getDecorView(),"Playback queue").performClick();
            AlertDialog prompt=playlistSavePrompt(a);EditText name=(EditText)findDescription(prompt.getWindow().getDecorView(),"Playlist name");name.setText("Night Mix");captureView(prompt.getWindow().getDecorView(),"save-playlist.png",720,600);prompt.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
            assertSame(bTrack,service.current);assertTrue(service.playing);assertEquals(12345,service.position);
            java.util.List<Library.Game> saved=new Playlists(app).all(new Library(app).games);assertEquals(1,saved.size());assertEquals(3,saved.get(0).tracks.size());assertEquals(bTrack.path,saved.get(0).tracks.get(0).path);assertEquals(aTrack.path,saved.get(0).tracks.get(1).path);assertEquals(bTrack.path,saved.get(0).tracks.get(2).path);
            service.queue.add(aTrack);service.listener.run();findDescription(a.getWindow().getDecorView(),"Open music library").performClick();android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();ExpandableListView list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");assertEquals(3,list.getExpandableListAdapter().getGroupCount());assertTrue(list.isGroupExpanded(2));captureView(browser.getWindow().getDecorView(),"saved-playlists.png",774,1500);
            list.performItemClick(null,list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(2,0)),0);assertNotNull(findText(a.getWindow().getDecorView(),"Night Mix"));assertEquals(4,service.queue.size());assertSame(bTrack,service.current);
            java.util.List<Library.Track> playingQueue=service.queue;service.queue=new java.util.ArrayList<>(java.util.Arrays.asList(bTrack,aTrack,bTrack));service.index=2;service.listener.run();
            LinearLayout rows=org.robolectric.util.ReflectionHelpers.getField(a,"trackList");assertNull(rows.getChildAt(0).getBackground());assertNotNull(rows.getChildAt(4).getBackground());service.queue=playingQueue;service.index=0;service.listener.run();
            c.recreate();a=c.get();assertNotNull(findText(a.getWindow().getDecorView(),"Night Mix"));
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");int position=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(2,0));list.getOnItemLongClickListener().onItemLongClick(list,null,position,0);
            android.app.Dialog actions=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findDescription(actions.getWindow().getDecorView(),"Play playlist Night Mix"));findDescription(actions.getWindow().getDecorView(),"Add Night Mix to queue").performClick();assertEquals(7,service.queue.size());assertSame(bTrack,service.current);assertTrue(service.playing);
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");position=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(2,0));list.getOnItemLongClickListener().onItemLongClick(list,null,position,0);actions=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(actions.getWindow().getDecorView(),"Rename playlist Night Mix").performClick();AlertDialog rename=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();((EditText)findDescription(rename.getWindow().getDecorView(),"Playlist name")).setText("Chill Mix");rename.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();assertEquals("Chill Mix",new Playlists(app).all(new Library(app).games).get(0).name);
            findDescription(a.getWindow().getDecorView(),"Open music library").performClick();browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");position=list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(2,0));list.getOnItemLongClickListener().onItemLongClick(list,null,position,0);actions=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(actions.getWindow().getDecorView(),"Delete Chill Mix from library").performClick();AlertDialog delete=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findText(delete.getWindow().getDecorView(),"Delete playlist?"));delete.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(new Playlists(app).all(new Library(app).games).isEmpty());assertEquals(2,new Library(app).games.size());assertEquals(7,service.queue.size());assertSame(bTrack,service.current);assertTrue(service.playing);
        }
    }
    @Test public void replacingAPlaylistRequiresConfirmationAndCancelKeepsItsContents()throws Exception{
        Library.Track[] sources=playlistSources();new Playlists(app).save(null,"Same",java.util.Collections.singletonList(sources[0]));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            for(boolean replace:new boolean[]{false,true}){
                AlertDialog prompt=playlistSavePrompt(c.get());((EditText)findDescription(prompt.getWindow().getDecorView(),"Playlist name")).setText("Same");prompt.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();AlertDialog confirmation=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findText(confirmation.getWindow().getDecorView(),"Replace playlist?"));confirmation.getButton(replace?android.content.DialogInterface.BUTTON_POSITIVE:android.content.DialogInterface.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                assertEquals(sources[replace?1:0].path,new Playlists(app).all(new Library(app).games).get(0).tracks.get(0).path);
            }
        }
    }
    @Test public void emptyLibraryStillOffersPlaylistsAndImportWithoutSavingAnEmptyList()throws Exception{
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            findDescription(c.get().getWindow().getDecorView(),"Open music library").performClick();android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();ExpandableListView list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");assertEquals(1,list.getExpandableListAdapter().getGroupCount());assertEquals("Playlists",((LibraryPlatforms.Group)list.getExpandableListAdapter().getGroup(0)).name);assertFalse(findDescription(browser.getWindow().getDecorView(),"Save current playlist").isEnabled());
            findText(browser.getWindow().getDecorView(),"IMPORT MUSIC").performClick();assertEquals(ImportActivity.class.getName(),Shadows.shadowOf(c.get()).getNextStartedActivityForResult().intent.getComponent().getClassName());
        }
    }
    @Test public void libraryAccordionsKeepTheirStateAndSelectTheCorrectSoundtrack() throws Exception {
        org.json.JSONArray games=new org.json.JSONArray();
        for(String[] info:new String[][]{{"Super Nintendo","Donkey Kong Country"},{"Game Boy","Tetris"},{"Sega Genesis","Sonic the Hedgehog"},{"SNES","Super Metroid"}}) {
            org.json.JSONObject t=new org.json.JSONObject().put("path","/fixture/"+info[1]+".spc").put("title","Theme").put("game",info[1]).put("artist","").put("system",info[0]).put("index",0).put("length",180000).put("loopLength",0);
            games.put(new org.json.JSONObject().put("name",info[1]).put("id",info[1]).put("tracks",new org.json.JSONArray().put(t)));
        }
        Files.write(new File(app.getFilesDir(),"library.json").toPath(),games.toString().getBytes("UTF-8"));
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); View opener=findDescription(a.getWindow().getDecorView(),"Open music library"); opener.performClick();
            android.app.Dialog browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            ExpandableListView list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");
            assertEquals(4,list.getExpandableListAdapter().getGroupCount());
            assertTrue(browser.getWindow().getAttributes().height>=a.dp(650));
            assertTrue(list.isGroupExpanded(2)); assertFalse(list.isGroupExpanded(0));
            list.expandGroup(0); list.collapseGroup(2); captureView(browser.getWindow().getDecorView(),"library-platforms.png",774,1500);
            assertTrue(list.getHeight()>=a.dp(500)); browser.dismiss();
            opener.performClick(); browser=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            list=(ExpandableListView)findDescription(browser.getWindow().getDecorView(),"Soundtracks grouped by platform");
            assertTrue(list.isGroupExpanded(0)); assertFalse(list.isGroupExpanded(2));
            list.performItemClick(null,list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(0,0)),0);
            assertTrue(text(a.getWindow().getDecorView()).contains("Tetris"));
        }
    }
    private static Bitmap bottomEdge(View frame) {
        Bitmap bitmap=Bitmap.createBitmap(frame.getWidth(),frame.getHeight(),Bitmap.Config.ARGB_8888); frame.draw(new Canvas(bitmap));
        int height=frame.getPaddingBottom();
        return Bitmap.createBitmap(bitmap,0,bitmap.getHeight()-height,bitmap.getWidth(),height);
    }
    private static ScrollView findScroll(View view) {
        if(view instanceof ScrollView) return (ScrollView)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            ScrollView result=findScroll(((ViewGroup)view).getChildAt(i)); if(result!=null) return result;
        }
        return null;
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
