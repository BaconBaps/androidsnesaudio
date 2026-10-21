package dev.pixelspc;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class PlaybackServiceTest {
    @Test public void shuffleChangesNextSelectionAndModesSurviveServiceRestart() {
        ServiceController<PlaybackService> first=Robolectric.buildService(PlaybackService.class).create();
        PlaybackService service=first.get();
        service.queue=java.util.Arrays.asList(new Library.Track(),new Library.Track(),new Library.Track()); service.index=1;
        service.setShuffle(false); assertEquals(2,service.nextIndex(1)); assertEquals(0,service.nextIndex(-1));
        service.setShuffle(true); for(int i=0;i<50;i++) { int selected=service.nextIndex(1); assertTrue(selected>=0 && selected<3); assertNotEquals(1,selected); }
        Library.Track a=new Library.Track(),b=new Library.Track(); a.path="a.spc"; b.path="b.spc"; a.loopLength=b.loopLength=1000;
        service.setTrackLoop(a,true); service.setTrackLoop(b,false);
        assertTrue(service.isLooping(a)); assertFalse(service.isLooping(b));
        service.setLoop(true); first.destroy();
        ServiceController<PlaybackService> second=Robolectric.buildService(PlaybackService.class).create();
        assertTrue(second.get().shuffle); assertTrue(second.get().repeat);
        assertTrue(second.get().isLooping(a)); assertFalse(second.get().isLooping(b)); second.destroy();
    }
    @Test public void mediaServiceCreatesBindsAndReleasesWithoutATrack() {
        ServiceController<PlaybackService> controller=Robolectric.buildService(PlaybackService.class).create();
        PlaybackService service=controller.get();
        assertSame(service,((PlaybackService.LocalBinder)service.onBind(new Intent())).get());
        assertFalse(service.playing); assertNull(service.current);
        controller.destroy();
    }
}
