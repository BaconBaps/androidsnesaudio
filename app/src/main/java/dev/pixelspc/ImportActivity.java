package dev.pixelspc;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Themed selection/review; Android grants access, this screen owns the import actions. */
public class ImportActivity extends Activity {
    static final String QUEUE="appendQueue",IDS="importedIds";
    private Model model;private PlayerTheme theme;private PlayerSkin skin;private Typeface pixel;
    private LinearLayout rows;private TextView count,error,add,queue,cancel,files,folder;private CheckBox subfolders,all;
    private ProgressBar progress;private boolean updating;
    private static final class Model {
        final Context context;final Handler main=new Handler(Looper.getMainLooper());
        volatile ImportActivity owner;volatile boolean scanning,importing;Thread task;
        List<ImportDocuments.Item> items=new ArrayList<>();Set<String> selected=new HashSet<>();String label="Selected music",error="";
        boolean folder,includeSubfolders=true,append;ArrayList<String> result;
        Model(Context context){this.context=context.getApplicationContext();}
        void update(){main.post(()->{if(owner!=null&&!owner.isDestroyed())owner.refresh();});}
        void scan(List<Uri> uris,Uri tree){
            scanning=true;error="";items=new ArrayList<>();selected=new HashSet<>();folder=tree!=null;update();task=new Thread(()->{
                try {
                    List<ImportDocuments.Item> found=tree==null?ImportDocuments.files(context,uris):ImportDocuments.folder(context,tree);
                    Set<String> chosen=new HashSet<>();for(ImportDocuments.Item item:found)if(!item.dependency())chosen.add(item.key());
                    ImportDocuments.checkCancelled();items=found;selected=chosen;folder=tree!=null;includeSubfolders=true;label=folder?ImportDocuments.folderName(context,tree):found.size()==1?found.get(0).path.replaceFirst("\\.[^.]+$",""):"Selected music";
                    if(chosen.isEmpty())error="No supported music found. Choose tracks, ZIP or RSN sets. PSF/USF libraries are included with their tracks.";
                }catch(Exception e){if(!Thread.currentThread().isInterrupted())error=e.getMessage()==null?"Could not read selected documents":e.getMessage();}
                finally{scanning=false;update();}
            },"Browse music");task.start();
        }
        void commit(boolean appendQueue,Set<String> selection){
            importing=true;append=appendQueue;error="";update();task=new Thread(()->{
                try{List<Library.Game> games=ImportBatch.run(context,items,selection,label);ArrayList<String> ids=new ArrayList<>();for(Library.Game game:games)ids.add(game.id);result=ids;}
                catch(Exception e){error="Could not add music: "+e.getMessage();}
                finally{importing=false;update();}
            },"Add music");task.start();
        }
    }
    int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView label(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);return v;}
    private TextView button(String text){TextView v=label(text,8,theme.buttonText(theme.accent));v.setTypeface(pixel);v.setGravity(Gravity.CENTER);v.setPadding(dp(6),0,dp(6),0);v.setBackground(skin.button(theme.buttonFace(theme.surface)));v.setContentDescription(text);return v;}
    private void pair(LinearLayout row,View left,View right){LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(46),1);a.setMargins(0,0,dp(8),0);row.addView(left,a);row.addView(right,new LinearLayout.LayoutParams(0,dp(46),1));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);model=(Model)getLastNonConfigurationInstance();if(model==null)model=new Model(this);model.owner=this;
        theme=ThemeStore.selected(this);skin=new PlayerSkin(this);pixel=Typeface.createFromAsset(getAssets(),"fonts/pixel.ttf");
        getWindow().setStatusBarColor(theme.background);getWindow().setNavigationBarColor(theme.background);getWindow().getDecorView().setSystemUiVisibility(theme.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout body=column();body.setPadding(dp(16),dp(14),dp(16),dp(14));body.setBackgroundColor(theme.background);setContentView(body);
        body.setOnApplyWindowInsetsListener((v,insets)->{body.setPadding(dp(16),dp(14)+insets.getSystemWindowInsetTop(),dp(16),dp(14)+insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        TextView heading=label("IMPORT MUSIC",16,theme.accent);heading.setTypeface(pixel);heading.setGravity(Gravity.CENTER_VERTICAL);body.addView(heading,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView hint=label("Choose files or a folder, then review your selection. Your originals stay on your device.",13,theme.muted);body.addView(hint,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout sources=new LinearLayout(this);sources.setPadding(0,dp(12),0,dp(8));files=button("CHOOSE FILES");folder=button("CHOOSE FOLDER");pair(sources,files,folder);body.addView(sources);
        files.setOnClickListener(v->pickFiles());folder.setOnClickListener(v->pickFolder());
        subfolders=new CheckBox(this);subfolders.setText(R.string.import_subfolders);subfolders.setTextColor(theme.text);subfolders.setButtonTintList(android.content.res.ColorStateList.valueOf(theme.accent));body.addView(subfolders,new LinearLayout.LayoutParams(-1,dp(42)));
        subfolders.setOnCheckedChangeListener((v,checked)->{if(!updating){model.includeSubfolders=checked;renderRows();}});
        all=new CheckBox(this);all.setText(R.string.import_select_all);all.setTextColor(theme.text);all.setButtonTintList(android.content.res.ColorStateList.valueOf(theme.accent));body.addView(all,new LinearLayout.LayoutParams(-1,dp(42)));
        all.setOnCheckedChangeListener((v,checked)->{if(updating)return;for(ImportDocuments.Item item:visible())if(checked)model.selected.add(item.key());else model.selected.remove(item.key());renderRows();});
        FrameLayout frame=new FrameLayout(this);frame.setBackground(skin.well(theme.library().background));frame.setPadding(dp(6),dp(5),dp(6),dp(5));body.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));rows=column();scroll.addView(rows);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);body.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        count=label("",13,theme.text);count.setPadding(0,dp(8),0,dp(4));count.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(count);
        error=label("",13,theme.muted);error.setPadding(0,0,0,dp(8));error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(error);
        LinearLayout actions=new LinearLayout(this);add=button("ADD");queue=button("ADD AND QUEUE");pair(actions,add,queue);body.addView(actions);
        cancel=button("CANCEL");LinearLayout.LayoutParams cancelParams=new LinearLayout.LayoutParams(-1,dp(42));cancelParams.topMargin=dp(8);body.addView(cancel,cancelParams);
        cancel.setOnClickListener(v->cancel());add.setOnClickListener(v->commit(false));queue.setOnClickListener(v->commit(true));refresh();
    }
    private List<ImportDocuments.Item> visible(){List<ImportDocuments.Item> result=new ArrayList<>();for(ImportDocuments.Item item:model.items)if(!item.dependency()&&(!model.folder||model.includeSubfolders||!item.path.contains("/")))result.add(item);return result;}
    private void refresh(){
        if(model.result!=null){setResult(RESULT_OK,new Intent().putStringArrayListExtra(IDS,model.result).putExtra(QUEUE,model.append));finish();return;}
        updating=true;subfolders.setVisibility(model.folder?View.VISIBLE:View.GONE);subfolders.setChecked(model.includeSubfolders);updating=false;renderRows();
    }
    private void renderRows(){
        rows.removeAllViews();List<ImportDocuments.Item> visible=visible();
        if(visible.isEmpty()){TextView empty=label("No files selected\n\nSPC · RSN · ZIP · PSF · USF · MIDI\nand other supported chiptunes",14,theme.muted);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(14),dp(28),dp(14),dp(28));rows.addView(empty,new LinearLayout.LayoutParams(-1,-2));}
        for(ImportDocuments.Item item:visible){
            CheckBox choice=new CheckBox(this);choice.setText(item.size>=0?getString(R.string.import_file_size,item.path,android.text.format.Formatter.formatShortFileSize(this,item.size)):item.path);choice.setTextColor(theme.text);choice.setTextSize(14);choice.setButtonTintList(android.content.res.ColorStateList.valueOf(theme.accent));choice.setPadding(dp(8),dp(8),dp(8),dp(8));choice.setChecked(model.selected.contains(item.key()));choice.setEnabled(!model.scanning&&!model.importing);choice.setContentDescription("Select "+item.path);rows.addView(choice,new LinearLayout.LayoutParams(-1,-2));
            choice.setOnCheckedChangeListener((v,checked)->{if(checked)model.selected.add(item.key());else model.selected.remove(item.key());updateActions();});
        }updateActions();
    }
    private void updateActions(){
        List<ImportDocuments.Item> visible=visible();int selected=0;for(ImportDocuments.Item item:visible)if(model.selected.contains(item.key()))selected++;
        boolean busy=model.scanning||model.importing;count.setText(model.scanning?"Reading folder…":model.importing?"Adding music…":selected+" files selected"+(model.items.stream().anyMatch(ImportDocuments.Item::dependency)?" · PSF/USF libraries included automatically":""));
        error.setText(model.error);error.setVisibility(model.error.isEmpty()?View.GONE:View.VISIBLE);progress.setVisibility(busy?View.VISIBLE:View.GONE);
        add.setEnabled(!busy&&selected>0);queue.setEnabled(!busy&&selected>0);add.setAlpha(add.isEnabled()?1:.4f);queue.setAlpha(queue.isEnabled()?1:.4f);
        files.setEnabled(!busy);folder.setEnabled(!busy);cancel.setEnabled(!model.importing);subfolders.setEnabled(!busy);all.setEnabled(!busy&&!visible.isEmpty());
        updating=true;all.setChecked(selected>0&&selected==visible.size());updating=false;
    }
    private void commit(boolean append){if(model.scanning||model.importing)return;Set<String> selected=new HashSet<>();for(ImportDocuments.Item item:visible())if(model.selected.contains(item.key()))selected.add(item.key());if(!selected.isEmpty())model.commit(append,selected);}
    void pickFiles(){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true),1);}
    void pickFolder(){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),2);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        if(request==2&&data.getData()!=null)model.scan(Collections.emptyList(),data.getData());
        if(request==1){List<Uri> uris=new ArrayList<>();if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());else if(data.getData()!=null)uris.add(data.getData());model.scan(uris,null);}
    }
    private void cancel(){if(model.importing){Toast.makeText(this,"Finishing the import…",Toast.LENGTH_SHORT).show();return;}if(model.task!=null)model.task.interrupt();setResult(RESULT_CANCELED);finish();}
    @Override public void onBackPressed(){cancel();}
    @Override public Object onRetainNonConfigurationInstance(){return model;}
    @Override protected void onDestroy(){model.owner=null;if(!isChangingConfigurations()&&!model.importing&&model.task!=null)model.task.interrupt();super.onDestroy();}
}
