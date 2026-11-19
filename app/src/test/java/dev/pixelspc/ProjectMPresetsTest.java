package dev.pixelspc;

import android.content.Context;
import android.graphics.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ProjectMPresetsTest {
    private ProjectMPresets store;private Context context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();remove(new File(context.getFilesDir(),"projectm"));store=new ProjectMPresets(context);}
    private static void remove(File f){File[] children=f.listFiles();if(children!=null)for(File c:children)remove(c);f.delete();}
    private static byte[] preset(){return "[preset00]\nzoom=1.01\nwave_r=1\n".getBytes(StandardCharsets.UTF_8);}
    private static byte[] image(){Bitmap b=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);b.eraseColor(Color.RED);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();return out.toByteArray();}
    private static byte[] zip(String[] paths,byte[][] data)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){for(int i=0;i<paths.length;i++){z.putNextEntry(new ZipEntry(paths[i]));z.write(data[i]);z.closeEntry();}}return out.toByteArray();}
    private void rejected(byte[] bytes,String name)throws Exception{try{store.importStream(new ByteArrayInputStream(bytes),name);fail("Expected invalid pack rejection");}catch(IOException expected){}assertEquals(3,store.all().size());File[] partial=new File(context.getFilesDir(),"projectm").listFiles((d,n)->n.startsWith(".import"));assertEquals(0,partial.length);}
    @Test public void importsNestedPresetsAndSharedTextureOnlyPacks()throws Exception{
        assertEquals(3,store.all().size());
        ProjectMPresets.Result p=store.importStream(new ByteArrayInputStream(zip(new String[]{"Pack/One.MILK","Pack/sub/Two.milk","Pack/README.md"},new byte[][]{preset(),preset(),"not instructions".getBytes()})),"pack.zip");assertEquals(2,p.presets);assertEquals(0,p.textures);
        ProjectMPresets.Result t=store.importStream(new ByteArrayInputStream(zip(new String[]{"Textures/Red.PNG"},new byte[][]{image()})),"textures.zip");assertEquals(0,t.presets);assertEquals(1,t.textures);
        List<ProjectMPresets.Preset> all=store.all();assertEquals(5,all.size());ProjectMPresets.Preset one=all.stream().filter(x->x.name.equals("One")).findFirst().get();assertTrue(Files.readAllBytes(one.file.toPath()).length>0);assertEquals(2,store.texturePaths(one).length);assertEquals(5,new ProjectMPresets(context).all().size());
    }
    @Test public void duplicateNamesInDifferentPacksHaveDistinctStableIds()throws Exception{
        for(int i=0;i<2;i++)store.importStream(new ByteArrayInputStream(preset()),"Same.milk");List<ProjectMPresets.Preset> all=store.all();assertEquals(5,all.size());Set<String> ids=new HashSet<>();for(ProjectMPresets.Preset p:all)assertTrue(ids.add(p.id));
    }
    @Test public void failedPackRollsBackAlreadyExtractedPresets()throws Exception{rejected(zip(new String[]{"Good.milk","Bad.milk"},new byte[][]{preset(),"garbage".getBytes()}),"pack.zip");}
    @Test public void pathsCannotEscapePrivateStorage()throws Exception{rejected(zip(new String[]{"Good.milk","../escape.milk"},new byte[][]{preset(),preset()}),"pack.zip");assertFalse(new File(context.getFilesDir(),"projectm/escape.milk").exists());}
    @Test public void caseCollisionsAndInvalidTexturesAreRejected()throws Exception{rejected(zip(new String[]{"A.milk","a.MILK"},new byte[][]{preset(),preset()}),"pack.zip");rejected(zip(new String[]{"bad.png"},new byte[][]{preset()}),"textures.zip");}
    @Test public void oversizedPresetAndNonPresetInputsAreRejected()throws Exception{rejected(new byte[2*1024*1024+1],"Large.milk");rejected(preset(),"readme.txt");rejected(zip(new String[]{"README.md"},new byte[][]{preset()}),"pack.zip");}
    @Test public void suppliedPacksImportWithoutBundlingTheirFiles()throws Exception{
        String textures=System.getProperty("projectm.test.textures"),presets=System.getProperty("projectm.test.presets");Assume.assumeNotNull(textures,presets);
        ProjectMPresets.Result t=store.importStream(new FileInputStream(textures),"textures.zip");assertEquals(67,t.textures);assertEquals(0,t.presets);
        ProjectMPresets.Result p=store.importStream(new FileInputStream(presets),"presets.zip");assertEquals(3996,p.presets);assertEquals(0,p.textures);assertEquals(3999,store.all().size());
    }
}
