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
        static volatile java.util.concurrent.CountDownLatch blocked,proceed;
        @Implementation protected static void __staticInitializer__(){}
        @Implementation protected static long open(String path)throws IOException{if(blocked!=null){blocked.countDown();try{if(!proceed.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IOException("Test import timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}if(Files.readAllBytes(Paths.get(path))[0]==127)throw new IOException("Invalid music");return Math.max(1,Files.readAllBytes(Paths.get(path))[0]);}
        @Implementation protected static void close(long handle){}
        @Implementation protected static int count(long handle){return handle>=10&&handle<=12?(int)handle-8:1;}
        @Implementation protected static String[] info(long handle,int index){return handle>=10&&handle<=12?new String[]{"Song "+handle+"-"+index,"NES Game "+handle,"NES Composer","Nintendo NES",""}:new String[]{"Test track","Fixture","Artist","Super Nintendo",""};}
        @Implementation protected static int length(long handle,int index){return 180000;}
    }
    private Context context;
    @Before public void setup()throws Exception{Decoder.blocked=null;Decoder.proceed=null;context=RuntimeEnvironment.getApplication();new File(context.getFilesDir(),"library.json").delete();ArchiveImporter.removeTree(new File(context.getFilesDir(),"sets"));}
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
    @Test public void multipleNsfeFilesKeepTheirOwnGamesForAddAndQueueAfterRotation()throws Exception{
        for(boolean queue:new boolean[]{false,true}){
            new File(context.getFilesDir(),"library.json").delete();ArchiveImporter.removeTree(new File(context.getFilesDir(),"sets"));
            try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
                ClipData clip=ClipData.newRawUri("NES sets",Uri.fromFile(source("One.nsfe",10)));clip.addItem(new ClipData.Item(Uri.fromFile(source("Two.NSFE",11))));clip.addItem(new ClipData.Item(Uri.fromFile(source("Three.nsfe",12))));Intent picked=new Intent();picked.setClipData(clip);
                c.get().onActivityResult(1,Activity.RESULT_OK,picked);drain(c.get());c.recreate();find(c.get().getWindow().getDecorView(),queue?"ADD AND QUEUE":"ADD").performClick();drain(c.get());
                Intent result=Shadows.shadowOf(c.get()).getResultIntent();assertEquals(Activity.RESULT_OK,Shadows.shadowOf(c.get()).getResultCode());assertEquals(queue,result.getBooleanExtra(ImportActivity.QUEUE,false));assertEquals(3,result.getStringArrayListExtra(ImportActivity.IDS).size());
                List<Library.Game> games=new Library(context).games;assertEquals(3,games.size());Set<String> ids=new HashSet<>();
                for(int i=0;i<3;i++){Library.Game game=games.get(i);assertTrue(ids.add(game.id));assertEquals("NES Game "+(10+i),game.name);assertEquals(i+2,game.tracks.size());for(int n=0;n<game.tracks.size();n++){Library.Track track=game.tracks.get(n);assertEquals("Song "+(10+i)+"-"+n,track.title);assertEquals(n,track.index);assertEquals(game.name,track.game);assertTrue(track.path.contains(game.id));}}
            }
        }
    }
    @Test public void nsfSetsStaySeparateBesideGroupedLooseTracksAndFolderPaths()throws Exception{
        List<ImportDocuments.Item> items=Arrays.asList(item(source("SameA.nsf",10),"First/Same.nsf"),item(source("SameB.nsf",11),"Second/Same.nsf"),item(source("One.spc",1),"Loose/One.spc"),item(source("Two.spc",1),"Loose/Two.spc"));Set<String> chosen=new HashSet<>();for(ImportDocuments.Item item:items)chosen.add(item.key());
        List<Library.Game> games=ImportBatch.run(context,items,chosen,"Folder");assertEquals(3,games.size());assertEquals(2,games.get(0).tracks.size());assertEquals("NES Game 10",games.get(1).name);assertEquals(2,games.get(1).tracks.size());assertEquals("NES Game 11",games.get(2).name);assertEquals(3,games.get(2).tracks.size());
    }
    @Test public void failedNsfeBatchRollsBackEverySeparateSet()throws Exception{
        File one=source("Good.nsfe",10),bad=source("Bad.nsfe",127);List<ImportDocuments.Item> items=Arrays.asList(item(one,"Good.nsfe"),item(bad,"Bad.nsfe"));Set<String> chosen=new HashSet<>();for(ImportDocuments.Item item:items)chosen.add(item.key());
        try{ImportBatch.run(context,items,chosen,"NES sets");fail();}catch(IOException expected){assertEquals("Invalid music",expected.getMessage());}
        assertTrue(new Library(context).games.isEmpty());assertEquals(0,new File(context.getFilesDir(),"sets").listFiles().length);assertTrue(one.exists());assertTrue(bad.exists());
    }
    @Test public void addReturnsLibraryOnlyAction()throws Exception{
        try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(source("Only.spc",1))));drain(c.get());find(c.get().getWindow().getDecorView(),"ADD").performClick();drain(c.get());assertFalse(Shadows.shadowOf(c.get()).getResultIntent().getBooleanExtra(ImportActivity.QUEUE,true));assertEquals(1,new Library(context).games.size());
        }
    }
    @Test public void reportingCopiesBytesExactlyWithoutDuplicatingWrites()throws Exception{
        byte[] data=new byte[65536];new java.util.Random(42).nextBytes(data);ByteArrayOutputStream out=new ByteArrayOutputStream();List<String> messages=new ArrayList<>();OutputStream reporting=ImportProgress.reporting(out,messages::add,"Copying · Game.zip");reporting.write(data);reporting.write(7);reporting.flush();byte[] expected=Arrays.copyOf(data,data.length+1);expected[data.length]=7;assertArrayEquals(expected,out.toByteArray());assertTrue(messages.get(0).startsWith("Copying · Game.zip · "));
    }
    @Test public void progressReportsArchivesEntriesSavingAndRollback()throws Exception{
        File one=new File(context.getCacheDir(),"NES.zip"),two=new File(context.getCacheDir(),"GameBoy.zip");
        for(File archive:Arrays.asList(one,two))try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){zip.putNextEntry(new ZipEntry(archive==one?"Mario.nsf":"Tetris.gbs"));zip.write(1);zip.closeEntry();}
        List<ImportDocuments.Item> items=Arrays.asList(item(one,"NES.zip"),item(two,"GameBoy.zip"));Set<String> selected=new HashSet<>();for(ImportDocuments.Item i:items)selected.add(i.key());List<String> messages=new ArrayList<>();
        assertEquals(2,ImportBatch.run(context,items,selected,"Games",messages::add).size());
        assertTrue(messages.stream().anyMatch(m->m.contains("1/2 · Unpacking · NES.zip")&&m.contains("Unpacking")&&m.contains("Mario.nsf")));
        assertTrue(messages.stream().anyMatch(m->m.contains("2/2 · Reading track 1/1 · GameBoy.zip")&&m.contains("Reading track 1/1")&&m.contains("Tetris.gbs")));
        assertTrue(messages.stream().anyMatch(m->m.contains("Saving 1 track")));
        File bad=new File(context.getCacheDir(),"Broken.zip");try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(bad))){zip.putNextEntry(new ZipEntry("bad.spc"));zip.write(127);zip.closeEntry();}
        ImportDocuments.Item broken=item(bad,"Broken.zip");messages.clear();try{ImportBatch.run(context,Collections.singletonList(broken),Collections.singleton(broken.key()),"Broken",messages::add);fail();}catch(IOException expected){}
        assertTrue(messages.contains("Undoing incomplete import…"));assertEquals(2,new Library(context).games.size());
    }
    @Test public void liveImportStatusUpdatesInOneLineAndSurvivesRotation()throws Exception{
        File archive=new File(context.getCacheDir(),"Live.zip");try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){for(String name:new String[]{"A.spc","B.gbs"}){zip.putNextEntry(new ZipEntry(name));zip.write(1);zip.closeEntry();}}
        try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(archive)));drain(c.get());Decoder.blocked=new java.util.concurrent.CountDownLatch(1);Decoder.proceed=new java.util.concurrent.CountDownLatch(1);
            try{
                find(c.get().getWindow().getDecorView(),"ADD").performClick();assertTrue(Decoder.blocked.await(5,java.util.concurrent.TimeUnit.SECONDS));Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));
                TextView status=org.robolectric.util.ReflectionHelpers.getField(c.get(),"count");assertTrue(status.getText().toString().contains("Live.zip"));assertTrue(status.getText().toString().contains("Reading track 1/2"));assertEquals(1,status.getMaxLines());
                String message=status.getText().toString();c.recreate();status=org.robolectric.util.ReflectionHelpers.getField(c.get(),"count");assertEquals(message,status.getText().toString());assertEquals(1,status.getMaxLines());
                View decor=c.get().getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(822,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1782,View.MeasureSpec.EXACTLY));decor.layout(0,0,822,1782);android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(822,1782,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(image));try(OutputStream out=new FileOutputStream("../docs/screenshots/import-progress.png")){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            }finally{Decoder.proceed.countDown();drain(c.get());Decoder.blocked=null;Decoder.proceed=null;}
            assertEquals(Activity.RESULT_OK,Shadows.shadowOf(c.get()).getResultCode());assertEquals(2,new Library(context).games.get(0).tracks.size());
        }
    }
    @Test public void failedBatchRollsBackEarlierImportsAndKeepsSourceFiles()throws Exception{
        File good=source("Good.spc",1),archive=new File(context.getCacheDir(),"Bad.zip");try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){zip.putNextEntry(new ZipEntry("bad.spc"));zip.write(127);zip.closeEntry();}
        List<ImportDocuments.Item> items=Arrays.asList(item(good,"Good.spc"),item(archive,"Bad.zip"));Set<String> selected=new HashSet<>();for(ImportDocuments.Item i:items)selected.add(i.key());
        try{ImportBatch.run(context,items,selected,"Test");fail();}catch(IOException expected){assertEquals("Invalid music",expected.getMessage());}
        assertTrue(new Library(context).games.isEmpty());assertEquals(0,new File(context.getFilesDir(),"sets").listFiles().length);assertTrue(good.exists());assertTrue(archive.exists());
    }
    @Test public void midiTitlesUseOriginalFilenamesInsteadOfInstrumentMetadata() throws Exception {
        File single=source("Race Theme.MID",1);Library library=new Library(context);Library.Game direct=library.importFile(Uri.fromFile(single),single.getName());assertEquals("Race Theme",direct.tracks.get(0).title);
        File archive=new File(context.getCacheDir(),"Midi set.zip");try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){for(String name:new String[]{"nested/Opening.midi","Another.mid","Track.spc"}){zip.putNextEntry(new ZipEntry(name));zip.write(1);zip.closeEntry();}}
        Library.Game set=library.importFile(Uri.fromFile(archive),archive.getName());assertEquals("Another",set.tracks.get(0).title);assertEquals("Opening",set.tracks.get(1).title);assertEquals("Test track",set.tracks.get(2).title);
        Library restored=new Library(context);assertEquals("Race Theme",restored.games.get(0).tracks.get(0).title);assertEquals("Opening",restored.games.get(1).tracks.get(1).title);
    }
    @Test public void psfDirectoryImportPreservesNestedDependenciesAndHidesLibraries()throws Exception{
        File track=source("One.minipsf",1),lib=source("Base.psflib",2);List<ImportDocuments.Item> items=Arrays.asList(item(track,"Game/Tracks/One.minipsf"),item(lib,"Game/Libraries/Base.psflib"));
        List<Library.Game> added=ImportBatch.run(context,items,Collections.singleton(items.get(0).key()),"PSF folder");assertEquals(1,added.get(0).tracks.size());File root=new File(context.getFilesDir(),"sets/"+added.get(0).id+"/psf");assertTrue(new File(root,"game/tracks/one.minipsf").isFile());assertTrue(new File(root,"game/libraries/base.psflib").isFile());
    }
    @Test public void miniUsfSelectionAutomaticallyIncludesUnselectedCompanionLibraries()throws Exception{
        File track=source("One.MINIUSF",1),lib=source("Base.USFLIB",2);List<ImportDocuments.Item> items=Arrays.asList(item(track,"Game/Tracks/One.MINIUSF"),item(lib,"Game/Libraries/Base.USFLIB"));
        List<Library.Game> added=ImportBatch.run(context,items,Collections.singleton(items.get(0).key()),"N64 folder");assertEquals(1,added.get(0).tracks.size());File root=new File(context.getFilesDir(),"sets/"+added.get(0).id+"/psf");assertTrue(new File(root,"game/tracks/one.miniusf").isFile());assertTrue(new File(root,"game/libraries/base.usflib").isFile());
    }
}
