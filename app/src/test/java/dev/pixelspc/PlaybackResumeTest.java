package dev.pixelspc;

import android.media.AudioTrack;
import android.os.Handler;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowAudioTrack;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=35,shadows={PlaybackResumeTest.Decoder.class,PlaybackResumeTest.Output.class})
@LooperMode(LooperMode.Mode.PAUSED)
public class PlaybackResumeTest {
    @Implements(value=Engine.class,isInAndroidSdk=false)
    public static class Decoder {
        static int position,fontChanges;
        static Runnable onSeek;
        @Implementation protected static void __staticInitializer__() {}
        @Implementation protected static long open(String path) { return 1; }
        @Implementation protected static void close(long handle) {}
        @Implementation protected static void soundFont(long handle,String path) {fontChanges++;}
        @Implementation protected static void start(long h,int index,int length) { position=0; }
        @Implementation protected static void setLoop(long h,boolean enabled,int length) {}
        @Implementation protected static void seek(long h,int ms) {
            position=(int)((long)ms*44100/1000*1000/44100);
            if(onSeek!=null) { Runnable action=onSeek; onSeek=null; action.run(); }
        }
        @Implementation protected static int tell(long h) { return position; }
        @Implementation protected static boolean render(long h,short[] pcm) { position+=46; return false; }
    }
    @Implements(AudioTrack.class)
    public static class Output extends ShadowAudioTrack {
        // A full audio sink must yield, allowing seek and pause commands through.
        @Implementation protected int write(short[] data,int offset,int size,int mode) {
            assertEquals(AudioTrack.WRITE_NON_BLOCKING,mode); return 0;
        }
    }
    private static Object field(Object object,String name) throws Exception {
        Field f=PlaybackService.class.getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    private static void drain(PlaybackService service) throws Exception {
        Shadows.shadowOf(((Handler)field(service,"worker")).getLooper()).idle();
    }
    @Test public void changingMidiBankRestoresPositionAndPausedOrPlayingState() throws Exception {
        ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create();PlaybackService service=c.get();
        try {
            Library.Track track=new Library.Track();track.path="fixture.mid";track.title="MIDI";track.length=120000;
            Decoder.fontChanges=0;service.play(Collections.singletonList(track),0);drain(service);assertEquals(1,Decoder.fontChanges);
            service.seek(5033);drain(service);int before=service.position;service.reloadSoundFont();drain(service);
            assertEquals(2,Decoder.fontChanges);assertEquals("",service.error);assertTrue(service.playing);assertTrue("Resume may pre-render one 46 ms buffer",Math.abs(service.position-before)<=50);assertEquals(-1,service.seekTarget);
            assertEquals(AudioTrack.PLAYSTATE_PLAYING,((AudioTrack)field(service,"audio")).getPlayState());
            service.pause();drain(service);before=service.position;service.reloadSoundFont();drain(service);
            assertEquals(3,Decoder.fontChanges);assertEquals("",service.error);assertFalse(service.playing);assertTrue(Math.abs(service.position-before)<=2);assertEquals(-1,service.seekTarget);
            assertEquals(AudioTrack.PLAYSTATE_PAUSED,((AudioTrack)field(service,"audio")).getPlayState());
        } finally {c.destroy();}
    }
    @Test public void stopClearsPlaybackAndNotificationAndAllowsAnotherTrack() throws Exception {
        ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create();
        PlaybackService service=c.get();
        try {
            Library.Track track=new Library.Track(); track.path="fixture.vgz"; track.title="Tone"; track.length=120000;
            service.play(Collections.singletonList(track),0); drain(service);
            final boolean[] completed={false};
            service.stopPlayback(()->completed[0]=true); drain(service);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(completed[0]); assertNull(service.current); assertTrue(service.queue.isEmpty());
            assertFalse(service.playing); assertEquals(0,service.position); assertNull(field(service,"audio")); assertEquals(0L,field(service,"handle"));
            android.app.NotificationManager manager=(android.app.NotificationManager)service.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            assertEquals(0,manager.getActiveNotifications().length);
            service.play(Collections.singletonList(track),0); drain(service); assertTrue(service.playing);
        } finally { c.destroy(); }
    }
    @Test public void widgetCommandsControlTheExistingDecoderAndQueue() throws Exception {
        ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create(); PlaybackService service=c.get();
        try {
            Library.Track first=new Library.Track(); first.path="first.vgz"; first.title="First"; first.length=120000;
            Library.Track second=new Library.Track(); second.path="second.spc"; second.title="Second"; second.length=120000;
            service.play(java.util.Arrays.asList(first,second),0); drain(service);
            service.onStartCommand(new android.content.Intent().setAction("widget:toggle"),0,1); drain(service); assertFalse(service.playing);
            service.onStartCommand(new android.content.Intent().setAction("widget:toggle"),0,2); drain(service); assertTrue(service.playing);
            service.onStartCommand(new android.content.Intent().setAction("widget:next"),0,3); drain(service); assertSame(second,service.current);
            service.onStartCommand(new android.content.Intent().setAction("widget:previous"),0,4); drain(service); assertSame(first,service.current);
        } finally { c.destroy(); }
    }
    @Test public void sleepTimerPausesWithoutLosingPositionAndCanBeCancelled() throws Exception {
        ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create(); PlaybackService service=c.get();
        try {
            Library.Track track=new Library.Track(); track.path="fixture.vgz"; track.title="Tone"; track.length=120000;
            service.play(Collections.singletonList(track),0); drain(service); service.seek(5033); drain(service);
            service.setLoop(true); drain(service); service.setSleepTimer(1000); assertTrue(service.sleepRemainingMs()>0);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1)); drain(service);
            assertFalse(service.playing); assertSame(track,service.current); assertTrue(service.position>=5032); assertEquals(0,service.sleepRemainingMs());
            service.toggle(); drain(service); service.setSleepTimer(1000); service.setSleepTimer(0);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2)); drain(service); assertTrue(service.playing);
            service.setSleepTimer(60000); service.stopPlayback(null); drain(service); assertEquals(0,service.sleepRemainingMs());
        } finally { c.destroy(); }
    }
    @Test public void notificationExitReleasesAudioAndRemovesNotification() throws Exception {
        ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create();
        PlaybackService service=c.get();
        try {
            Library.Track track=new Library.Track(); track.path="fixture.vgz"; track.title="Tone"; track.length=120000;
            service.play(Collections.singletonList(track),0); drain(service);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            android.app.NotificationManager manager=service.getSystemService(android.app.NotificationManager.class);
            android.app.Notification notification=manager.getActiveNotifications()[0].getNotification();
            assertNull(notification.getLargeIcon());
            assertFalse(notification.extras.getBoolean(android.app.Notification.EXTRA_COLORIZED));
            assertEquals("Exit app",notification.actions[3].title);
            android.content.Intent exit=Shadows.shadowOf(notification.actions[3].actionIntent).getSavedIntent();
            assertEquals("exit",exit.getAction());
            service.onStartCommand(exit,0,1); drain(service);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertNull(service.current); assertFalse(service.playing); assertNull(field(service,"audio"));
            assertEquals(0,manager.getActiveNotifications().length);
        } finally { c.destroy(); }
    }
    @Test public void arbitrarySeekResumesOutputWithoutAnExtraButtonPressAndPausedSeekStaysPaused() throws Exception {
        ServiceController<PlaybackService> c=Robolectric.buildService(PlaybackService.class).create();
        PlaybackService service=c.get();
        try {
            Library.Track track=new Library.Track(); track.path="fixture.vgz"; track.title="Tone"; track.length=120000;
            service.play(Collections.singletonList(track),0); drain(service);
            assertTrue(service.playing);
            for(boolean loop:new boolean[]{false,true}) {
                service.setLoop(loop); service.seek(12347); drain(service);
                assertEquals(-1,service.seekTarget); assertEquals("",service.error); assertTrue(service.playing);
                assertEquals(AudioTrack.PLAYSTATE_PLAYING,((AudioTrack)field(service,"audio")).getPlayState());
                assertTrue(service.position>=12346);
            }
            Decoder.onSeek=service::pause; service.seek(60001); drain(service);
            assertFalse(service.playing); assertEquals(-1,service.seekTarget); assertTrue(service.position<60000);
            service.seek(5033); drain(service);
            assertEquals(-1,service.seekTarget); assertEquals("",service.error); assertFalse(service.playing);
            assertEquals(AudioTrack.PLAYSTATE_PAUSED,((AudioTrack)field(service,"audio")).getPlayState());
            service.toggle(); drain(service); assertTrue(service.playing);
            assertEquals(AudioTrack.PLAYSTATE_PLAYING,((AudioTrack)field(service,"audio")).getPlayState());
        } finally { c.destroy(); }
    }
}
