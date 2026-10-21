package dev.pixelspc;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PlayerThemeTest {
    @Test public void allPalettesHaveReadableTextAndStableUniqueIds() {
        Set<String> ids=new HashSet<>();
        for(PlayerTheme t:PlayerTheme.ALL) {
            assertTrue(t.id,ids.add(t.id)); assertSame(t,PlayerTheme.find(t.id));
            assertTrue(t.name+" text",PlayerTheme.contrast(t.background,t.text)>=7);
            assertTrue(t.name+" muted text",PlayerTheme.contrast(t.background,t.muted)>=4.5);
            assertTrue(t.name+" accent text",PlayerTheme.contrast(t.background,t.accent)>=4.5);
            assertTrue(t.name+" active track",PlayerTheme.contrast(t.surface,t.accent)>=4.5);
            assertTrue(t.name+" play button",PlayerTheme.contrast(t.accent,t.onAccent)>=4.5);
            assertTrue(t.name+" badge",PlayerTheme.contrast(t.secondary,t.chipText)>=4.5);
        }
        assertSame(PlayerTheme.ALL[0],PlayerTheme.find("retired-theme"));
    }
}
