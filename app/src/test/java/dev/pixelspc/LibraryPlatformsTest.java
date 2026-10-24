package dev.pixelspc;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LibraryPlatformsTest {
    private Library.Game game(String name,String... systems) {
        Library.Game game=new Library.Game(); game.name=name;
        for(String system:systems) { Library.Track t=new Library.Track(); t.system=system; game.tracks.add(t); }
        return game;
    }
    @Test public void playstationMetadataAliasesShareOneGroup() {
        assertEquals("PlayStation",LibraryPlatforms.canonical("Sony PlayStation"));
        assertEquals("PlayStation",LibraryPlatforms.canonical("PS1"));
        assertEquals("PlayStation",LibraryPlatforms.canonical("PSX"));
        assertEquals("Nintendo 64",LibraryPlatforms.canonical("N64"));
        assertEquals("Nintendo 64",LibraryPlatforms.canonical("Nintendo64"));
    }
    @Test public void metadataAliasesShareAPlatformWithoutGuessingUnknownVgmSystems() {
        assertEquals("Super Nintendo",LibraryPlatforms.platform(game("a","SNES","Super Famicom","Super Nintendo")));
        assertEquals("Game Boy",LibraryPlatforms.canonical("Nintendo Gameboy"));
        assertEquals("Mega Drive / Genesis",LibraryPlatforms.canonical("Sega Mega Drive / Genesis"));
        assertEquals("Sega SMS/Genesis",LibraryPlatforms.canonical("Sega SMS/Genesis"));
        assertEquals("Other systems",LibraryPlatforms.canonical(null));
    }
    @Test public void groupsAndTitlesAreSortedAndDuplicateImportsAreRetained() {
        Library.Game a=game("Zelda","SNES"),b=game("Mario","Super Nintendo"),c=game("Tetris","Game Boy");
        List<LibraryPlatforms.Group> groups=LibraryPlatforms.group(Arrays.asList(a,b,c,b));
        assertEquals("Game Boy",groups.get(0).name); assertEquals("Super Nintendo",groups.get(1).name);
        assertEquals(3,groups.get(1).games.size()); assertSame(b,groups.get(1).games.get(0)); assertEquals(3,groups.get(1).trackCount());
    }
    @Test public void mixedArchivesGetOneGroupAndMissingTagsDoNotSplitKnownPlatforms() {
        assertEquals("Multiple systems",LibraryPlatforms.platform(game("Mixed","SNES","Game Boy")));
        assertEquals("Super Nintendo",LibraryPlatforms.platform(game("Missing","",null,"SNES")));
    }
}
