package dev.pixelspc;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import android.net.Uri;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
/** Optional end-to-end test against private archives and the host JNI library. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class ArcadeBatchNativeIntegrationTest {
 @Test public void privateArcadeZipsStaySeparateThroughBatchAndReload()throws Exception{
  String sets=System.getProperty("arcade.test.sets","");Assume.assumeFalse(sets.isEmpty());Context context=RuntimeEnvironment.getApplication();
  new File(context.getFilesDir(),"library.json").delete();List<ImportDocuments.Item> items=new ArrayList<>();Set<String> chosen=new HashSet<>();
  for(String path:sets.split(";")){File file=new File(path);ImportDocuments.Item item=new ImportDocuments.Item(Uri.fromFile(file),file.getName(),file.length());items.add(item);chosen.add(item.key());}
  List<Library.Game> games=ImportBatch.run(context,items,chosen,"Multiple arcade ZIPs");assertEquals(items.size(),games.size());Set<String> names=new HashSet<>(),ids=new HashSet<>();int total=0;
  for(Library.Game game:games){System.out.println(game.name+" | "+game.tracks.size()+" tracks | "+game.id);names.add(game.name);ids.add(game.id);total+=game.tracks.size();}
  assertEquals(3,names.size());assertEquals(3,ids.size());assertEquals(164,total);assertEquals(3,new Library(context).games.size());
  new File(context.getFilesDir(),"library.json").delete();ArchiveImporter.removeTree(new File(context.getFilesDir(),"sets"));
  try(org.robolectric.android.controller.ActivityController<ImportActivity> c=Robolectric.buildActivity(ImportActivity.class).setup()){
   ClipData clip=ClipData.newRawUri("Arcade ZIPs",items.get(0).uri);for(int i=1;i<items.size();i++)clip.addItem(new ClipData.Item(items.get(i).uri));
   Intent picked=new Intent();picked.setClipData(clip);c.get().onActivityResult(1,Activity.RESULT_OK,picked);drain(c.get());find(c.get().getWindow().getDecorView(),"ADD").performClick();drain(c.get());
   Intent result=Shadows.shadowOf(c.get()).getResultIntent();assertEquals(Activity.RESULT_OK,Shadows.shadowOf(c.get()).getResultCode());assertEquals(3,result.getStringArrayListExtra(ImportActivity.IDS).size());
   Library reloaded=new Library(context);assertEquals(3,reloaded.games.size());assertEquals(3,new HashSet<String>(Arrays.asList(reloaded.games.get(0).name,reloaded.games.get(1).name,reloaded.games.get(2).name)).size());
  }
  new File(context.getFilesDir(),"library.json").delete();ArchiveImporter.removeTree(new File(context.getFilesDir(),"sets"));items.clear();chosen.clear();
  for(int i=0;i<3;i++){File file=new File(context.getCacheDir(),"NES-"+i+".nsfe");Files.write(file.toPath(),nsfe(i));ImportDocuments.Item item=new ImportDocuments.Item(Uri.fromFile(file),file.getName(),file.length());items.add(item);chosen.add(item.key());}
  games=ImportBatch.run(context,items,chosen,"NES sets");assertEquals(3,games.size());
  for(int i=0;i<3;i++){Library.Game game=games.get(i);assertEquals("NES Fixture "+i,game.name);assertEquals(2,game.tracks.size());assertEquals("Boss "+i,game.tracks.get(0).title);assertEquals("Menu "+i,game.tracks.get(1).title);assertEquals(2000,game.tracks.get(0).length);assertEquals(1000,game.tracks.get(1).length);}
  assertEquals(3,new Library(context).games.size());

 }
 private void number(ByteArrayOutputStream out,int n){for(int i=0;i<4;i++)out.write(n>>>(i*8));}
 private void chunk(ByteArrayOutputStream out,String name,byte[] data)throws Exception{number(out,data.length);out.write(name.getBytes("US-ASCII"));out.write(data);}
 private byte[] nsfe(int game)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("NSFE".getBytes("US-ASCII"));chunk(out,"INFO",new byte[]{0,(byte)0x80,0,(byte)0x80,1,(byte)0x80,0,0,2,0});chunk(out,"DATA",new byte[]{0x60,0x60});
  chunk(out,"auth",("NES Fixture "+game+"\0Composer\0Original\0Test\0").getBytes("UTF-8"));chunk(out,"tlbl",("Menu "+game+"\0Boss "+game+"\0").getBytes("UTF-8"));chunk(out,"plst",new byte[]{1,0});ByteArrayOutputStream times=new ByteArrayOutputStream();number(times,1000);number(times,2000);chunk(out,"time",times.toByteArray());chunk(out,"NEND",new byte[0]);return out.toByteArray();
 }
 private View find(View view,String label){if(view instanceof TextView&&label.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=find(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}return null;}
 private void drain(ImportActivity activity)throws Exception{Object model=org.robolectric.util.ReflectionHelpers.getField(activity,"model");Thread task=org.robolectric.util.ReflectionHelpers.getField(model,"task");if(task!=null){task.join(30000);assertFalse(task.isAlive());}org.robolectric.shadows.ShadowLooper.idleMainLooper();}
}
