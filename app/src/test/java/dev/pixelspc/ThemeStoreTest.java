package dev.pixelspc;
import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class ThemeStoreTest {
    private Context context;private ThemeStore store;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("custom-themes",0).edit().clear().commit();context.getSharedPreferences("appearance",0).edit().clear().commit();store=new ThemeStore(context);}
    @Test public void namedThemeRoundTripsIndependentPlayerLibraryAndSpectrumColours()throws Exception{
        ThemeStore.Draft d=ThemeStore.from(PlayerTheme.find("desktop-98"),false);d.name="Night sky";d.colors.put("background",0xff101020);d.colors.put("player.background",0xff050509);d.colors.put("library.text",0xffccbbee);d.colors.put("player.spectrumLow",0xff55aaee);d.colors.put("player.spectrumMid",0xffcc55aa);d.colors.put("player.spectrumHigh",0xffeeaa55);d.colors.put("player.button",0xff334455);store.save(d);
        context.getSharedPreferences("appearance",0).edit().putString("theme",d.id).commit();PlayerTheme selected=ThemeStore.selected(context);assertEquals("Night sky",selected.name);assertEquals(0xff101020,selected.background);assertEquals(0xff050509,selected.nowPlaying().background);assertEquals(0xffccbbee,selected.library().text);assertTrue(selected.nowPlaying().spectrumGradient());assertEquals(0xff55aaee,selected.nowPlaying().spectrumLow);assertEquals(0xff334455,selected.nowPlaying().buttonFace(0));
        ThemeStore.Draft imported=ThemeStore.read(new ByteArrayInputStream(ThemeStore.encode(d).toString().getBytes("UTF-8")));assertNotEquals(d.id,imported.id);assertEquals(d.colors,imported.colors);store.save(imported);assertEquals(2,store.drafts().size());assertEquals(PlayerTheme.ALL.length+2,store.all().size());
        ThemeStore.Draft copy=ThemeStore.from(selected,true);copy.colors.put("background",0xffeeeeee);assertEquals(0xff101020,ThemeStore.selected(context).background);store.delete(d.id);assertEquals("light",ThemeStore.selected(context).id);assertEquals(1,store.drafts().size());
    }
    @Test public void invalidAndOversizedImportsCannotOverwriteSavedThemes()throws Exception{
        ThemeStore.Draft d=ThemeStore.from(PlayerTheme.find("light"),false);store.save(d);
        for(JSONObject bad:new JSONObject[]{ThemeStore.encode(d).put("version",99),ThemeStore.encode(d).put("base","missing"),ThemeStore.encode(d).put("colors",new JSONObject().put("execute","#FFFFFF")),ThemeStore.encode(d).put("colors",new JSONObject().put("background","#00FFFFFF"))})try{store.save(ThemeStore.decode(bad));fail();}catch(Exception expected){assertEquals(1,store.drafts().size());}
        try{ThemeStore.read(new ByteArrayInputStream(new byte[ThemeStore.FILE_LIMIT+1]));fail();}catch(IOException expected){assertEquals(1,store.drafts().size());}
    }
    @Test public void editingAnExistingThemeUpdatesOnlyThatThemeAndNamesAreValidated()throws Exception{
        ThemeStore.Draft a=ThemeStore.from(PlayerTheme.find("dark"),false),b=ThemeStore.from(PlayerTheme.find("light"),false);store.save(a);store.save(b);a.name="Renamed";a.colors.put("heading",0xffffbbaa);store.save(a);assertEquals(2,store.drafts().size());assertEquals("Renamed",store.drafts().get(0).name);assertEquals(b.name,store.drafts().get(1).name);
        a.name=" ";try{store.save(a);fail();}catch(IOException expected){assertEquals("Renamed",store.drafts().get(0).name);}
    }
}
