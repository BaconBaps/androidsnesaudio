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
    @Test public void mediaServiceCreatesBindsAndReleasesWithoutATrack() {
        ServiceController<PlaybackService> controller=Robolectric.buildService(PlaybackService.class).create();
        PlaybackService service=controller.get();
        assertSame(service,((PlaybackService.LocalBinder)service.onBind(new Intent())).get());
        assertFalse(service.playing); assertNull(service.current);
        controller.destroy();
    }
}
