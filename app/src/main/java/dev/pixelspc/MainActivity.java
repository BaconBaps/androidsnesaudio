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
    static final int PAPER=0xfff6f3eb,INK=0xff302b40,MUTED=0xff817b8a,PURPLE=0xff7453cc,LINE=0xffddd8e3,GREEN=0xffdce7bf;
    private Typeface pixel;
    private Library library;
    private Library.Game selected;
    private PlaybackService player;
    private LinearLayout root,trackList;
    private TextView gameTitle,gameMeta,nowTitle,nowArtist,playButton,time,shuffleButton,repeatButton,libraryButton,status;
    private SeekBar seek;
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
        pixel=Typeface.createFromAsset(getAssets(),"fonts/pixel.ttf");
        try { library=new Library(this); if(!library.games.isEmpty()) selected=library.games.get(library.games.size()-1); }
        catch(Exception e) { new AlertDialog.Builder(this).setTitle("Library could not be read").setMessage(e.getMessage()).setPositiveButton("Close",(d,w)->finish()).show(); return; }
        buildUI();
    }
    @Override protected void onStart() { super.onStart(); if(library!=null) { bound=bindService(new Intent(this,PlaybackService.class),connection,BIND_AUTO_CREATE); main.post(tick); } }
    @Override protected void onStop() { main.removeCallbacks(tick); if(player!=null) player.listener=null; if(bound) { unbindService(connection); bound=false; } player=null; super.onStop(); }
    @Override protected void onDestroy() { importer.shutdown(); super.onDestroy(); }
    private final Runnable tick=new Runnable() { public void run() { updatePlayer(); main.postDelayed(this,350); } };
    int dp(float n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("kern"); return t; }
    private TextView pixel(String s,int size,int color) { TextView t=text(s,size,color); t.setTypeface(pixel); return t; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private GradientDrawable bg(int color,int stroke) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(3)); if(stroke!=0) d.setStroke(dp(1),stroke); return d; }
    private void add(LinearLayout l,View v,int w,int h) { l.addView(v,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h))); }
    private void gap(LinearLayout l,int h) { add(l,new View(this),1,h); }
    private void rule(LinearLayout l) { View v=new View(this); v.setBackgroundColor(LINE); add(l,v,-1,1); }
    private TextView button(String label,int color,int background) { TextView t=pixel(label,9,color); t.setGravity(Gravity.CENTER); t.setBackground(bg(background,LINE)); t.setPadding(dp(10),0,dp(10),0); t.setClickable(true); t.setFocusable(true); return t; }
    private void buildUI() {
        root=column(); root.setBackgroundColor(PAPER); root.setPadding(dp(22),dp(14),dp(22),0); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{ root.setPadding(dp(22),dp(14)+insets.getSystemWindowInsetTop(),dp(22),insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets(); });
        LinearLayout header=row(); TextView logo=pixel("♫",20,PURPLE); add(header,logo,34,38);
        LinearLayout brand=column(); TextView wordmark=pixel("PixelSPC",16,INK); add(brand,wordmark,-1,-2); gap(brand,5); add(brand,text("A little player. A whole lot of nostalgia.",10,MUTED),-1,-2);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1)); TextView about=button("?",MUTED,PAPER); add(header,about,36,36); about.setContentDescription("About PixelSPC and supported formats"); about.setOnClickListener(v->about()); add(root,header,-1,48); gap(root,18);
        LinearLayout tabs=row(); TextView tab=pixel("YOUR SOUNDTRACKS",8,PURPLE); tabs.addView(tab,new LinearLayout.LayoutParams(0,-2,1)); libraryButton=button("LIBRARY ▾",PURPLE,PAPER); add(tabs,libraryButton,112,34); libraryButton.setOnClickListener(v->chooseGame()); add(root,tabs,-1,36); gap(root,12);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=column(); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout artwork=column(); artwork.setBackground(bg(0xffe4d9f3,LINE)); PixelScene scene=new PixelScene(this); add(artwork,scene,-1,158); add(content,artwork,-1,-2); gap(content,16);
        LinearLayout titleRow=row(); gameTitle=text("Your next adventure",25,INK); gameTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); gameTitle.setMaxLines(2); titleRow.addView(gameTitle,new LinearLayout.LayoutParams(0,-2,1)); TextView chip=pixel("16-BIT",7,0xff536243); chip.setPadding(dp(9),dp(9),dp(9),dp(9)); chip.setBackground(bg(GREEN,0)); add(titleRow,chip,-2,-2); add(content,titleRow,-1,-2); gap(content,5);
        gameMeta=text("Import a game. Rediscover its world.",12,MUTED); add(content,gameMeta,-1,-2); gap(content,17);
        LinearLayout listHeader=row(); TextView tracks=pixel("TRACKLIST",8,INK); listHeader.addView(tracks,new LinearLayout.LayoutParams(0,-2,1)); status=pixel("READY",7,MUTED); add(listHeader,status,-2,20); add(content,listHeader,-1,26); rule(content);
        trackList=column(); add(content,trackList,-1,-2); gap(content,14);
        TextView importButton=button("+  IMPORT MUSIC",PURPLE,0xffeee8f8); add(content,importButton,-1,48); importButton.setContentDescription("Import music file or game set"); importButton.setOnClickListener(v->pickFile()); gap(content,10);
        TextView formats=text("SPC  /  RSN  /  ZIP  +  more retro formats",10,MUTED); formats.setGravity(Gravity.CENTER); add(content,formats,-1,24); gap(content,16);
        rule(root); gap(root,12);
        LinearLayout playerHeading=row(); TextView tag=pixel("NOW PLAYING",7,PURPLE); playerHeading.addView(tag,new LinearLayout.LayoutParams(0,-2,1)); TextView quality=pixel("♪  44.1 kHz",6,MUTED); add(playerHeading,quality,-2,-2); add(root,playerHeading,-1,18);
        nowTitle=text("The adventure starts here",17,INK); nowTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); nowTitle.setSingleLine(true); nowTitle.setEllipsize(android.text.TextUtils.TruncateAt.END); add(root,nowTitle,-1,26);
        nowArtist=text("Choose a track to press play",11,MUTED); nowArtist.setSingleLine(true); add(root,nowArtist,-1,22);
        seek=new SeekBar(this); seek.setPadding(0,0,0,0); seek.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE)); seek.setThumbTintList(android.content.res.ColorStateList.valueOf(PURPLE)); seek.setContentDescription("Playback position"); add(root,seek,-1,24);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(SeekBar s,int p,boolean user) { if(user) updateTime(p); } public void onStartTrackingTouch(SeekBar s) { seeking=true; } public void onStopTrackingTouch(SeekBar s) { seeking=false; if(player!=null) player.seek(s.getProgress()); } });
        time=text("0:00                                                      --:--",10,MUTED); time.setTypeface(Typeface.MONOSPACE); add(root,time,-1,18);
        LinearLayout controls=row(); controls.setGravity(Gravity.CENTER);
        shuffleButton=button("⇄",MUTED,PAPER); shuffleButton.setTypeface(Typeface.DEFAULT); shuffleButton.setTextSize(24); shuffleButton.setContentDescription("Toggle shuffle"); add(controls,shuffleButton,44,44); shuffleButton.setOnClickListener(v->{ if(player!=null) { player.shuffle=!player.shuffle; updatePlayer(); } });
        TextView prev=button("|◀",INK,PAPER); prev.setContentDescription("Previous track"); add(controls,prev,54,44); prev.setOnClickListener(v->{ if(player!=null) player.next(-1); });
        playButton=button("▶",0xffffffff,PURPLE); playButton.setTextSize(19); playButton.setContentDescription("Play"); LinearLayout.LayoutParams playParams=new LinearLayout.LayoutParams(dp(66),dp(54)); playParams.setMargins(dp(12),0,dp(12),0); controls.addView(playButton,playParams); playButton.setOnClickListener(v->toggle());
        TextView next=button("▶|",INK,PAPER); next.setContentDescription("Next track"); add(controls,next,54,44); next.setOnClickListener(v->{ if(player!=null) player.next(1); });
        repeatButton=button("↻",MUTED,PAPER); repeatButton.setTypeface(Typeface.DEFAULT); repeatButton.setTextSize(25); repeatButton.setContentDescription("Toggle repeat track"); add(controls,repeatButton,44,44); repeatButton.setOnClickListener(v->{ if(player!=null) { player.repeat=!player.repeat; updatePlayer(); } }); add(root,controls,-1,64);
        TextView footer=pixel("SOUNDTRACKS NEVER GET OLD.",6,MUTED); footer.setGravity(Gravity.CENTER); add(root,footer,-1,27);
        renderTracks(); updatePlayer();
    }
    public void renderTracks() {
        if(trackList==null) return;
        trackList.removeAllViews(); gameTitle.setText(selected==null?"Your next adventure":selected.name);
        gameMeta.setText(selected==null?"Import a game. Rediscover its world.":systemLabel(selected)+"  ·  "+selected.tracks.size()+" tracks  ·  Local files");
        status.setText(busy?"IMPORTING…":selected==null?"READY":String.format(Locale.ROOT,"%02d TRACKS",selected.tracks.size()));
        if(selected==null) {
            LinearLayout empty=column(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(16),dp(22),dp(16),dp(22)); add(empty,pixel("INSERT SOUNDTRACK",9,INK),-2,-2); gap(empty,10); TextView hint=text("Open an SPC track or a whole game set.\nYour music stays on your device.",12,MUTED); hint.setGravity(Gravity.CENTER); add(empty,hint,-1,-2); add(trackList,empty,-1,-2); return;
        }
        for(int i=0;i<selected.tracks.size();i++) {
            final int index=i; Library.Track track=selected.tracks.get(i); boolean active=player!=null && player.current!=null && track.path.equals(player.current.path) && track.index==player.current.index;
            LinearLayout line=row(); line.setPadding(dp(10),dp(7),dp(10),dp(7)); if(active) line.setBackground(bg(0xffeae3f5,0));
            TextView number=pixel(active && player.playing?"▮▮":String.format(Locale.ROOT,"%02d",i+1),8,active?PURPLE:MUTED); add(line,number,34,26);
            LinearLayout names=column(); TextView title=text(track.title,14,active?PURPLE:INK); title.setSingleLine(true); title.setEllipsize(android.text.TextUtils.TruncateAt.END); if(active) title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); add(names,title,-1,23); TextView sub=text(track.artist.isEmpty()?track.system:track.artist,10,MUTED); sub.setSingleLine(true); add(names,sub,-1,17); line.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            TextView duration=text(clock(track.length),11,MUTED); duration.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); add(line,duration,43,30); add(trackList,line,-1,57); rule(trackList);
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
        seek.setEnabled(loaded); shuffleButton.setAlpha(loaded?1f:.45f); repeatButton.setAlpha(loaded?1f:.45f);
        if(!loaded) return;
        Library.Track t=player.current; nowTitle.setText(t.title); nowArtist.setText(getString(R.string.artist_and_game,t.artist.isEmpty()?"Unknown artist":t.artist,t.game));
        playButton.setText(player.playing?"Ⅱ":"▶"); playButton.setContentDescription(player.playing?"Pause":"Play");
        shuffleButton.setTextColor(player.shuffle?PURPLE:MUTED); shuffleButton.setContentDescription("Shuffle "+(player.shuffle?"on":"off")); repeatButton.setTextColor(player.repeat?PURPLE:MUTED); repeatButton.setContentDescription("Repeat track "+(player.repeat?"on":"off"));
        if(!seeking) { seek.setMax(t.length); seek.setProgress(Math.min(player.position,t.length)); updateTime(player.position); }
        if(!player.error.isEmpty() && !player.error.equals(shownError)) { shownError=player.error; new AlertDialog.Builder(this).setTitle("Playback interrupted").setMessage(player.error).setPositiveButton("OK",null).show(); }
    }
    private void updateTime(int ms) { String end=player==null||player.current==null?"--:--":clock(player.current.length); time.setText(getString(R.string.position_and_duration,clock(ms),end)); }
    public static String clock(int ms) { return String.format(Locale.ROOT,"%d:%02d",Math.max(ms,0)/60000,(Math.max(ms,0)/1000)%60); }
    private void chooseGame() {
        if(library.games.isEmpty()) { pickFile(); return; }
        String[] titles=new String[library.games.size()+1]; for(int i=0;i<library.games.size();i++) titles[i]=library.games.get(i).name+"  ("+library.games.get(i).tracks.size()+")"; titles[titles.length-1]="+ Import another soundtrack";
        new AlertDialog.Builder(this).setTitle("Your soundtracks").setItems(titles,(d,w)->{ if(w==library.games.size()) pickFile(); else { selected=library.games.get(w); renderTracks(); } }).show();
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
            catch(Exception e) { main.post(()->{ busy=false; if(!isDestroyed()) { renderTracks(); new AlertDialog.Builder(this).setTitle("Could not import soundtrack").setMessage(e.getMessage()).setPositiveButton("OK",null).show(); } }); }
        });
    }
    private void about() {
        new AlertDialog.Builder(this).setTitle("PixelSPC · v1.0").setMessage("A pocket home for retro soundtracks.\n\nOpen SPC tracks, ZIP sets and RAR-based RSN sets. Also plays NSF/NSFE (NES), GBS (Game Boy), VGM/VGZ (many systems), GYM, HES, KSS, AY and SAP. Multi-song files become individual tracks.\n\nNo ROMs or music are included. Imports are copied into private app storage; no network or broad storage permission is used. Tracks without timing tags play for 3 minutes, followed by a fade.\n\nAudio: Game Music Emu (LGPL 2.1+). RSN: Junrar (UnRAR license). Pixel font: Press Start 2P (SIL OFL). Original pixel art.").setNeutralButton("LICENSES",(d,w)->licenses()).setPositiveButton("LET’S PLAY",null).show();
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
        new AlertDialog.Builder(this).setTitle("Open-source licenses").setMessage(all.toString()).setPositiveButton("CLOSE",null).show();
    }
}
