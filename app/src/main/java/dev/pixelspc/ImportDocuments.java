package dev.pixelspc;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.*;

/** A bounded view of only the files/folder granted by Android's document picker. */
final class ImportDocuments {
    static final int ENTRY_LIMIT=3000,FILE_LIMIT=1000,DEPTH_LIMIT=16;
    static final class Item {
        final Uri uri; final String path; final long size;
        Item(Uri uri,String path,long size){this.uri=uri;this.path=path;this.size=size;}
        boolean dependency(){return ArchiveImporter.dependency(path);}
        boolean archive(){String ext=ArchiveImporter.extension(path);return ext.equals("zip")||ext.equals("rsn")||ext.equals("rar");}
        String key(){return uri.toString();}
    }
    static final class Child {
        final String id,name;final boolean directory;final long size;
        Child(String id,String name,boolean directory,long size){this.id=id;this.name=name;this.directory=directory;this.size=size;}
    }
    interface Tree {List<Child> children(String id)throws Exception;Uri uri(String id);}
    static boolean supported(String name){return name!=null&&(ArchiveImporter.supported(name)||ArchiveImporter.dependency(name)||Arrays.asList("zip","rsn","rar").contains(ArchiveImporter.extension(name)));}
    static void checkCancelled()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Import cancelled");}
    static String component(String name)throws IOException{
        if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.contains("/")||name.contains("\\")||name.contains(":" )||name.indexOf('\0')>=0||name.length()>240)throw new IOException("Invalid document name");return name;
    }
    static List<Item> walk(Tree tree,String root)throws Exception{
        List<Item> result=new ArrayList<>();walk(tree,root,"",0,new HashSet<>(),new int[]{0,0},result);result.sort(Comparator.comparing(i->i.path,String.CASE_INSENSITIVE_ORDER));return result;
    }
    private static void walk(Tree tree,String id,String prefix,int depth,Set<String> seen,int[] count,List<Item> result)throws Exception{
        checkCancelled();if(depth>DEPTH_LIMIT)throw new IOException("Folder nesting exceeds 16 levels. Choose a smaller folder.");if(!seen.add(id))throw new IOException("Folder contains a repeated directory");
        for(Child child:tree.children(id)){
            checkCancelled();if(++count[0]>ENTRY_LIMIT)throw new IOException("Folder exceeds 3000 entries. Choose a smaller folder.");String path=prefix+component(child.name);
            if(path.length()>1000)throw new IOException("Folder path is too long");
            if(child.directory)walk(tree,child.id,path+"/",depth+1,seen,count,result);
            else if(supported(child.name)){if(!ArchiveImporter.dependency(path)&&++count[1]>FILE_LIMIT)throw new IOException("Select a folder with at most 1000 music files");result.add(new Item(tree.uri(child.id),path,child.size));}
        }
    }
    static String folderName(Context context,Uri root) {
        Uri document=DocumentsContract.buildDocumentUriUsingTree(root,DocumentsContract.getTreeDocumentId(root));
        try(Cursor cursor=context.getContentResolver().query(document,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) {
            if(cursor!=null&&cursor.moveToFirst()&&cursor.getString(0)!=null)return cursor.getString(0);
        }catch(RuntimeException ignored){/* The folder listing may still be readable. */}
        return "Selected folder";
    }
    static List<Item> folder(Context context,Uri root)throws Exception{
        ContentResolver resolver=context.getContentResolver();String rootId=DocumentsContract.getTreeDocumentId(root);
        return walk(new Tree(){
            public Uri uri(String id){return DocumentsContract.buildDocumentUriUsingTree(root,id);}
            public List<Child> children(String id)throws Exception{
                List<Child> result=new ArrayList<>();Uri listing=DocumentsContract.buildChildDocumentsUriUsingTree(root,id);
                try(Cursor cursor=resolver.query(listing,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE},null,null,null)){
                    if(cursor==null)throw new IOException("Cannot read selected folder");while(cursor.moveToNext()){checkCancelled();if(result.size()>=ENTRY_LIMIT)throw new IOException("Folder exceeds 3000 entries");result.add(new Child(cursor.getString(0),cursor.getString(1),DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2)),cursor.isNull(3)?-1:cursor.getLong(3)));}
                }return result;
            }
        },rootId);
    }
    static List<Item> files(Context context,List<Uri> uris)throws Exception{
        if(uris.size()>FILE_LIMIT)throw new IOException("Choose at most 1000 music files");List<Item> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Uri uri:uris){checkCancelled();if(!seen.add(uri.toString()))continue;String name=uri.getLastPathSegment();long size=-1;
            try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){name=cursor.getString(0);if(!cursor.isNull(1))size=cursor.getLong(1);}}
            if(!supported(name))continue;result.add(new Item(uri,component(name),size));
        }return result;
    }
}
