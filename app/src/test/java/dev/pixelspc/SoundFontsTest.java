package dev.pixelspc;
import android.content.*;
import android.net.Uri;
import java.io.*;
import java.nio.file.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,shadows=SoundFontsTest.NativeCheck.class)
public class SoundFontsTest {
    @Implements(value=Engine.class,isInAndroidSdk=false)
    public static class NativeCheck {
        static boolean reject;
        @Implementation protected static void __staticInitializer__() {}
        @Implementation protected static void validateSoundFont(String path) throws IOException {if(reject)throw new IOException("Invalid SoundFont");}
    }
    private Context context;private SoundFonts store;
    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication();context.getSharedPreferences("soundfonts",0).edit().clear().commit();NativeCheck.reject=false;
        File folder=new File(context.getFilesDir(),"soundfonts");if(folder.exists())ArchiveImporter.removeTree(folder);store=new SoundFonts(context);
    }
    @Test public void builtinIsAvailableAndDefaultAndOverridesPersist() throws Exception {
        SoundFonts.Bank builtin=store.banks().get(0);assertTrue(builtin.bundled);assertTrue(builtin.file.length()>1000);assertEquals(builtin.file.getAbsolutePath(),store.pathFor("song.mid"));
        File source=new File(context.getCacheDir(),"test.sf2");Files.write(source.toPath(),new byte[]{1,2,3});SoundFonts.Bank added=store.importBank(Uri.fromFile(source));
        assertEquals(2,store.banks().size());assertArrayEquals(Files.readAllBytes(source.toPath()),Files.readAllBytes(added.file.toPath()));
        store.setDefault(added.id);store.setTrack("song.mid",SoundFonts.BUILTIN);
        SoundFonts restored=new SoundFonts(context);assertEquals(builtin.file.getAbsolutePath(),restored.pathFor("song.mid"));assertEquals(added.file.getAbsolutePath(),restored.pathFor("other.mid"));
        restored.setTrack("song.mid","");assertEquals(added.file.getAbsolutePath(),restored.pathFor("song.mid"));
        restored.rename(added,"My retro bank");assertEquals("My retro bank",restored.banks().get(1).name);assertTrue(source.exists());
    }
    @Test public void deletionClearsReferencesAndKeepsOriginalAndBuiltin() throws Exception {
        File source=new File(context.getCacheDir(),"test.sf2");Files.write(source.toPath(),new byte[]{1,2,3});SoundFonts.Bank added=store.importBank(Uri.fromFile(source));
        store.setDefault(added.id);store.setTrack("first.mid",added.id);store.setTrack("second.mid",added.id);store.delete(added);
        assertFalse(added.file.exists());assertTrue(source.exists());assertEquals("",store.overrideId("first.mid"));assertEquals("",store.overrideId("second.mid"));assertEquals(SoundFonts.BUILTIN,store.defaultId());assertEquals(store.banks().get(0).file.getAbsolutePath(),store.pathFor("first.mid"));
        try{store.delete(store.banks().get(0));fail();}catch(IOException expected){assertEquals(1,store.banks().size());}
    }
    @Test public void invalidBankRollsBackWithoutPreferencesOrPartialFiles() throws Exception {
        store.banks();File source=new File(context.getCacheDir(),"invalid.sf2");Files.write(source.toPath(),new byte[]{1,2,3});NativeCheck.reject=true;
        try{store.importBank(Uri.fromFile(source));fail();}catch(IOException expected){assertEquals("Invalid SoundFont",expected.getMessage());}
        assertEquals(1,store.banks().size());assertEquals(1,new File(context.getFilesDir(),"soundfonts").listFiles().length);assertTrue(context.getSharedPreferences("soundfonts",0).getAll().isEmpty());assertTrue(source.exists());
    }
}
