package dev.pixelspc;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w411dp-h891dp-xhdpi",shadows=ImportFlowTest.Decoder.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ImportFlowTest {
    @Implements(value=Engine.class,isInAndroidSdk=false)
    public static class Decoder {
        @Implementation protected static void __staticInitializer__(){}
        @Implementation protected static long open(String path)throws IOException{if(Files.readAllBytes(Paths.get(path))[0]==127)throw new IOException("Invalid music");return 1;}
        @Implementation protected static void close(long handle){}
        @Implementation protected static int count(long handle){return 1;}
        @Implementation protected static String[] info(long handle,int index){return new String[]{"Test track","Fixture","Artist","Super Nintendo",""};}
        @Implementation protected static int length(long handle,int index){return 180000;}
    }
    private Context context;
    @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();new File(context.getFilesDir(),"library.json").delete();ArchiveImporter.removeTree(new File(context.getFilesDir(),"sets"));}
    private File source(String name,int value)throws Exception{File file=new File(context.getCacheDir(),name);Files.write(file.toPath(),new byte[]{(byte)value});return file;}
    private ImportDocuments.Item item(File file,String path){return new ImportDocuments.Item(Uri.fromFile(file),path,file.length());}
    private View find(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=find(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private void drain(ImportActivity activity)throws Exception{java.lang.reflect.Field f=ImportActivity.class.getDeclaredField("model");f.setAccessible(true);Object model=f.get(activity);f=model.getClass().getDeclaredField("task");f.setAccessible(true);Thread task=(Thread)f.get(model);if(task!=null){task.join(5000);assertFalse("Import worker stalled",task.isAlive());}org.robolectric.shadows.ShadowLooper.idleMainLooper();}
    @Test public void cancelAfterReviewDoesNotImportAndPickersSupportFilesAndFolders()throws Exception{
        try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
            ImportActivity a=c.get();View decor=a.getWindow().getDecorView();assertFalse(find(decor,"ADD").isEnabled());
            find(decor,"CHOOSE FILES").performClick();Intent pick=Shadows.shadowOf(a).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_OPEN_DOCUMENT,pick.getAction());assertTrue(pick.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            find(decor,"CHOOSE FOLDER").performClick();assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,Shadows.shadowOf(a).getNextStartedActivityForResult().intent.getAction());
            File file=source("First.spc",1);a.onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(file)));drain(a);assertTrue(find(decor,"ADD").isEnabled());
            find(decor,"CANCEL").performClick();assertEquals(Activity.RESULT_CANCELED,Shadows.shadowOf(a).getResultCode());assertTrue(new Library(context).games.isEmpty());assertTrue(file.exists());
        }
    }
    @Test public void reviewedMultiSelectionSurvivesRotationAndReturnsAddAndQueue()throws Exception{
        try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
            File first=source("First.spc",1),second=source("Second.mid",2);ClipData clip=ClipData.newRawUri("music",Uri.fromFile(first));clip.addItem(new ClipData.Item(Uri.fromFile(second)));
            Intent picked=new Intent();picked.setClipData(clip);c.get().onActivityResult(1,Activity.RESULT_OK,picked);drain(c.get());c.recreate();
            View decor=c.get().getWindow().getDecorView();assertNotNull(find(decor,"2 files selected"));
            decor.measure(View.MeasureSpec.makeMeasureSpec(822,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1782,View.MeasureSpec.EXACTLY));decor.layout(0,0,822,1782);
            android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(822,1782,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(image));try(OutputStream out=new FileOutputStream("../docs/screenshots/import-music.png")){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            find(decor,"ADD AND QUEUE").performClick();drain(c.get());assertEquals(Activity.RESULT_OK,Shadows.shadowOf(c.get()).getResultCode());Intent result=Shadows.shadowOf(c.get()).getResultIntent();assertTrue(result.getBooleanExtra(ImportActivity.QUEUE,false));assertEquals(1,result.getStringArrayListExtra(ImportActivity.IDS).size());
            assertEquals(2,new Library(context).games.get(0).tracks.size());assertTrue(first.exists());assertTrue(second.exists());
        }
    }
    @Test public void addReturnsLibraryOnlyAction()throws Exception{
        try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(source("Only.spc",1))));drain(c.get());find(c.get().getWindow().getDecorView(),"ADD").performClick();drain(c.get());assertFalse(Shadows.shadowOf(c.get()).getResultIntent().getBooleanExtra(ImportActivity.QUEUE,true));assertEquals(1,new Library(context).games.size());
        }
    }
    @Test public void failedBatchRollsBackEarlierImportsAndKeepsSourceFiles()throws Exception{
        File good=source("Good.spc",1),archive=new File(context.getCacheDir(),"Bad.zip");try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){zip.putNextEntry(new ZipEntry("bad.spc"));zip.write(127);zip.closeEntry();}
        List<ImportDocuments.Item> items=Arrays.asList(item(good,"Good.spc"),item(archive,"Bad.zip"));Set<String> selected=new HashSet<>();for(ImportDocuments.Item i:items)selected.add(i.key());
        try{ImportBatch.run(context,items,selected,"Test");fail();}catch(IOException expected){assertEquals("Invalid music",expected.getMessage());}
        assertTrue(new Library(context).games.isEmpty());assertEquals(0,new File(context.getFilesDir(),"sets").listFiles().length);assertTrue(good.exists());assertTrue(archive.exists());
    }
    @Test public void psfDirectoryImportPreservesNestedDependenciesAndHidesLibraries()throws Exception{
        File track=source("One.minipsf",1),lib=source("Base.psflib",2);List<ImportDocuments.Item> items=Arrays.asList(item(track,"Game/Tracks/One.minipsf"),item(lib,"Game/Libraries/Base.psflib"));
        List<Library.Game> added=ImportBatch.run(context,items,Collections.singleton(items.get(0).key()),"PSF folder");assertEquals(1,added.get(0).tracks.size());File root=new File(context.getFilesDir(),"sets/"+added.get(0).id+"/psf");assertTrue(new File(root,"game/tracks/one.minipsf").isFile());assertTrue(new File(root,"game/libraries/base.psflib").isFile());
    }
}
