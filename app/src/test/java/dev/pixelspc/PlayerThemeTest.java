package dev.pixelspc;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PlayerThemeTest {
    @Test public void playbackDeckIsDarkerAndItsAdaptedColorsStayReadable() {
        for(PlayerTheme theme:PlayerTheme.ALL) {
            PlayerTheme p=theme.nowPlaying();
            assertTrue(theme.name,PlayerTheme.luminance(p.background)<PlayerTheme.luminance(theme.background));
            assertTrue(PlayerTheme.contrast(p.background,p.text)>=7);
            assertTrue(PlayerTheme.contrast(p.background,p.muted)>=4.5);
            assertTrue(PlayerTheme.contrast(p.surface,p.accent)>=4.5);
            assertTrue(PlayerTheme.contrast(p.accent,p.onAccent)>=4.5);
        }
    }
    @Test public void dimmedLibraryKeepsReadableTextAndLibraryButtonIsLighter() {
        for(PlayerTheme theme:PlayerTheme.ALL) {
            PlayerTheme list=theme.library();
            assertTrue(theme.name,PlayerTheme.luminance(list.background)<PlayerTheme.luminance(theme.background));
            assertTrue(theme.name+" text",PlayerTheme.contrast(list.background,list.text)>=7);
            assertTrue(theme.name+" muted",PlayerTheme.contrast(list.background,list.muted)>=4.5);
            assertTrue(theme.name+" selected",PlayerTheme.contrast(list.surface,list.accent)>=4.5);
            assertTrue(theme.name+" library button",PlayerTheme.luminance(theme.libraryButtonBackground())>PlayerTheme.luminance(theme.background));
            assertTrue(theme.name+" button text",PlayerTheme.contrast(theme.libraryButtonBackground(),theme.accent)>=4.5);
        }
    }
    @Test public void allPalettesHaveReadableTextAndStableUniqueIds() {
        Set<String> ids=new HashSet<>();
        for(PlayerTheme t:PlayerTheme.ALL) {
            assertTrue(t.id,ids.add(t.id)); assertSame(t,PlayerTheme.find(t.id));
            assertTrue(t.name+" text",PlayerTheme.contrast(t.background,t.text)>=7);
            assertTrue(t.name+" muted text",PlayerTheme.contrast(t.background,t.muted)>=4.5);
            assertTrue(t.name+" accent text",PlayerTheme.contrast(t.background,t.accent)>=4.5);
            assertTrue(t.name+" active track",PlayerTheme.contrast(t.surface,t.accent)>=4.5);
            assertTrue(t.name+" play button",PlayerTheme.contrast(t.accent,t.onAccent)>=4.5);
        }
        assertEquals(10,PlayerTheme.ALL.length);
        for(String removed:new String[]{"retired-theme","playstation","dreamcast","n64"})
            assertSame(PlayerTheme.ALL[0],PlayerTheme.find(removed));
    }
}
