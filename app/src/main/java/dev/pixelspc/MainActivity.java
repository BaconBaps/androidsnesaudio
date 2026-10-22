package dev.pixelspc;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private int backgroundColor,textColor,mutedColor,accentColor,borderColor;
    private PlayerTheme theme;
    private Typeface pixel;
    private Library library;
    private Library.Game selected;
    private PlaybackService player;
    private LinearLayout root,trackList;
    private TextView gameTitle,gameMeta,nowTitle,nowArtist,playButton,time,shuffleButton,repeatButton,libraryButton,status;
    private SeekBar seek;
    private PixelVisualizer visualizer;
    private long lastPlayerUpdate;
    private boolean seeking=false,busy=false,bound=false;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService importer=Executors.newSingleThreadExecutor();
    private String shownError="";
    private final ServiceConnection connection=new ServiceConnection() {
        public void onServiceConnected(ComponentName n,IBinder b) { player=((PlaybackService.LocalBinder)b).get(); player.listener=()->{ updatePlayer(); renderTracks(); }; updatePlayer(); renderTracks(); }
        public void onServiceDisconnected(ComponentName n) { player=null; }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        applyPalette(PlayerTheme.find(getSharedPreferences("appearance",MODE_PRIVATE).getString("theme","light")));
        pixel=Typeface.createFromAsset(getAssets(),"fonts/pixel.ttf");
        try { library=new Library(this); if(!library.games.isEmpty()) selected=library.games.get(library.games.size()-1); }
        catch(Exception e) { dialog().setTitle("Library could not be read").setMessage(e.getMessage()).setPositiveButton("Close",(d,w)->finish()).show(); return; }
        buildUI();
    }
    @Override protected void onStart() { super.onStart(); if(library!=null) { bound=bindService(new Intent(this,PlaybackService.class),connection,BIND_AUTO_CREATE); main.post(tick); } }
    @Override protected void onStop() { main.removeCallbacks(tick); if(player!=null) player.listener=null; if(bound) { unbindService(connection); bound=false; } player=null; super.onStop(); }
    @Override protected void onDestroy() { importer.shutdown(); super.onDestroy(); }
    private final Runnable tick=new Runnable() { public void run() { updateVisualizer(); if(SystemClock.uptimeMillis()-lastPlayerUpdate>=250) { updatePlayer(); lastPlayerUpdate=SystemClock.uptimeMillis(); } main.postDelayed(this,40); } };
    int dp(float n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("kern"); return t; }
    private TextView trackTitle(String label,int size,int color) { BouncingTextView view=new BouncingTextView(this); view.setText(label); view.setTextSize(size); view.setTextColor(color); return view; }
    private TextView pixel(String s,int size,int color) { TextView t=text(s,size,color); t.setTypeface(pixel); return t; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private GradientDrawable bg(int color,int stroke) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(3)); if(stroke!=0) d.setStroke(dp(1),stroke); return d; }
    private void add(LinearLayout l,View v,int w,int h) { l.addView(v,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h))); }
    private void gap(LinearLayout l,int h) { add(l,new View(this),1,h); }
    private void rule(LinearLayout l) { View v=new View(this); v.setBackgroundColor(borderColor); add(l,v,-1,1); }
    private TextView button(String label,int color,int background) { TextView t=pixel(label,9,color); t.setGravity(Gravity.CENTER); t.setBackground(bg(background,borderColor)); t.setPadding(dp(10),0,dp(10),0); t.setClickable(true); t.setFocusable(true); return t; }
    private void buildUI() {
        getWindow().setStatusBarColor(backgroundColor); getWindow().setNavigationBarColor(Build.VERSION.SDK_INT>=27||theme.dark?backgroundColor:textColor);
        int flags=theme.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if(Build.VERSION.SDK_INT>=27 && !theme.dark) flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        root=column(); root.setBackgroundColor(backgroundColor); root.setPadding(dp(22),dp(14),dp(22),0); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{ root.setPadding(dp(22),dp(14)+insets.getSystemWindowInsetTop(),dp(22),insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets(); });
        LinearLayout header=row(); TextView logo=pixel("♫",20,accentColor); add(header,logo,34,38);
        LinearLayout brand=column(); TextView wordmark=pixel("PixelSPC",16,textColor); add(brand,wordmark,-1,-2);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1)); TextView themes=button("◈",accentColor,backgroundColor); themes.setTypeface(Typeface.DEFAULT); themes.setTextSize(22); themes.setContentDescription("Choose color theme"); add(header,themes,40,40); themes.setOnClickListener(v->chooseTheme()); TextView about=button("?",mutedColor,backgroundColor); add(header,about,36,36); about.setContentDescription("About PixelSPC and supported formats"); about.setOnClickListener(v->about()); add(root,header,-1,48); gap(root,18);
        libraryButton=button("LIBRARY ▾",accentColor,backgroundColor); libraryButton.setContentDescription("Open music library"); add(root,libraryButton,-1,44); libraryButton.setOnClickListener(v->chooseGame()); gap(root,12);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=column(); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        visualizer=new PixelVisualizer(this); visualizer.setPlayerTheme(theme); add(content,visualizer,-1,158); gap(content,16);
        gameTitle=text("Your music library",25,textColor); gameTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); gameTitle.setMaxLines(2); add(content,gameTitle,-1,-2); gap(content,5);
        gameMeta=text("Import a track or a game set.",12,mutedColor); add(content,gameMeta,-1,-2); gap(content,17);
        LinearLayout listHeader=row(); TextView tracks=pixel("TRACKLIST",8,textColor); listHeader.addView(tracks,new LinearLayout.LayoutParams(0,-2,1)); status=pixel("READY",7,mutedColor); add(listHeader,status,-2,20); add(content,listHeader,-1,26); rule(content);
        trackList=column(); add(content,trackList,-1,-2); gap(content,14);
        TextView importButton=button("+  IMPORT MUSIC",accentColor,theme.surface); add(content,importButton,-1,48); importButton.setContentDescription("Import music file or game set"); importButton.setOnClickListener(v->pickFile()); gap(content,10);
        TextView formats=text("SPC  /  RSN  /  ZIP  +  more retro formats",10,mutedColor); formats.setGravity(Gravity.CENTER); add(content,formats,-1,24); gap(content,16);
        rule(root); gap(root,12);
        LinearLayout playerHeading=row(); TextView tag=pixel("NOW PLAYING",7,accentColor); playerHeading.addView(tag,new LinearLayout.LayoutParams(0,-2,1)); TextView quality=pixel("♪  44.1 kHz",6,mutedColor); add(playerHeading,quality,-2,-2); add(root,playerHeading,-1,18);
        nowTitle=trackTitle("No track selected",17,textColor); nowTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); add(root,nowTitle,-1,26);
        nowArtist=text("Choose a track to press play",11,mutedColor); nowArtist.setSingleLine(true); add(root,nowArtist,-1,22);
        seek=new SeekBar(this); seek.setPadding(dp(12),0,dp(12),0); seek.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor)); seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(borderColor)); seek.setThumbTintList(android.content.res.ColorStateList.valueOf(accentColor)); seek.setContentDescription("Playback position"); LinearLayout.LayoutParams seekParams=new LinearLayout.LayoutParams(-1,dp(28)); seekParams.setMargins(dp(18),0,dp(18),0); root.addView(seek,seekParams);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(SeekBar s,int p,boolean user) { if(user) updateTime(p); } public void onStartTrackingTouch(SeekBar s) { seeking=true; } public void onStopTrackingTouch(SeekBar s) { seeking=false; if(player!=null) player.seek(s.getProgress()); } });
        time=text("0:00   /   --:--",10,mutedColor); time.setTypeface(Typeface.MONOSPACE); add(root,time,-1,18);
        LinearLayout controls=row(); controls.setGravity(Gravity.CENTER);
        shuffleButton=button("⇄",mutedColor,backgroundColor); shuffleButton.setTypeface(Typeface.DEFAULT); shuffleButton.setTextSize(10); shuffleButton.setContentDescription("Toggle shuffle"); add(controls,shuffleButton,50,54); shuffleButton.setOnClickListener(v->{ if(player!=null) { player.setShuffle(!player.shuffle); updatePlayer(); Toast.makeText(this,player.shuffle?"Shuffle on · next track is random":"Shuffle off · play in order",Toast.LENGTH_SHORT).show(); } });
        TextView prev=button("|◀",textColor,backgroundColor); prev.setContentDescription("Previous track"); add(controls,prev,54,44); prev.setOnClickListener(v->{ if(player!=null) player.next(-1); });
        playButton=button("▶",theme.onAccent,accentColor); playButton.setTextSize(19); playButton.setContentDescription("Play"); LinearLayout.LayoutParams playParams=new LinearLayout.LayoutParams(dp(66),dp(54)); playParams.setMargins(dp(12),0,dp(12),0); controls.addView(playButton,playParams); playButton.setOnClickListener(v->toggle());
        TextView next=button("▶|",textColor,backgroundColor); next.setContentDescription("Next track"); add(controls,next,54,44); next.setOnClickListener(v->{ if(player!=null) player.next(1); });
        repeatButton=button("↻",mutedColor,backgroundColor); repeatButton.setTypeface(Typeface.DEFAULT); repeatButton.setTextSize(10); repeatButton.setContentDescription("Toggle repeat track"); add(controls,repeatButton,50,54); repeatButton.setOnClickListener(v->{ if(player!=null) { player.setLoop(!player.repeat); updatePlayer(); Toast.makeText(this,player.repeat?"Loop on · keep the sound program running":"Loop off · fade and advance",Toast.LENGTH_SHORT).show(); } }); add(root,controls,-1,64);
        gap(root,10);
        renderTracks(); updatePlayer();
    }
    public void renderTracks() {
        if(trackList==null) return;
        trackList.removeAllViews(); gameTitle.setText(selected==null?"Your music library":selected.name);
        gameMeta.setText(selected==null?"Import a track or a game set.":systemLabel(selected)+"  ·  "+selected.tracks.size()+" tracks  ·  Local files");
        status.setText(busy?"IMPORTING…":selected==null?"READY":String.format(Locale.ROOT,"%02d TRACKS",selected.tracks.size()));
        if(selected==null) {
            LinearLayout empty=column(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(16),dp(22),dp(16),dp(22)); add(empty,pixel("INSERT SOUNDTRACK",9,textColor),-2,-2); gap(empty,10); TextView hint=text("Open an SPC track or a whole game set.\nYour music stays on your device.",12,mutedColor); hint.setGravity(Gravity.CENTER); add(empty,hint,-1,-2); add(trackList,empty,-1,-2); return;
        }
        for(int i=0;i<selected.tracks.size();i++) {
            final int index=i; Library.Track track=selected.tracks.get(i); boolean active=player!=null && player.current!=null && track.path.equals(player.current.path) && track.index==player.current.index;
            LinearLayout line=row(); line.setPadding(dp(10),dp(7),dp(10),dp(7)); if(active) line.setBackground(bg(theme.surface,0));
            TextView number=pixel(active && player.playing?"▮▮":String.format(Locale.ROOT,"%02d",i+1),8,active?accentColor:mutedColor); add(line,number,34,26);
            LinearLayout names=column(); TextView title=trackTitle(track.title,14,active?accentColor:textColor); if(active) title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); add(names,title,-1,23); TextView sub=text(track.artist.isEmpty()?track.system:track.artist,10,mutedColor); sub.setSingleLine(true); add(names,sub,-1,17); line.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            if(track.loopLength>0) {
                boolean looping=player!=null && player.isLooping(track);
                TextView mode=button(looping?"LOOP":"STRAIGHT",looping?theme.onAccent:accentColor,looping?accentColor:theme.surface);
                mode.setTextSize(6); mode.setContentDescription("Loop mode for "+track.title+": "+(looping?"looping":"straight")); mode.setEnabled(player!=null);
                add(line,mode,74,34);
                mode.setOnClickListener(v->{ if(player!=null) { player.setTrackLoop(track,!player.isLooping(track)); renderTracks(); updatePlayer(); } });
            }
            TextView duration=text(clock(track.length),11,mutedColor); duration.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); add(line,duration,43,30); add(trackList,line,-1,57); rule(trackList);
            line.setContentDescription("Play "+track.title); line.setClickable(true); line.setFocusable(true); line.setOnClickListener(v->{ if(player!=null) { notifyPermission(); player.play(selected.tracks,index); } });
        }
    }
    private String systemLabel(Library.Game g) { String s=g.tracks.get(0).system; return s.isEmpty()?"Retro game music":s; }
    private void toggle() {
        if(player==null) return;
        notifyPermission();
        if(player.current==null) { if(selected!=null) player.play(selected.tracks,0); else pickFile(); }
        else player.toggle();
    }
    private void notifyPermission() { if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2); }
    private void updatePlayer() {
        if(nowTitle==null) return;
        boolean loaded=player!=null && player.current!=null;
        seek.setEnabled(loaded);
        styleMode(shuffleButton,player!=null && player.shuffle,"⇄","Shuffle");
        styleMode(repeatButton,player!=null && player.repeat,"↻","Continuous loop");
        shuffleButton.setEnabled(player!=null); repeatButton.setEnabled(player!=null);
        updateVisualizer();
        if(!loaded) return;
        Library.Track t=player.current; nowTitle.setText(t.title); nowArtist.setText(getString(R.string.artist_and_game,t.artist.isEmpty()?"Unknown artist":t.artist,t.game));
        playButton.setText(player.playing?"Ⅱ":"▶"); playButton.setContentDescription(player.playing?"Pause":"Play");
        if(!seeking) { seek.setMax(player.repeat?Math.max(t.length,player.position):t.length); seek.setProgress(Math.min(player.position,seek.getMax())); updateTime(player.position); }
        if(!player.error.isEmpty() && !player.error.equals(shownError)) { shownError=player.error; dialog().setTitle("Playback interrupted").setMessage(player.error).setPositiveButton("OK",null).show(); }
    }
    private void styleMode(TextView button,boolean enabled,String icon,String name) {
        android.text.SpannableString label=new android.text.SpannableString(icon+"\n"+(enabled?"ON":"OFF"));
        label.setSpan(new android.text.style.RelativeSizeSpan(2f),0,icon.length(),0);
        button.setText(label); button.setTextColor(enabled?accentColor:mutedColor);
        button.setBackground(null);
        button.setSelected(enabled); button.setContentDescription(name+" "+(enabled?"on":"off"));
    }
    private void updateVisualizer() {
        if(visualizer!=null && player!=null) visualizer.setFrame(player.levels,player.playing,player.current!=null);
    }
    private void updateTime(int ms) { String end=player==null||player.current==null?"--:--":player.repeat?"∞ LOOP":clock(player.current.length); time.setText(getString(R.string.position_and_duration,clock(ms),end)); }
    public static String clock(int ms) { return String.format(Locale.ROOT,"%d:%02d",Math.max(ms,0)/60000,(Math.max(ms,0)/1000)%60); }
    private View gameRow(Library.Game game) {
        LinearLayout line=row(); line.setPadding(dp(8),dp(10),dp(8),dp(10));
        LinearLayout names=column();  TextView title=text(game.name,16,textColor); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setMaxLines(2); add(names,title,-1,-2); add(names,text(game.tracks.size()+" tracks · "+systemLabel(game),11,mutedColor),-1,-2); line.addView(names,new LinearLayout.LayoutParams(0,-2,1)); return line;
    }
    private void chooseGame() {
        if(library.games.isEmpty()) { pickFile(); return; }
        ListView list=new ListView(this); list.setBackgroundColor(backgroundColor); list.setAdapter(new BaseAdapter() {
            public int getCount() { return library.games.size(); }
            public Object getItem(int position) { return library.games.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position,View old,ViewGroup parent) { return gameRow(library.games.get(position)); }
        });
        LinearLayout panel=column(); add(panel,list,-1,Math.min(420,library.games.size()*84));
        AlertDialog picker=dialog().setTitle("Library").setView(panel).setPositiveButton("IMPORT MUSIC",(d,w)->pickFile()).setNegativeButton("CLOSE",null).show();
        list.setOnItemClickListener((parent,view,position,id)->{ selected=library.games.get(position); picker.dismiss(); renderTracks(); });
    }
    private void pickFile() {
        if(busy) { Toast.makeText(this,"A soundtrack is already importing",Toast.LENGTH_SHORT).show(); return; }
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(pick,1);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==1 && result==RESULT_OK && data!=null && data.getData()!=null) importUri(data.getData());
    }
    void importUri(Uri uri) {
        busy=true; renderTracks();
        importer.execute(()->{
            try { Library.Game game=library.importFile(uri); main.post(()->{ busy=false; selected=game; if(!isDestroyed()) { renderTracks(); Toast.makeText(this,"Imported "+game.tracks.size()+" tracks",Toast.LENGTH_SHORT).show(); } }); }
            catch(Exception e) { main.post(()->{ busy=false; if(!isDestroyed()) { renderTracks(); dialog().setTitle("Could not import soundtrack").setMessage(e.getMessage()).setPositiveButton("OK",null).show(); } }); }
        });
    }
    private void applyPalette(PlayerTheme palette) {
        theme=palette; backgroundColor=palette.background; textColor=palette.text; mutedColor=palette.muted;
        accentColor=palette.accent; borderColor=palette.border;
    }
    void selectTheme(String id) {
        applyPalette(PlayerTheme.find(id));
        getSharedPreferences("appearance",MODE_PRIVATE).edit().putString("theme",theme.id).apply();
        seeking=false; buildUI();
    }
    private void chooseTheme() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false);
        GridLayout grid=new GridLayout(this); grid.setColumnCount(2); grid.setPadding(dp(12),dp(4),dp(12),dp(12)); scroll.addView(grid);
        AlertDialog picker=dialog().setTitle("Choose your palette").setView(scroll).setNegativeButton("CANCEL",null).create();
        for(int i=0;i<PlayerTheme.ALL.length;i++) {
            PlayerTheme palette=PlayerTheme.ALL[i]; ThemePreview preview=new ThemePreview(this,palette,palette==theme);
            GridLayout.LayoutParams params=new GridLayout.LayoutParams(GridLayout.spec(i/2),GridLayout.spec(i%2,1f));
            params.width=0; params.height=dp(150); params.setMargins(dp(4),dp(4),dp(4),dp(4)); grid.addView(preview,params);
            preview.setOnClickListener(v->{ picker.dismiss(); selectTheme(palette.id); });
        }
        picker.show(); picker.getWindow().setBackgroundDrawable(bg(backgroundColor,borderColor)); tintDialog(picker.getWindow().getDecorView()); picker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(accentColor);
        picker.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(560)),(int)(getResources().getDisplayMetrics().heightPixels*.85f));
    }
    private AlertDialog.Builder dialog() {
        Context themed=new ContextThemeWrapper(this,theme.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        return new AlertDialog.Builder(themed) {
            @Override public AlertDialog show() {
                AlertDialog result=super.show();
                result.getWindow().setBackgroundDrawable(bg(backgroundColor,borderColor));
                tintDialog(result.getWindow().getDecorView());
                for(int which:new int[]{DialogInterface.BUTTON_POSITIVE,DialogInterface.BUTTON_NEGATIVE,DialogInterface.BUTTON_NEUTRAL}) {
                    Button b=result.getButton(which); if(b!=null) b.setTextColor(accentColor);
                }
                return result;
            }
        };
    }
    private void tintDialog(View view) {
        if(view instanceof TextView) ((TextView)view).setTextColor(textColor);
        if(view instanceof CheckedTextView) ((CheckedTextView)view).setCheckMarkTintList(android.content.res.ColorStateList.valueOf(accentColor));
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) tintDialog(((ViewGroup)view).getChildAt(i));
    }
    private void about() {
        dialog().setTitle("PixelSPC · v1.5").setMessage("A pocket home for retro soundtracks.\n\nOpen SPC tracks, ZIP sets and RAR-based RSN sets. Also plays NSF/NSFE (NES), GBS (Game Boy), VGM/VGZ (many systems), GYM, HES, KSS, AY and SAP. Multi-song files become individual tracks.\n\nNo ROMs or music are included. Imports are copied into private app storage; no network or broad storage permission is used. Tracks without timing tags play for 3 minutes, followed by a fade. Enable Loop to let an SPC sound program loop naturally without a timed restart. One-shot tracks without a programmed loop may become silent. Shuffle changes the next track, not the current song. The visualizer follows decoded audio and settles when paused.\n\nAudio: Game Music Emu (LGPL 2.1+). RSN: Junrar (UnRAR license). Pixel font: Press Start 2P (SIL OFL). Original pixel art.").setNeutralButton("LICENSES",(d,w)->licenses()).setPositiveButton("LET’S PLAY",null).show();
    }
    private void licenses() {
        StringBuilder all=new StringBuilder("Source and build instructions:\nhttps://github.com/BaconBaps/androidsnesaudio\n\n");
        try {
            for(String name:getAssets().list("licenses")) {
                all.append("\n━━ ").append(name).append(" ━━\n\n");
                try(java.io.InputStream in=getAssets().open("licenses/"+name); java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
                    byte[] buffer=new byte[4096]; int n; while((n=in.read(buffer))!=-1) out.write(buffer,0,n); all.append(out.toString("UTF-8"));
                }
            }
            try(java.io.InputStream in=getAssets().open("fonts/OFL.txt"); java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096]; int n; while((n=in.read(buffer))!=-1) out.write(buffer,0,n); all.append("\n\nPress Start 2P\n\n").append(out.toString("UTF-8"));
            }
        } catch(java.io.IOException e) { all.append("\nLicense files could not be read: ").append(e.getMessage()); }
        dialog().setTitle("Open-source licenses").setMessage(all.toString()).setPositiveButton("CLOSE",null).show();
    }
}
