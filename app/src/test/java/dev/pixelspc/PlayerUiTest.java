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
        app.getSharedPreferences("library-browser",0).edit().clear().commit();
        app.getSharedPreferences("playback",0).edit().clear().commit();
        app.getSharedPreferences("favourites",0).edit().clear().commit();
        app.getSharedPreferences("soundfonts",0).edit().clear().commit();
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
            }
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
    @Test public void themedMenuGroupsNavigateAndUnavailableTrackActionsStayDisabled() throws Exception {
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get();a.selectTheme("desktop-98");findDescription(a.getWindow().getDecorView(),"App menu").performClick();
            android.app.Dialog menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();View decor=menu.getWindow().getDecorView();
            for(String label:new String[]{"Import music","Library","Playback queue","Favourites","Playback tools","Settings","Exit app"})assertNotNull(findDescription(decor,label));
            assertNotNull(findText(decor,"MUSIC"));assertNotNull(findText(decor,"PREFERENCES"));captureView(decor,"app-menu.png",774,1200);
            findDescription(decor,"Playback tools").performClick();menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();decor=menu.getWindow().getDecorView();
            assertFalse(findDescription(decor,"Export current track…").isEnabled());assertFalse(findDescription(decor,"Track SoundFont…").isEnabled());assertTrue(findDescription(decor,"Sleep timer").isEnabled());captureView(decor,"playback-menu.png",774,712);
            findDescription(decor,"Back to app menu").performClick();menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();findDescription(menu.getWindow().getDecorView(),"Settings").performClick();
            menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();decor=menu.getWindow().getDecorView();assertNotNull(findDescription(decor,"Color theme"));assertNotNull(findDescription(decor,"SoundFonts"));assertNotNull(findDescription(decor,"About ChipPlayer 199x"));captureView(decor,"settings-menu.png",774,696);
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
            View logo=findDescription(decor,"ChipPlayer 199x");
            assertTrue(logo instanceof ChipLogo); assertEquals(a.dp(42),logo.getHeight()); assertTrue(logo.getWidth()>a.dp(150));
            assertEquals("ChipPlayer 199x",a.getApplicationInfo().loadLabel(a.getPackageManager()).toString());
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
            View deck=findDescription(decor,"Now Playing panel"),spectrum=findDescription(decor,"Audio spectrum visualizer, 24 frequency bands");
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
            assertEquals(3,list.getExpandableListAdapter().getGroupCount());
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
