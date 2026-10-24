package dev.pixelspc;
import android.net.Uri;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class ImportDocumentsTest {
    private static ImportDocuments.Child file(String id,String name){return new ImportDocuments.Child(id,name,false,10);}
    private static ImportDocuments.Child dir(String id,String name){return new ImportDocuments.Child(id,name,true,0);}
    private ImportDocuments.Tree tree(Map<String,List<ImportDocuments.Child>> folders){return new ImportDocuments.Tree(){public Uri uri(String id){return Uri.parse("content://test/"+id);}public List<ImportDocuments.Child> children(String id){return folders.getOrDefault(id,Collections.emptyList());}};}
    public static class Documents extends android.content.ContentProvider {
        @Override public boolean onCreate(){return true;}
        @Override public android.database.Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
            String id=android.provider.DocumentsContract.getDocumentId(uri);
            if(!uri.getLastPathSegment().equals("children")){android.database.MatrixCursor cursor=new android.database.MatrixCursor(new String[]{"_display_name"});cursor.addRow(new Object[]{"Game folder"});return cursor;}
            android.database.MatrixCursor cursor=new android.database.MatrixCursor(new String[]{"document_id","_display_name","mime_type","_size"});
            if(id.equals("root")){cursor.addRow(new Object[]{"root/game","Game","vnd.android.document/directory",null});cursor.addRow(new Object[]{"root/zip","Set.rsn","application/octet-stream",100});}
            else if(id.equals("root/game"))cursor.addRow(new Object[]{"root/game/track","One.spc","application/octet-stream",64});return cursor;
        }
        @Override public String getType(Uri uri){return null;}
        @Override public Uri insert(Uri uri,android.content.ContentValues values){throw new UnsupportedOperationException();}
        @Override public int update(Uri uri,android.content.ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}
        @Override public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
    }
    @Test public void folderProviderQueriesKeepTheGrantedTreeAndFolderName()throws Exception {
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("music.test",new Documents());Uri tree=Uri.parse("content://music.test/tree/root");
        android.content.Context context=org.robolectric.RuntimeEnvironment.getApplication();java.util.List<ImportDocuments.Item> found=ImportDocuments.folder(context,tree);
        assertEquals("Game folder",ImportDocuments.folderName(context,tree));assertEquals(2,found.size());assertEquals("Game/One.spc",found.get(0).path);
        assertEquals("root/game/track",android.provider.DocumentsContract.getDocumentId(found.get(0).uri));assertEquals("root",android.provider.DocumentsContract.getTreeDocumentId(found.get(0).uri));
    }
    @Test public void nestedMusicAndLibrariesKeepRelativePathsAndIgnoreOtherFiles()throws Exception{
        Map<String,List<ImportDocuments.Child>> map=new HashMap<>();map.put("root",Arrays.asList(file("a","Set.rsn"),dir("game","Game"),file("text","Readme.txt")));map.put("game",Arrays.asList(file("b","Track.minipsf"),file("c","Base.psflib")));
        List<ImportDocuments.Item> result=ImportDocuments.walk(tree(map),"root");assertEquals(3,result.size());assertEquals("Game/Base.psflib",result.get(0).path);assertTrue(result.get(0).dependency());assertEquals("Game/Track.minipsf",result.get(1).path);assertTrue(result.get(2).archive());
    }
    @Test public void cyclesAndUnsafeProviderNamesAreRejected()throws Exception{
        Map<String,List<ImportDocuments.Child>> map=new HashMap<>();map.put("root",Collections.singletonList(dir("root","Again")));
        try{ImportDocuments.walk(tree(map),"root");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("repeated"));}
        map.put("root",Collections.singletonList(file("bad","../escape.spc")));try{ImportDocuments.walk(tree(map),"root");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Invalid"));}
    }
    @Test public void directoryScanIsBounded()throws Exception{
        List<ImportDocuments.Child> files=new ArrayList<>();for(int i=0;i<1001;i++)files.add(file("f"+i,i+".spc"));Map<String,List<ImportDocuments.Child>> map=Collections.singletonMap("root",files);
        try{ImportDocuments.walk(tree(map),"root");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("1000"));}
    }
    @Test public void n64FolderScanIncludesTracksAndSharedLibraries()throws Exception{
        Map<String,List<ImportDocuments.Child>> map=new HashMap<>();map.put("root",Arrays.asList(file("mini","One.MINIUSF"),file("full","Two.USF"),file("lib","Base.USFLIB")));
        List<ImportDocuments.Item> items=ImportDocuments.walk(tree(map),"root");assertEquals(3,items.size());assertTrue(items.get(0).dependency());assertFalse(items.get(1).dependency());assertFalse(items.get(2).dependency());
    }
}
