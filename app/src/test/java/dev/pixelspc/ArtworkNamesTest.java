package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkNamesTest {
    @Test public void regionRevisionAndMarqueeSuffixMatchMetadata() {
        assertEquals(ArtworkNames.key("Donkey Kong Country"),ArtworkNames.key("Donkey Kong Country (U) (V1.2) [!]-marquee.png"));
        assertEquals(ArtworkNames.key("The 7th Saga"),ArtworkNames.key("7th Saga, The.png"));
        assertEquals(ArtworkNames.key("Street Fighter 2 Turbo"),ArtworkNames.key("Street Fighter II Turbo.png"));
        assertEquals(ArtworkNames.key("Batman & Robin"),ArtworkNames.key("Batman and Robin"));
    }
    @Test public void sequelsAndMeaningfulSubtitlesStayDistinct() {
        assertNotEquals(ArtworkNames.key("Donkey Kong Country"),ArtworkNames.key("Donkey Kong Country 2"));
        assertNotEquals(ArtworkNames.key("Street Fighter II Turbo"),ArtworkNames.key("Super Street Fighter II"));
        assertNotEquals(ArtworkNames.key("Game (Lost World)"),ArtworkNames.key("Game"));
    }
}
