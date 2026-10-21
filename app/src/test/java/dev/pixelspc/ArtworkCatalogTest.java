package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class ArtworkCatalogTest {
    private Context context;
    private ArtworkCatalog catalog;
    @Before public void setup() throws Exception { context=RuntimeEnvironment.getApplication(); context.getSharedPreferences("artwork",0).edit().clear().commit(); catalog=new ArtworkCatalog(context); }
    static Library.Game game(String name,String extension) { Library.Game g=new Library.Game(); g.id=name; g.name=name; Library.Track t=new Library.Track(); t.path="/fixture/game."+extension; t.game=name; g.tracks.add(t); return g; }
    @Test public void suppliedPackMatchesBothSampleGameMetadataNames() {
        assertEquals(379,catalog.items.size());
        assertNotNull(catalog.find(game("Donkey Kong Country","spc")));
        assertEquals("Street Fighter II Turbo",catalog.find(game("Street Fighter 2 Turbo","spc")).name);
        assertNull(catalog.find(game("Donkey Kong Country","gbs")));
        assertNull(catalog.find(game("A game absent from the pack","spc")));
    }
    @Test public void manualOverridePersistsAndAutoMatchRestores() throws Exception {
        Library.Game game=game("Donkey Kong Country","spc"); String automatic=catalog.find(game).file;
        ArtworkCatalog.Item choice=catalog.match("Street Fighter 2 Turbo"); catalog.choose(game,choice);
        assertEquals(choice.file,new ArtworkCatalog(context).find(game).file);
        catalog.choose(game,null); assertEquals(automatic,catalog.find(game).file);
        Library.Game nes=game("Donkey Kong Country","nsf"); catalog.choose(nes,choice); assertNull(catalog.find(nes));
    }
    @Test public void everyBundledImageHasValidBoundsAndRepresentativeLogosDecode() throws Exception {
        Set<String> seen=new HashSet<>();
        for(ArtworkCatalog.Item item:catalog.items) if(seen.add(item.file)) {
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            try(InputStream in=context.getAssets().open(item.file)) { BitmapFactory.decodeStream(in,null,options); }
            assertTrue(item.name,options.outWidth>0 && options.outHeight>0);
        }
        assertEquals(336,seen.size()); assertNotNull(catalog.load(catalog.match("Donkey Kong Country"),512)); assertNotNull(catalog.load(catalog.match("Street Fighter 2 Turbo"),128));
    }
}
