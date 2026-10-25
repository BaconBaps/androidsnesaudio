package dev.pixelspc;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private int backgroundColor,textColor,mutedColor,accentColor,borderColor;
    private PlayerTheme theme,playbackTheme,listTheme;
    private PlayerSkin skin;
    private Typeface pixel,dotMatrix,dotMatrixBold;
    private Library library;
    private TrackFavorites favourites;
    private SoundFonts soundFonts;
    private boolean showFavourites,showQueue;
    private ArrayList<String> pendingQueueIds=new ArrayList<>();
    private Library.Game selected;
    private PlaybackService player;
    private LinearLayout root,trackList;
    private TextView gameTitle,gameMeta,nowTitle,nowArtist,playButton,time,shuffleButton,repeatButton,libraryButton,status,listHeading;
    private SeekBar seek;
    private PixelVisualizer visualizer;
    private long lastPlayerUpdate;
    private boolean seeking=false,busy=false,bound=false;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService importer=Executors.newSingleThreadExecutor();
    private String shownError="";
    private Dialog libraryPicker;
    private ThemeEditor.State editState;
    private final List<ThemeEditor.Target> editTargets=new ArrayList<>();
    private final List<Dialog> editorDialogs=new ArrayList<>();
    private String pendingThemeExport;
    void trackEditorDialog(Dialog dialog){editorDialogs.add(dialog);dialog.setOnDismissListener(d->editorDialogs.remove(dialog));}
    private void editTarget(View view,String title,String...roles){if(editState!=null)editTargets.add(new ThemeEditor.Target(view,title,roles));}

    private final ServiceConnection connection=new ServiceConnection() {
        public void onServiceConnected(ComponentName n,IBinder b) { player=((PlaybackService.LocalBinder)b).get(); flushPendingQueue(); player.listener=()->{ if(editState==null){updatePlayer(); renderTracks();} }; if(editState==null){updatePlayer(); renderTracks();} }
        public void onServiceDisconnected(ComponentName n) { player=null; }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        applyPalette(ThemeStore.selected(this));
        pixel=Typeface.createFromAsset(getAssets(),"fonts/pixel.ttf");
        dotMatrix=new Typeface.Builder(getAssets(),"fonts/doto.ttf").setFontVariationSettings("'ROND' 100, 'wght' 800").build();
        dotMatrixBold=new Typeface.Builder(getAssets(),"fonts/doto.ttf").setFontVariationSettings("'ROND' 100, 'wght' 900").setWeight(900).build(); skin=new PlayerSkin(this);
        try { library=new Library(this); if(!library.games.isEmpty()) selected=library.games.get(library.games.size()-1); }
        catch(Exception e) { dialog().setTitle("Library could not be read").setMessage(e.getMessage()).setPositiveButton("Close",(d,w)->finish()).show(); return; }
        soundFonts=new SoundFonts(this);
        favourites=new TrackFavorites(this); showFavourites=state!=null && state.getBoolean("showFavourites");
        showQueue=state!=null&&state.getBoolean("showQueue");
        if(state!=null&&state.getStringArrayList("pendingQueueIds")!=null)pendingQueueIds=state.getStringArrayList("pendingQueueIds");
        if(state!=null)for(Library.Game game:library.games)if(game.id.equals(state.getString("selectedGame")))selected=game;
        String savedId=state!=null?state.getString("selectedGame"):getSharedPreferences("library-browser",MODE_PRIVATE).getString("selected-playlist","");if(savedId!=null&&savedId.startsWith("playlist-"))for(Library.Game game:savedPlaylists())if(game.id.equals(savedId))selected=game;
        if(getLastNonConfigurationInstance() instanceof ThemeEditor.State)editState=(ThemeEditor.State)getLastNonConfigurationInstance();
        else if(state!=null&&state.getString("themeEdit")!=null)try{editState=ThemeEditor.State.read(state.getString("themeEdit"));editState.tracks=new ArrayList<>(selected==null?Collections.emptyList():selected.tracks);}catch(Exception ignored){editState=null;}
        if(editState!=null)applyPalette(editState.draft.theme());
        if(state!=null)pendingThemeExport=state.getString("themeExport");
        buildUI();
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putBoolean("showFavourites",showFavourites);out.putString("themeExport",pendingThemeExport);if(editState!=null)try{out.putString("themeEdit",editState.json().toString());}catch(Exception ignored){}out.putBoolean("showQueue",showQueue);out.putStringArrayList("pendingQueueIds",pendingQueueIds);if(selected!=null)out.putString("selectedGame",selected.id); }
    @Override protected void onStart() { super.onStart(); if(library!=null) { bound=bindService(new Intent(this,PlaybackService.class),connection,BIND_AUTO_CREATE); if(editState==null)main.post(tick); } }
    @Override protected void onStop() { main.removeCallbacks(tick); if(player!=null) player.listener=null; if(bound) { unbindService(connection); bound=false; } player=null; super.onStop(); }
    @Override protected void onDestroy() { if(appMenu!=null)appMenu.dismiss();for(Dialog d:new ArrayList<>(editorDialogs))d.dismiss();main.removeCallbacks(repaintEdit); importer.shutdown(); super.onDestroy(); }
    private final Runnable tick=new Runnable() { public void run() { if(editState!=null)return;updateVisualizer(); if(SystemClock.uptimeMillis()-lastPlayerUpdate>=250) { updatePlayer(); lastPlayerUpdate=SystemClock.uptimeMillis(); } main.postDelayed(this,40); } };
    int dp(float n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("kern"); return t; }
    private TextView trackTitle(String label,int size,int color) { BouncingTextView view=new BouncingTextView(this); view.setText(label);view.setFrozen(editState!=null); view.setTextSize(size); view.setTextColor(color); return view; }
    private TextView pixel(String s,int size,int color) { TextView t=text(s,size,color); t.setTypeface(pixel); t.setIncludeFontPadding(false); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); l.setBaselineAligned(false); return l; }
    private void add(LinearLayout l,View v,int w,int h) { l.addView(v,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h))); }
    private void gap(LinearLayout l,int h) { add(l,new View(this),1,h); }
    private void rule(LinearLayout l) { View v=new View(this); v.setBackgroundColor(borderColor); add(l,v,-1,1); }
    private TextView button(String label,int color,int background) { return button(theme,label,color,background); }
    private TextView button(PlayerTheme palette,String label,int color,int background) { TextView t=pixel(label,9,palette.buttonText(color)); t.setGravity(Gravity.CENTER); t.setBackground(skin.button(palette.buttonFace(background))); t.setPadding(dp(10),0,dp(10),0); t.setClickable(true); t.setFocusable(true); return t; }
    private void playbackControl(LinearLayout row,View control,float weight,int height) {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(height),weight);
        params.setMargins(row.getChildCount()==0?0:dp(getResources().getConfiguration().screenWidthDp<360?6:8),0,0,0);row.addView(control,params);
    }
    private ImageButton headerIcon(int image,String description) {
        ImageButton button=new ImageButton(this);
        button.setImageResource(image); button.setImageTintList(null);
        button.setScaleType(ImageView.ScaleType.CENTER); button.setBackground(null);
        button.setPadding(dp(4),dp(4),dp(4),dp(4)); button.setContentDescription(description); button.setTooltipText(description);
        return button;
    }
    private void buildUI() {
        editTargets.clear();
        getWindow().setStatusBarColor(backgroundColor); getWindow().setNavigationBarColor(Build.VERSION.SDK_INT>=27||theme.dark?backgroundColor:textColor);
        int flags=theme.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if(Build.VERSION.SDK_INT>=27 && !theme.dark) flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        root=column(); root.setBackgroundColor(backgroundColor); root.setPadding(dp(12),dp(12),dp(12),dp(10)); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{ root.setPadding(dp(12),dp(12)+insets.getSystemWindowInsetTop(),dp(12),dp(10)+insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets(); });
        LinearLayout header=row(); header.setPadding(dp(8),dp(4),dp(8),dp(4));
        ChipLogo wordmark=new ChipLogo(this); wordmark.setPadding(0,0,dp(8),0);
        header.addView(wordmark,new LinearLayout.LayoutParams(0,dp(42),1));
        ImageButton menu=headerIcon(R.drawable.ic_menu,"App menu"); add(header,menu,40,40); menu.setOnClickListener(this::showMenu);menu.setOnLongClickListener(v->{showSavedThemes();return true;});
        add(root,header,-1,50); gap(root,8);
        libraryButton=button("LIBRARY ▾",accentColor,theme.libraryButtonBackground()); libraryButton.setBackground(skin.button(theme.libraryButtonBackground()));libraryButton.setContentDescription("Open music library");editTarget(libraryButton,"Library button","libraryButton","accent"); add(root,libraryButton,-1,38); libraryButton.setOnClickListener(v->chooseGame()); gap(root,8);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(true); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=root;
        gameTitle=trackTitle("Your music library",26,theme.heading); gameTitle.setTypeface(dotMatrix);editTarget(gameTitle,"Game heading","heading"); add(content,gameTitle,-1,-2); gap(content,5);
        gameMeta=text("Import a track or a game set.",12,mutedColor); add(content,gameMeta,-1,-2);editTarget(gameMeta,"Secondary text","muted"); gap(content,10);
        LinearLayout listHeader=row(); listHeader.setBackground(skin.panel(backgroundColor)); listHeader.setPadding(dp(10),0,dp(10),0); listHeading=pixel("TRACKLIST",8,theme.heading); add(listHeader,listHeading,-2,-2); View listRail=new View(this); listRail.setBackground(skin.rail(theme.secondary)); LinearLayout.LayoutParams listRailParams=new LinearLayout.LayoutParams(0,dp(6),1); listRailParams.setMargins(dp(10),0,dp(10),0); listHeader.addView(listRail,listRailParams); status=pixel("READY",7,mutedColor); add(listHeader,status,-2,20); add(content,listHeader,-1,28);editTarget(listHeader,"Section headings","heading","secondary","border");
        FrameLayout playlistFrame=new FrameLayout(this); playlistFrame.setContentDescription("Tracklist frame");
        playlistFrame.setBackground(skin.well(listTheme.background)); playlistFrame.setPadding(dp(6),dp(5),dp(6),dp(5));
        root.addView(playlistFrame,new LinearLayout.LayoutParams(-1,0,1));
        playlistFrame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        trackList=column();editTarget(playlistFrame,"Tracklist","library.background","library.text","library.accent","library.muted","library.surface"); scroll.addView(trackList,new ScrollView.LayoutParams(-1,-2));
        gap(root,8);
        LinearLayout playerPanel=column(); playerPanel.setBackground(skin.chassis(playbackTheme.background)); playerPanel.setContentDescription("Now Playing panel");editTarget(playerPanel,"Player background","player.background","player.border"); playerPanel.setPadding(dp(8),dp(6),dp(8),dp(6)); add(root,playerPanel,-1,-2);
        LinearLayout playerHeading=row(); View rail=new View(this); rail.setBackground(skin.rail(playbackTheme.secondary)); playerHeading.addView(rail,new LinearLayout.LayoutParams(dp(32),dp(7))); TextView tag=pixel(" NOW PLAYING",7,playbackTheme.heading); add(playerHeading,tag,-2,-2); View playerRail=new View(this); playerRail.setBackground(skin.rail(playbackTheme.secondary)); LinearLayout.LayoutParams railParams=new LinearLayout.LayoutParams(0,dp(6),1); railParams.setMargins(dp(8),0,dp(8),0); playerHeading.addView(playerRail,railParams); TextView quality=pixel("♪  44.1 kHz",6,playbackTheme.muted); add(playerHeading,quality,-2,-2); add(playerPanel,playerHeading,-1,22);editTarget(playerHeading,"Player heading","player.heading","player.secondary");
        visualizer=new PixelVisualizer(this); visualizer.setPlayerTheme(playbackTheme);editTarget(visualizer,"Spectrum colours","player.spectrumLow","player.spectrumMid","player.spectrumHigh","player.spectrumPeak","player.spectrumBackground","player.heading"); add(playerPanel,visualizer,-1,126); gap(playerPanel,6);
        LinearLayout readout=column(); readout.setBackground(skin.well(playbackTheme.value("readout",playbackTheme.background)));editTarget(readout,"Track readout","player.readout","player.accent","player.muted"); readout.setPadding(dp(8),dp(4),dp(8),dp(4)); add(playerPanel,readout,-1,-2);
        nowTitle=trackTitle("No track selected",22,playbackTheme.accent); nowTitle.setTypeface(dotMatrixBold); add(readout,nowTitle,-1,30);editTarget(nowTitle,"Track title","player.accent");
        nowArtist=text("Choose a track to press play",11,playbackTheme.muted); nowArtist.setSingleLine(true); add(readout,nowArtist,-1,20);editTarget(nowArtist,"Artist text","player.muted");
        seek=(SeekBar)getLayoutInflater().inflate(R.layout.player_seek,playerPanel,false); seek.setPadding(dp(12),0,dp(12),0); seek.setProgressTintList(null); seek.setProgressBackgroundTintList(null); seek.setThumbTintList(null); seek.setProgressDrawable(skin.progress(playbackTheme)); seek.setThumb(skin.thumb(playbackTheme.accent)); seek.setSplitTrack(false); seek.setContentDescription("Playback position"); LinearLayout.LayoutParams seekParams=new LinearLayout.LayoutParams(-1,dp(32)); seekParams.setMargins(dp(18),0,dp(18),0); playerPanel.addView(seek,seekParams);editTarget(seek,"Progress bar","player.accent","player.secondary");
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(SeekBar s,int p,boolean user) { if(user) updateTime(p); } public void onStartTrackingTouch(SeekBar s) { seeking=true; } public void onStopTrackingTouch(SeekBar s) { seeking=false; if(player!=null) player.seek(s.getProgress()); } });
        time=text("0:00   /   --:--",10,playbackTheme.accent); time.setTypeface(Typeface.create("monospace",Typeface.BOLD)); time.setPadding(dp(8),0,0,0); add(playerPanel,time,-1,18);editTarget(time,"Time text","player.accent");
        LinearLayout controls=row(); controls.setGravity(Gravity.CENTER);
        shuffleButton=button(playbackTheme,"⇄",playbackTheme.muted,playbackTheme.background); shuffleButton.setTypeface(Typeface.DEFAULT); shuffleButton.setTextSize(10); shuffleButton.setContentDescription("Toggle shuffle"); playbackControl(controls,shuffleButton,1,60);editTarget(shuffleButton,"Shuffle colours","player.accent","player.muted"); shuffleButton.setOnClickListener(v->{ if(player!=null) { player.setShuffle(!player.shuffle); updatePlayer(); Toast.makeText(this,player.shuffle?"Shuffle on · next track is random":"Shuffle off · play in order",Toast.LENGTH_SHORT).show(); } });
        TextView prev=button(playbackTheme,"|◀",playbackTheme.text,playbackTheme.background); prev.setContentDescription("Previous track"); prev.setTextSize(11);playbackControl(controls,prev,1,56); prev.setOnClickListener(v->{ if(player!=null) player.next(-1); });
        playButton=button(playbackTheme,"▶",playbackTheme.onAccent,playbackTheme.accent); playButton.setTextSize(22); playButton.setContentDescription("Play"); playbackControl(controls,playButton,1.2f,60); playButton.setOnClickListener(v->toggle());
        TextView next=button(playbackTheme,"▶|",playbackTheme.text,playbackTheme.background); next.setContentDescription("Next track"); next.setTextSize(11);playbackControl(controls,next,1,56); next.setOnClickListener(v->{ if(player!=null) player.next(1); });
        repeatButton=button(playbackTheme,"↻",playbackTheme.muted,playbackTheme.background); repeatButton.setTypeface(Typeface.DEFAULT); repeatButton.setTextSize(10); repeatButton.setContentDescription("Toggle repeat track"); playbackControl(controls,repeatButton,1,60);editTarget(repeatButton,"Loop colours","player.accent","player.muted"); repeatButton.setOnClickListener(v->{ if(player!=null) { player.setLoop(!player.repeat); updatePlayer(); Toast.makeText(this,player.repeat?"Loop on · keep the sound program running":"Loop off · fade and advance",Toast.LENGTH_SHORT).show(); } }); add(playerPanel,controls,-1,72);editTarget(controls,"Playback buttons","player.button","player.buttonText","player.accent");
        renderTracks(); updatePlayer();
        if(editState!=null){editTarget(root,"App background","background","text","button","buttonText","border");ThemeEditor.install(this,root,editTargets);}
    }
    public void renderTracks() {
        if(trackList==null) return;
        List<Library.Track> visible=currentTracks();
        trackList.removeAllViews(); listHeading.setText(showQueue?"QUEUE":"TRACKLIST");gameTitle.setText(showQueue?"Playback queue":showFavourites?"Favourite tracks":selected==null?"Your music library":selected.name);
        gameMeta.setText(showQueue?getResources().getQuantityString(R.plurals.track_count,visible.size(),visible.size())+" · hold to reorder or remove":showFavourites?getResources().getQuantityString(R.plurals.favourite_count,visible.size(),visible.size()):selected==null?"Import a track or a game set.":systemLabel(selected)+"  ·  "+getResources().getQuantityString(R.plurals.track_count,selected.tracks.size(),selected.tracks.size()));
        status.setText(busy?"IMPORTING…":!showFavourites && !showQueue && selected==null?"READY":String.format(Locale.ROOT,"%02d TRACKS",visible.size()));
        if(showFavourites && visible.isEmpty()) { TextView hint=text("No favourites yet. Tap a track’s ☆ to add it here.",14,listTheme.muted); hint.setPadding(dp(12),dp(24),dp(12),dp(24)); add(trackList,hint,-1,-2); return; }
        if(visible.isEmpty()) {
            LinearLayout empty=column(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(16),dp(22),dp(16),dp(22)); add(empty,pixel(showQueue?"QUEUE IS EMPTY":Playlists.saved(selected)?"PLAYLIST IS EMPTY":"INSERT SOUNDTRACK",9,listTheme.text),-2,-2); gap(empty,10); TextView hint=text(showQueue?"Use Add and queue to prepare your next tracks.":Playlists.saved(selected)?"Its source tracks are no longer in the library.":"Open an SPC track or a whole game set.\nYour music stays on your device.",12,listTheme.muted); hint.setGravity(Gravity.CENTER); add(empty,hint,-1,-2); gap(empty,18); TextView importButton=button("+  IMPORT MUSIC",listTheme.accent,listTheme.surface); importButton.setContentDescription("Import music file or game set"); add(empty,importButton,220,48); add(trackList,empty,-1,-2); importButton.setOnClickListener(v->pickFile()); return;
        }
        int playingAt=playingRow(visible);
        for(int i=0;i<visible.size();i++) {
            final int index=i; Library.Track track=visible.get(i); boolean active=i==playingAt;
            LinearLayout line=row(); if(active) line.setBackground(skin.selection(listTheme.surface)); line.setPadding(dp(8),dp(7),dp(8),dp(7));
            TextView number=pixel(active && (editState!=null?editState.playing:player.playing)?"▮▮":String.format(Locale.ROOT,"%02d",i+1),8,active?listTheme.accent:listTheme.muted); add(line,number,34,26);
            LinearLayout names=column(); TextView title=trackTitle(track.title,14,active || theme.id.equals("desktop-98")?listTheme.accent:listTheme.text); if(active) title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); add(names,title,-1,23); TextView sub=text(showQueue||Playlists.saved(selected)?(track.game.isEmpty()?track.system:track.game):showFavourites?track.game:track.artist.isEmpty()?track.system:track.artist,10,listTheme.muted); sub.setSingleLine(true); add(names,sub,-1,17); line.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            if(track.loopLength>0) {
                boolean looping=editState!=null&&active?editState.repeat:player!=null && player.isLooping(track);
                TextView mode=button(looping?"LOOP":"STRAIGHT",looping?listTheme.onAccent:listTheme.accent,looping?listTheme.accent:listTheme.surface);
                mode.setTextSize(6); mode.setContentDescription("Loop mode for "+track.title+": "+(looping?"looping":"straight")); mode.setEnabled(player!=null);
                add(line,mode,74,34);
                mode.setOnClickListener(v->{ if(player!=null) { player.setTrackLoop(track,!player.isLooping(track)); renderTracks(); updatePlayer(); } });
            }
            boolean starred=favourites.contains(track);
            TextView star=button(starred?"★":"☆",starred?listTheme.accent:listTheme.muted,backgroundColor); star.setBackground(null);star.setTextColor(starred?listTheme.accent:listTheme.muted); star.setPadding(0,0,0,0); star.setTypeface(Typeface.DEFAULT); star.setTextSize(23);
            star.setContentDescription((starred?"Remove ":"Add ")+track.title+(starred?" from favourites":" to favourites")); star.setSelected(starred); add(line,star,40,44);
            star.setOnClickListener(v->{ favourites.toggle(track); renderTracks(); });
            TextView duration=text(clock(track.length),11,listTheme.muted); duration.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); add(line,duration,43,30); add(trackList,line,-1,57); if(i==0)editTarget(line,"Tracklist entry","library.text","library.accent","library.muted","library.surface"); rule(trackList);
            line.setOnLongClickListener(v->{ if(showQueue)queueActions(index);else if(SoundFonts.midi(track.path)) dialog().setTitle(track.title).setItems(new String[]{"Choose SoundFont…","Export audio…"},(d,which)->{ if(which==0)chooseTrackSoundFont(track);else exportTrack(track); }).setNegativeButton("CANCEL",null).show();else exportTrack(track); return true; });
            line.setTooltipText(showQueue?"Hold to reorder or remove":"Hold to export audio");
            line.setContentDescription("Play "+track.title); line.setClickable(true); line.setFocusable(true); line.setOnClickListener(v->{ if(player!=null) { notifyPermission(); player.play(visible,index); } });
        }
    }
    private String systemLabel(Library.Game g) { return Playlists.saved(g)?"Saved playlist":LibraryPlatforms.platform(g); }
    private void toggle() {
        if(player==null) return;
        notifyPermission();
        if(player.current==null) { if(showQueue)pickFile();else if(showFavourites) { List<Library.Track> tracks=favourites.tracks(library.games); if(!tracks.isEmpty()) player.play(tracks,0); } else if(selected!=null) player.play(selected.tracks,0); else pickFile(); }
        else player.toggle();
    }
    private void notifyPermission() { if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2); }
    private void updatePlayer() {
        if(nowTitle==null) return;
        if(editState!=null){renderEditPlayer();return;}
        boolean loaded=player!=null && player.current!=null;
        seek.setEnabled(loaded);
        styleMode(shuffleButton,player!=null && player.shuffle,"⇄","Shuffle");
        styleMode(repeatButton,player!=null && player.repeat,"↻","Continuous loop");
        shuffleButton.setEnabled(player!=null); repeatButton.setEnabled(player!=null);
        updateVisualizer();
        if(!loaded) { nowTitle.setText(R.string.no_track_selected); nowArtist.setText(R.string.choose_track); playButton.setText("▶"); playButton.setContentDescription("Play"); seek.setProgress(0); time.setText(getString(R.string.position_and_duration,clock(0),"--:--")); return; }
        Library.Track t=player.current; nowTitle.setText(soundFonts.nowPlayingTitle(t)); nowArtist.setText(getString(R.string.artist_and_game,t.artist.isEmpty()?"Unknown artist":t.artist,t.game));
        playButton.setText(player.playing?"Ⅱ":"▶"); playButton.setContentDescription(player.playing?"Pause":"Play");
        if(!seeking) { seek.setMax(player.repeat?Math.max(t.length,player.position):t.length); seek.setProgress(Math.min(player.position,seek.getMax())); updateTime(player.position); }
        if(!player.error.isEmpty() && !player.error.equals(shownError)) { shownError=player.error; dialog().setTitle("Playback interrupted").setMessage(player.error).setPositiveButton("OK",null).show(); }
    }
    private void styleMode(TextView button,boolean enabled,String icon,String name) {
        android.text.SpannableString label=new android.text.SpannableString(icon+"\n"+(enabled?"ON":"OFF"));
        label.setSpan(new android.text.style.RelativeSizeSpan(2f),0,icon.length(),0);
        button.setText(label); button.setTextColor(enabled?playbackTheme.accent:playbackTheme.muted);
        button.setBackground(null);
        button.setSelected(enabled); button.setContentDescription(name+" "+(enabled?"on":"off"));
    }
    private void updateVisualizer() {
        if(editState!=null)return;
        if(visualizer!=null && player!=null) visualizer.setFrame(player.levels,player.waveform,player.playing && player.seekTarget<0,player.current!=null);
    }
    private void updateTime(int ms) { String end=player==null||player.current==null?"--:--":player.repeat?"∞ LOOP":clock(player.current.length); String label=getString(R.string.position_and_duration,clock(ms),end);
        if(player!=null && player.seekTarget>=0) label+="  ·  SEEKING → "+clock(player.seekTarget);
        time.setText(label); }
    public static String clock(int ms) { return String.format(Locale.ROOT,"%d:%02d",Math.max(ms,0)/60000,(Math.max(ms,0)/1000)%60); }
    private static boolean sameTrack(Library.Track a,Library.Track b){return a!=null&&b!=null&&a.index==b.index&&a.path.equals(b.path);}
    private int playingRow(List<Library.Track> tracks){
        Library.Track current=editState!=null?editState.current:player==null?null:player.current;if(current==null)return -1;
        if(editState==null&&player!=null){
            if(showQueue)return player.index;
            if(Playlists.saved(selected)&&!showFavourites&&tracks.size()==player.queue.size()){
                boolean aligned=true;for(int i=0;i<tracks.size();i++)if(!sameTrack(tracks.get(i),player.queue.get(i))){aligned=false;break;}if(aligned)return player.index;
            }
        }
        for(int i=0;i<tracks.size();i++)if(sameTrack(tracks.get(i),current))return i;return -1;
    }
    private List<Library.Track> currentTracks(){return editState!=null?editState.tracks:showQueue?(player==null?Collections.emptyList():player.queue):showFavourites?favourites.tracks(library.games):selected==null?Collections.emptyList():selected.tracks;}
    private List<Library.Game> savedPlaylists(){try{return new Playlists(this).all(library.games);}catch(Exception e){Toast.makeText(this,"Could not read saved playlists: "+e.getMessage(),Toast.LENGTH_LONG).show();return Collections.emptyList();}}
    private void selectLibraryGame(Library.Game game){selected=game;showFavourites=false;showQueue=false;getSharedPreferences("library-browser",MODE_PRIVATE).edit().putString("selected-playlist",Playlists.saved(game)?game.id:"").apply();}
    private void saveCurrentPlaylist(boolean reopen){
        List<Library.Track> snapshot=new ArrayList<>(currentTracks());if(snapshot.isEmpty())return;
        LinearLayout body=column();body.setPadding(dp(20),dp(8),dp(20),dp(8));
        Context themed=new ContextThemeWrapper(this,theme.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        EditText name=new EditText(themed);name.setSingleLine(true);name.setTextColor(textColor);name.setContentDescription("Playlist name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});if(!showQueue&&!showFavourites&&selected!=null)name.setText(selected.name);add(body,name,-1,56);
        add(body,text(getResources().getQuantityString(R.plurals.track_count,snapshot.size(),snapshot.size())+" · current order",12,mutedColor),-1,30);
        AlertDialog prompt=dialog().setTitle("Save playlist").setView(body).setNegativeButton("CANCEL",null).setPositiveButton("SAVE",null).show();
        prompt.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{
            String title=name.getText().toString().trim();if(title.isEmpty()){name.setError("Enter a playlist name");return;}
            Library.Game existing=null;for(Library.Game list:savedPlaylists())if(list.name.equalsIgnoreCase(title))existing=list;
            if(existing!=null){final String id=existing.id;prompt.dismiss();dialog().setTitle("Replace playlist?").setMessage("Replace “"+existing.name+"” with the current tracklist?").setNegativeButton("CANCEL",null).setPositiveButton("REPLACE",(d,w)->storePlaylist(id,title,snapshot,reopen)).show();}
            else try{new Playlists(this).save(null,title,snapshot);prompt.dismiss();playlistSaved(reopen);}catch(Exception e){name.setError(e.getMessage());}
        });
        name.requestFocus();name.selectAll();prompt.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    private void storePlaylist(String id,String name,List<Library.Track> tracks,boolean reopen){try{new Playlists(this).save(id,name,tracks);if(selected!=null&&id.equals(selected.id)){selected.name=name;selected.tracks=new ArrayList<>(tracks);}playlistSaved(reopen);}catch(Exception e){dialog().setTitle("Could not save playlist").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}}
    private void playlistSaved(boolean reopen){getSharedPreferences("library-browser",MODE_PRIVATE).edit().putBoolean("expanded:Playlists",true).apply();Toast.makeText(this,"Playlist saved",Toast.LENGTH_SHORT).show();renderTracks();if(reopen)chooseGame(true);}
    private View gameRow(Library.Game game) {
        LinearLayout line=row(); line.setPadding(dp(8),dp(10),dp(8),dp(10));
        LinearLayout names=column();  TextView title=text(game.name,16,listTheme.text); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setMaxLines(2); add(names,title,-1,-2); add(names,text(getString(R.string.library_entry,getResources().getQuantityString(R.plurals.track_count,game.tracks.size(),game.tracks.size()),systemLabel(game)),11,listTheme.muted),-1,-2); line.addView(names,new LinearLayout.LayoutParams(0,-2,1)); line.setContentDescription((Playlists.saved(game)?"Playlist ":"Soundtrack ")+game.name+". Hold for actions"); return line;
    }
    private void libraryActions(Library.Game game) {
        LinearLayout actions=column();actions.setPadding(dp(20),dp(8),dp(20),dp(8));
        TextView play=button("PLAY",accentColor,theme.surface);play.setContentDescription((Playlists.saved(game)?"Play playlist ":"Play soundtrack ")+game.name);add(actions,play,-1,48);gap(actions,8);
        TextView enqueue=button("ADD TO QUEUE",accentColor,theme.surface);enqueue.setContentDescription("Add "+game.name+" to queue");add(actions,enqueue,-1,48);
        gap(actions,8);TextView rename=button("RENAME",accentColor,theme.surface);rename.setContentDescription(Playlists.saved(game)?"Rename playlist "+game.name:"Rename "+game.name+" in library");add(actions,rename,-1,48);
        gap(actions,24);rule(actions);gap(actions,16);
        TextView delete=button("DELETE",textColor,theme.surface);delete.setContentDescription("Delete "+game.name+" from library");add(actions,delete,-1,48);
        AlertDialog menu=dialog().setTitle(game.name).setView(actions).setNegativeButton("CANCEL",null).show();
        play.setTextColor(accentColor);enqueue.setTextColor(accentColor);rename.setTextColor(accentColor);delete.setTextColor(theme.dark?0xffffa0a8:0xffa32e3b);
        play.setEnabled(player!=null&&!game.tracks.isEmpty());enqueue.setEnabled(player!=null&&!game.tracks.isEmpty());
        play.setOnClickListener(v->{if(player==null)return;menu.dismiss();if(libraryPicker!=null)libraryPicker.dismiss();selectLibraryGame(game);notifyPermission();player.play(game.tracks,0);renderTracks();});
        enqueue.setOnClickListener(v->{if(player==null)return;menu.dismiss();if(libraryPicker!=null)libraryPicker.dismiss();player.appendTracks(game.tracks);showQueue=true;showFavourites=false;renderTracks();});
        rename.setOnClickListener(v->{menu.dismiss();if(libraryPicker!=null)libraryPicker.dismiss();renameLibrary(game);});
        delete.setOnClickListener(v->{menu.dismiss();if(libraryPicker!=null)libraryPicker.dismiss();confirmDelete(game);});
    }
    private void renameLibrary(Library.Game game) {
        Context themed=new ContextThemeWrapper(this,theme.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        LinearLayout body=column();body.setPadding(dp(20),dp(8),dp(20),dp(8));
        EditText name=new EditText(themed);name.setSingleLine(true);name.setTextColor(textColor);name.setText(game.name);name.setContentDescription(Playlists.saved(game)?"Playlist name":"Library name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});add(body,name,-1,56);
        AlertDialog rename=dialog().setTitle(Playlists.saved(game)?"Rename playlist":"Rename library").setView(body).setNegativeButton("CANCEL",null).setPositiveButton("SAVE",null).show();
        rename.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{
            try {if(Playlists.saved(game)){new Playlists(this).rename(game.id,name.getText().toString());game.name=name.getText().toString().trim();if(selected!=null&&selected.id.equals(game.id))selected.name=game.name;}else library.rename(game,name.getText().toString());rename.dismiss();renderTracks();}
            catch(Exception e){name.setError(e.getMessage());}
        });
        name.requestFocus();name.selectAll();rename.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    private void chooseGame(){chooseGame(false);}
    private void chooseGame(boolean focusPlaylists) {
        List<LibraryPlatforms.Group> groups=LibraryPlatforms.group(library.games);
        LibraryPlatforms.Group playlists=new LibraryPlatforms.Group("Playlists");playlists.games.addAll(savedPlaylists());groups.add(playlists);
        ExpandableListView list=new ExpandableListView(this); list.setContentDescription("Soundtracks grouped by platform");
        list.setBackground(skin.well(listTheme.background)); list.setPadding(dp(6),dp(6),dp(6),dp(6)); list.setGroupIndicator(null);
        list.setDivider(new android.graphics.drawable.ColorDrawable(borderColor)); list.setDividerHeight(dp(1));
        list.setAdapter(new BaseExpandableListAdapter() {
            public int getGroupCount() { return groups.size(); }
            public int getChildrenCount(int group) { return groups.get(group).games.size(); }
            public Object getGroup(int group) { return groups.get(group); }
            public Object getChild(int group,int child) { return groups.get(group).games.get(child); }
            public long getGroupId(int group) { return group; }
            public long getChildId(int group,int child) { return child; }
            public boolean hasStableIds() { return false; }
            public boolean isChildSelectable(int group,int child) { return true; }
            public View getGroupView(int group,boolean expanded,View recycled,ViewGroup parent) {
                LibraryPlatforms.Group platform=groups.get(group);
                LinearLayout heading=row(); heading.setBackground(skin.selection(listTheme.background)); heading.setPadding(dp(10),dp(10),dp(10),dp(10)); heading.setMinimumHeight(dp(58));
                TextView arrow=text(expanded?"▾":"▸",20,listTheme.accent); arrow.setGravity(Gravity.CENTER); add(heading,arrow,28,30);
                LinearLayout labels=column(); TextView name=text(platform.name,16,listTheme.text); name.setTypeface(Typeface.DEFAULT,Typeface.BOLD); name.setMaxLines(2); add(labels,name,-1,-2);
                add(labels,text(getString(R.string.library_entry,platform==playlists?getResources().getQuantityString(R.plurals.playlist_count,platform.games.size(),platform.games.size()):getResources().getQuantityString(R.plurals.soundtrack_count,platform.games.size(),platform.games.size()),getResources().getQuantityString(R.plurals.track_count,platform.trackCount(),platform.trackCount())),11,listTheme.muted),-1,-2);
                heading.addView(labels,new LinearLayout.LayoutParams(0,-2,1)); heading.setContentDescription(platform.name+(expanded?", expanded":", collapsed")); return heading;
            }
            public View getChildView(int group,int child,boolean last,View recycled,ViewGroup parent) { return gameRow(groups.get(group).games.get(child)); }
        });
        list.setOnItemLongClickListener((parent,view,position,id)->{
            long packed=list.getExpandableListPosition(position);
            if(ExpandableListView.getPackedPositionType(packed)!=ExpandableListView.PACKED_POSITION_TYPE_CHILD)return false;
            libraryActions(groups.get(ExpandableListView.getPackedPositionGroup(packed)).games.get(ExpandableListView.getPackedPositionChild(packed)));return true;
        });
        android.content.SharedPreferences prefs=getSharedPreferences("library-browser",MODE_PRIVATE);
        String selectedPlatform=Playlists.saved(selected)?"Playlists":selected==null?"":LibraryPlatforms.platform(selected);
        for(int i=0;i<groups.size();i++) if(prefs.getBoolean("expanded:"+groups.get(i).name,groups.get(i).name.equals(selectedPlatform))) list.expandGroup(i);
        list.setOnGroupExpandListener(group->prefs.edit().putBoolean("expanded:"+groups.get(group).name,true).apply());
        list.setOnGroupCollapseListener(group->prefs.edit().putBoolean("expanded:"+groups.get(group).name,false).apply());
        Context themed=new ContextThemeWrapper(this,theme.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        Dialog picker=new Dialog(themed); picker.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout body=column(); body.setBackground(skin.chassis(backgroundColor)); body.setPadding(dp(12),dp(12),dp(12),dp(12));
        TextView title=pixel("Library",18,theme.heading); add(body,title,-1,44); add(body,text("Hold a soundtrack or playlist for actions",12,mutedColor),-1,24);gap(body,8);
        TextView save=button("SAVE CURRENT PLAYLIST",accentColor,theme.surface);save.setContentDescription("Save current playlist");save.setEnabled(!currentTracks().isEmpty());add(body,save,-1,44);gap(body,8);save.setOnClickListener(v->{picker.dismiss();saveCurrentPlaylist(true);});
        body.addView(list,new LinearLayout.LayoutParams(-1,0,1)); gap(body,8);
        LinearLayout actions=row(); TextView close=button("CLOSE",accentColor,backgroundColor),importMusic=button("IMPORT MUSIC",accentColor,backgroundColor);
        LinearLayout.LayoutParams closeParams=new LinearLayout.LayoutParams(0,dp(44),1); closeParams.setMargins(0,0,dp(8),0); actions.addView(close,closeParams);
        actions.addView(importMusic,new LinearLayout.LayoutParams(0,dp(44),2)); add(body,actions,-1,44);
        picker.setContentView(body,new ViewGroup.LayoutParams(-1,-1)); picker.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        close.setOnClickListener(v->picker.dismiss()); importMusic.setOnClickListener(v->{ picker.dismiss(); pickFile(); });
        libraryPicker=picker;
        list.setOnChildClickListener((parent,view,group,child,id)->{ selectLibraryGame(groups.get(group).games.get(child));picker.dismiss(); renderTracks(); return true; });
        picker.show(); picker.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(720)),Math.round(getResources().getDisplayMetrics().heightPixels*.86f));
        if(focusPlaylists)list.post(()->list.setSelectedGroup(groups.size()-1));
    }
    private Dialog appMenu;
    private void showMenu(View anchor) { showMenuPage("main"); }
    private void menuSection(LinearLayout body,String title) {
        gap(body,8);LinearLayout heading=row();heading.setPadding(dp(10),0,dp(10),0);
        TextView label=pixel(title,7,theme.value("heading",accentColor));add(heading,label,-2,28);View rail=new View(this);rail.setBackground(skin.rail(theme.secondary));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(5),1);params.leftMargin=dp(12);heading.addView(rail,params);add(body,heading,-1,28);
    }
    private void menuEntry(LinearLayout body,Dialog menu,String title,boolean enabled,boolean submenu,Runnable action) {
        LinearLayout line=row();line.setPadding(dp(14),0,dp(12),0);line.setMinimumHeight(dp(48));
        android.graphics.drawable.StateListDrawable state=new android.graphics.drawable.StateListDrawable();state.addState(new int[]{android.R.attr.state_pressed},skin.selection(listTheme.surface));state.addState(new int[]{android.R.attr.state_focused},skin.selection(listTheme.surface));state.addState(new int[]{},new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));line.setBackground(state);
        TextView label=text(title,16,enabled?listTheme.text:listTheme.muted);label.setSingleLine(true);line.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        if(submenu){TextView arrow=pixel("›",12,accentColor);add(line,arrow,24,40);}
        line.setContentDescription(title);line.setEnabled(enabled);line.setAlpha(enabled?1:.5f);line.setFocusable(enabled);line.setClickable(enabled);add(body,line,-1,48);
        if(enabled)line.setOnClickListener(v->{menu.dismiss();action.run();});
    }
    private void showMenuPage(String page) {
        if(appMenu!=null)appMenu.dismiss();
        Context themed=new ContextThemeWrapper(this,theme.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        Dialog menu=new Dialog(themed);menu.setCanceledOnTouchOutside(true);menu.requestWindowFeature(Window.FEATURE_NO_TITLE);appMenu=menu;
        LinearLayout body=column();body.setPadding(dp(12),dp(10),dp(12),dp(10));body.setBackground(skin.chassis(backgroundColor));
        LinearLayout heading=row();
        if(!page.equals("main")){TextView back=button("‹ BACK",accentColor,backgroundColor);back.setBackground(null);back.setTextColor(accentColor);back.setContentDescription("Back to app menu");add(heading,back,88,42);back.setOnClickListener(v->{menu.dismiss();showMenuPage("main");});}
        TextView title=pixel(page.equals("main")?"PLAYER MENU":page.equals("playback")?"PLAYBACK":"SETTINGS",11,theme.heading);heading.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));add(body,heading,-1,42);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setBackground(skin.well(listTheme.background));scroll.setPadding(dp(5),dp(4),dp(5),dp(4));LinearLayout entries=column();scroll.addView(entries);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page.equals("main")) {
            menuSection(entries,"MUSIC");
            menuEntry(entries,menu,"Import music",true,false,this::pickFile);
            menuEntry(entries,menu,"Library",true,false,this::chooseGame);
            menuEntry(entries,menu,showQueue?"Back to tracklist":"Playback queue",true,false,()->{showQueue=!showQueue;showFavourites=false;renderTracks();});
            menuEntry(entries,menu,"Save current playlist",!currentTracks().isEmpty(),false,()->saveCurrentPlaylist(false));
            menuEntry(entries,menu,showFavourites?"Back to tracklist":"Favourites",true,false,()->{showFavourites=!showFavourites;showQueue=false;renderTracks();});
            menuSection(entries,"PLAYER");menuEntry(entries,menu,"Playback tools",true,true,()->showMenuPage("playback"));
            menuSection(entries,"PREFERENCES");menuEntry(entries,menu,"Settings",true,true,()->showMenuPage("settings"));
            gap(entries,16);rule(entries);gap(entries,8);menuEntry(entries,menu,"Exit app",true,false,this::exitApp);
        } else if(page.equals("playback")) {
            menuSection(entries,"CURRENT TRACK");
            menuEntry(entries,menu,"Export current track…",player!=null&&player.current!=null,false,()->{if(player!=null&&player.current!=null)exportTrack(player.current);});
            menuEntry(entries,menu,"Track SoundFont…",player!=null&&player.current!=null&&SoundFonts.midi(player.current.path),false,()->{if(player!=null&&player.current!=null&&SoundFonts.midi(player.current.path))chooseTrackSoundFont(player.current);});
            menuSection(entries,"LISTENING");String sleep=player!=null&&player.sleepRemainingMs()>0?"Sleep timer · "+((player.sleepRemainingMs()+59999)/60000)+" min left":"Sleep timer";
            menuEntry(entries,menu,sleep,player!=null,false,this::chooseSleepTimer);
        } else {
            menuSection(entries,"LOOK & SOUND");menuEntry(entries,menu,"Color theme",true,false,this::chooseTheme);menuEntry(entries,menu,"SoundFonts",true,false,this::manageSoundFonts);menuEntry(entries,menu,"Custom themes",true,false,this::showSavedThemes);
            menuSection(entries,"APP");menuEntry(entries,menu,"About ChipAmp9x",true,false,this::about);
        }
        gap(body,10);TextView close=button("CLOSE",accentColor,backgroundColor);add(body,close,-1,44);close.setOnClickListener(v->menu.dismiss());
        menu.setContentView(body,new ViewGroup.LayoutParams(-1,-1));menu.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        if(!page.equals("main"))menu.setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP){menu.dismiss();showMenuPage("main");}return true;});
        menu.show();int height=dp(page.equals("main")?644:page.equals("playback")?356:404);menu.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(440)),Math.min(height,(int)(getResources().getDisplayMetrics().heightPixels*.86f)));
    }
    private void soundFontChanged() { soundFontChanged(null); }
    private void soundFontChanged(String affectedPath) {
        if(player!=null && player.current!=null && (affectedPath==null?soundFonts.overrideId(player.current.path).isEmpty():player.current.path.equals(affectedPath))) player.reloadSoundFont();
        Toast.makeText(this,"SoundFont saved",Toast.LENGTH_SHORT).show();
    }
    private void manageSoundFonts() {
        try {
            List<SoundFonts.Bank> banks=soundFonts.banks();String[] labels=new String[banks.size()];
            for(int i=0;i<labels.length;i++) { SoundFonts.Bank bank=banks.get(i);labels[i]=(bank.id.equals(soundFonts.defaultId())?"★ ":"")+bank.name+" · "+android.text.format.Formatter.formatShortFileSize(this,bank.file.length()); }
            dialog().setTitle("SoundFonts · choose a default").setItems(labels,(d,which)->bankActions(banks.get(which)))
                .setNeutralButton("IMPORT .SF2…",(d,w)->pickSoundFont()).setNegativeButton("CLOSE",null).show();
        } catch(Exception e) { soundFontError(e); }
    }
    private void bankActions(SoundFonts.Bank bank) {
        String[] actions=bank.bundled?new String[]{"Use as default"}:new String[]{"Use as default","Rename","Delete"};
        dialog().setTitle(bank.name).setItems(actions,(d,which)->{
            if(which==0) {soundFonts.setDefault(bank.id);soundFontChanged();manageSoundFonts();}
            else if(which==1) {
                EditText input=new EditText(this);input.setText(bank.name);input.setSingleLine(true);
                dialog().setTitle("Rename SoundFont").setView(input).setNegativeButton("CANCEL",null).setPositiveButton("SAVE",(x,w)->{
                    try {soundFonts.rename(bank,input.getText().toString());manageSoundFonts();}catch(Exception e){soundFontError(e);}
                }).show();
            } else dialog().setTitle("Delete SoundFont?").setMessage("Remove “"+bank.name+"” from ChipAmp9x? Tracks using this bank will use the default bank. Your original file is kept.")
                .setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(x,w)->{
                    try {soundFonts.delete(bank);soundFontChanged();manageSoundFonts();}catch(Exception e){soundFontError(e);}
                }).show();
        }).setNegativeButton("BACK",(d,w)->manageSoundFonts()).show();
    }
    private void chooseTrackSoundFont(Library.Track track) {
        try {
            List<SoundFonts.Bank> banks=soundFonts.banks();String[] labels=new String[banks.size()+1];labels[0]="Follow default bank";int checked=0;
            for(int i=0;i<banks.size();i++){labels[i+1]=banks.get(i).name;if(banks.get(i).id.equals(soundFonts.overrideId(track.path)))checked=i+1;}
            dialog().setTitle("SoundFont · "+track.title).setSingleChoiceItems(labels,checked,(d,which)->{
                soundFonts.setTrack(track.path,which==0?"":banks.get(which-1).id);d.dismiss();soundFontChanged(track.path);
            }).setNeutralButton("MANAGE BANKS",(d,w)->manageSoundFonts()).setNegativeButton("CANCEL",null).show();
        } catch(Exception e) { soundFontError(e); }
    }
    private void pickSoundFont() {
        if(busy) {Toast.makeText(this,"Wait for the current import",Toast.LENGTH_SHORT).show();return;}
        Toast.makeText(this,"Choose an uncompressed .sf2 bank · up to 64 MiB",Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),3);
    }
    private void importSoundFont(Uri uri) {
        if(busy) return;busy=true;renderTracks();
        importer.execute(()->{
            try {SoundFonts.Bank bank=soundFonts.importBank(uri);main.post(()->{busy=false;if(!isDestroyed()){renderTracks();Toast.makeText(this,"Imported "+bank.name,Toast.LENGTH_SHORT).show();manageSoundFonts();}});}
            catch(Exception e){main.post(()->{busy=false;if(!isDestroyed()){renderTracks();soundFontError(e);}});}
        });
    }
    private void soundFontError(Exception e) { dialog().setTitle("SoundFont unavailable").setMessage(e.getMessage()).setPositiveButton("OK",null).show(); }
    private void chooseSleepTimer() {
        String[] choices={"Off","15 minutes","30 minutes","60 minutes","90 minutes"};
        int[] minutes={0,15,30,60,90};
        dialog().setTitle("Sleep timer").setItems(choices,(d,which)->{
            if(player!=null) { player.setSleepTimer(minutes[which]*60000L); Toast.makeText(this,which==0?"Sleep timer off":"Playback will pause in "+minutes[which]+" minutes",Toast.LENGTH_SHORT).show(); }
        }).setNegativeButton("CANCEL",null).show();
    }
    private void exportTrack(Library.Track track) {
        if(track==null) return;
        startActivity(new Intent(this,ExportActivity.class).putExtra("path",track.path).putExtra("title",track.title).putExtra("index",track.index).putExtra("length",track.length));
    }
    private void exitApp() {
        if(busy) { Toast.makeText(this,"Wait for the import to finish before exiting",Toast.LENGTH_SHORT).show(); return; }
        if(player!=null) player.stopPlayback(()->finishAndRemoveTask());
        else { stopService(new Intent(this,PlaybackService.class)); finishAndRemoveTask(); }
    }
    private void confirmDelete(Library.Game game) {
        if(Playlists.saved(game)){
            dialog().setTitle("Delete playlist?").setMessage("Delete “"+game.name+"”? Your music files stay in the library.").setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{
                try{new Playlists(this).delete(game.id);if(selected!=null&&selected.id.equals(game.id))selectLibraryGame(library.games.isEmpty()?null:library.games.get(library.games.size()-1));renderTracks();}
                catch(Exception e){dialog().setTitle("Could not delete playlist").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
            }).show();return;
        }
        if(busy) { Toast.makeText(this,"Wait for the import to finish before deleting",Toast.LENGTH_SHORT).show(); return; }
        dialog().setTitle("Delete soundtrack?").setMessage("Remove “"+game.name+"” and its "+game.tracks.size()+" tracks from your library? Your original files will be kept.")
            .setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{
                Runnable remove=()->{
                    try {
                        library.remove(game);
                        if(selected==game) selectLibraryGame(library.games.isEmpty()?null:library.games.get(Math.max(0,library.games.size()-1)));else if(Playlists.saved(selected))for(Library.Game list:savedPlaylists())if(list.id.equals(selected.id))selected=list;
                        renderTracks(); updatePlayer();
                    } catch(Exception e) { dialog().setTitle("Could not delete soundtrack").setMessage(e.getMessage()).setPositiveButton("OK",null).show(); }
                };
                boolean queued=player!=null && player.queue.stream().anyMatch(t->game.tracks.stream().anyMatch(g->g.path.equals(t.path)));
                if(queued) player.stopPlayback(remove); else remove.run();
            }).show();
    }
    private void pickFile() {
        if(busy) { Toast.makeText(this,"A soundtrack is already importing",Toast.LENGTH_SHORT).show(); return; }
        startActivityForResult(new Intent(this,ImportActivity.class),4);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if((request==5||request==6)&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();final String exportId=pendingThemeExport;busy=true;renderTracks();importer.execute(()->{try{
                if(request==5){try(java.io.InputStream input=getContentResolver().openInputStream(uri)){ThemeStore.Draft draft=ThemeStore.read(input);new ThemeStore(this).save(draft);main.post(()->{busy=false;if(!isDestroyed()){selectTheme(draft.id);showSavedThemes();}});}}
                else {ThemeStore.Draft draft=null;for(ThemeStore.Draft d:new ThemeStore(this).drafts())if(d.id.equals(exportId))draft=d;if(draft==null)throw new java.io.IOException("Theme no longer exists");try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("Cannot save theme");out.write(ThemeStore.encode(draft).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}main.post(()->{busy=false;pendingThemeExport=null;if(!isDestroyed()){renderTracks();Toast.makeText(this,"Theme exported",Toast.LENGTH_SHORT).show();}});}
            }catch(Exception e){main.post(()->{busy=false;if(!isDestroyed()){renderTracks();ThemeLibrary.error(this,e.getMessage());}});}});
        }
        if(request==4&&result==RESULT_OK&&data!=null) {
            try {
                ArrayList<String> ids=data.getStringArrayListExtra(ImportActivity.IDS);library=new Library(this);
                if(ids!=null) {
                    for(Library.Game game:library.games)if(ids.contains(game.id))selectLibraryGame(game);
                    if(data.getBooleanExtra(ImportActivity.QUEUE,false)){pendingQueueIds.addAll(ids);showQueue=true;}else showQueue=false;
                }
                showFavourites=false;flushPendingQueue();renderTracks();updatePlayer();
            } catch(Exception e){dialog().setTitle("Could not refresh library").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
        }
        if(request==1 && result==RESULT_OK && data!=null && data.getData()!=null) importUri(data.getData());
        if(request==3 && result==RESULT_OK && data!=null && data.getData()!=null) importSoundFont(data.getData());
    }
    private void flushPendingQueue() {
        if(player==null||pendingQueueIds.isEmpty()||library==null)return;
        List<Library.Track> tracks=new ArrayList<>();for(String id:pendingQueueIds)for(Library.Game game:library.games)if(game.id.equals(id))tracks.addAll(game.tracks);
        player.appendTracks(tracks);pendingQueueIds.clear();
    }
    private void queueActions(int at) {
        if(player==null||at>=player.queue.size())return;Library.Track track=player.queue.get(at);
        List<String> labels=new ArrayList<>();if(at>0)labels.add("Move up");if(at+1<player.queue.size())labels.add("Move down");labels.add("Remove from queue");if(SoundFonts.midi(track.path))labels.add("Choose SoundFont…");labels.add("Export audio…");
        dialog().setTitle(track.title).setItems(labels.toArray(new String[0]),(d,which)->{
            String action=labels.get(which);if(player==null)return;
            if(action.equals("Move up"))player.moveQueued(at,at-1);else if(action.equals("Move down"))player.moveQueued(at,at+1);else if(action.equals("Remove from queue"))player.removeQueued(at);else if(action.equals("Choose SoundFont…"))chooseTrackSoundFont(track);else exportTrack(track);renderTracks();
        }).setNegativeButton("CANCEL",null).show();
    }
    void importUri(Uri uri) {
        busy=true; renderTracks();
        importer.execute(()->{
            try { Library.Game game=library.importFile(uri); main.post(()->{ busy=false; selectLibraryGame(game); if(!isDestroyed()) { renderTracks(); Toast.makeText(this,"Imported "+game.tracks.size()+" tracks",Toast.LENGTH_SHORT).show(); } }); }
            catch(Exception e) { main.post(()->{ busy=false; if(!isDestroyed()) { renderTracks(); dialog().setTitle("Could not import soundtrack").setMessage(e.getMessage()).setPositiveButton("OK",null).show(); } }); }
        });
    }
    private void applyPalette(PlayerTheme palette) {
        theme=palette; playbackTheme=palette.nowPlaying(); listTheme=palette.library(); backgroundColor=palette.background; textColor=palette.text; mutedColor=palette.muted;
        accentColor=palette.accent; borderColor=palette.border;
    }
    void selectTheme(String id) {
        applyPalette(ThemeStore.find(this,id));
        getSharedPreferences("appearance",MODE_PRIVATE).edit().putString("theme",theme.id).apply();
        seeking=false; buildUI(); if(player!=null) player.refreshAppearance();
    }
    private void chooseTheme() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false);
        GridLayout grid=new GridLayout(this); grid.setColumnCount(2); grid.setPadding(dp(12),dp(4),dp(12),dp(12)); scroll.addView(grid);
        AlertDialog picker=dialog().setTitle("Choose your palette").setView(scroll).setNegativeButton("CANCEL",null).create();
        List<PlayerTheme> previews=new ArrayList<>(new ThemeStore(this).all());
        PlayerTheme desktop=PlayerTheme.find("desktop-98"); previews.remove(desktop); previews.add(0,desktop);
        for(int i=0;i<previews.size();i++) {
            PlayerTheme palette=previews.get(i); ThemePreview preview=new ThemePreview(this,palette,palette.id.equals(theme.id));
            GridLayout.LayoutParams params=new GridLayout.LayoutParams(GridLayout.spec(i/2),GridLayout.spec(i%2,1f));
            params.width=0; params.height=dp(150); params.setMargins(dp(4),dp(4),dp(4),dp(4)); grid.addView(preview,params);
            preview.setOnClickListener(v->{ picker.dismiss(); selectTheme(palette.id); });
        }
        picker.setButton(DialogInterface.BUTTON_NEUTRAL,"EDIT THEME",(d,w)->beginThemeEdit(ThemeStore.from(theme,false)));
        picker.setButton(DialogInterface.BUTTON_POSITIVE,"SAVED THEMES",(d,w)->showSavedThemes());
                picker.show(); picker.getWindow().setBackgroundDrawable(skin.chassis(backgroundColor)); tintDialog(picker.getWindow().getDecorView()); picker.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(accentColor);
        picker.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(560)),(int)(getResources().getDisplayMetrics().heightPixels*.85f));
    }
    @Override public Object onRetainNonConfigurationInstance(){return editState;}
    @Override public void onBackPressed(){if(editState!=null)cancelThemeEdit();else super.onBackPressed();}
    ThemeStore.Draft editDraft(){return editState==null?null:editState.draft;}
    void beginThemeEdit(ThemeStore.Draft draft){
        if(editState!=null)return;if(busy){Toast.makeText(this,"Finish the import before editing a theme",Toast.LENGTH_SHORT).show();return;}editState=new ThemeEditor.State();editState.draft=draft;
        editState.tracks=new ArrayList<>(showQueue?(player==null?Collections.emptyList():player.queue):showFavourites?favourites.tracks(library.games):selected==null?Collections.emptyList():selected.tracks);
        if(player!=null){editState.current=player.current;editState.position=player.position;editState.playing=player.playing;editState.repeat=player.repeat;editState.shuffle=player.shuffle;editState.levels=player.levels.clone();editState.waveform=player.waveform.clone();}
        float sum=0;for(float f:editState.levels)sum+=f;if(sum<.01f)for(int i=0;i<editState.levels.length;i++)editState.levels[i]=.2f+(i*7%13)/18f;
        float energy=0;for(float value:editState.waveform)energy+=Math.abs(value);
        if(energy<.01f)for(int i=0;i<AudioWaveform.POINTS;i++){editState.waveform[i*2]=.6f*(float)Math.sin(i*.18);editState.waveform[i*2+1]=.6f*(float)Math.cos(i*.18);}
        main.removeCallbacks(tick);repaintTheme();
    }
    private void renderEditPlayer(){
        Library.Track track=editState.current;nowTitle.setText(track==null?"Theme preview":soundFonts.nowPlayingTitle(track));nowArtist.setText(track==null?"Tap a part to edit its colours":track.artist+" · "+track.game);
        seek.setMax(track==null?180000:track.length);seek.setProgress(editState.position);time.setText(getString(R.string.position_and_duration,clock(editState.position),clock(track==null?180000:track.length)));
        playButton.setText(editState.playing?"Ⅱ":"▶");styleMode(shuffleButton,editState.shuffle,"⇄","Shuffle");styleMode(repeatButton,editState.repeat,"↻","Continuous loop");visualizer.setFrozenFrame(editState.levels,editState.waveform);
    }
    private final Runnable repaintEdit=()->{if(editState!=null){applyPalette(editState.draft.theme());buildUI();}};
    void paintTheme(String role,int color){if(editState==null)return;editState.draft.colors.put(role,color);main.removeCallbacks(repaintEdit);main.postDelayed(repaintEdit,80);}
    void repaintTheme(){main.removeCallbacks(repaintEdit);repaintEdit.run();}
    void cancelThemeEdit(){main.removeCallbacks(repaintEdit);for(Dialog d:new ArrayList<>(editorDialogs))d.dismiss();editState=null;applyPalette(ThemeStore.selected(this));buildUI();main.removeCallbacks(tick);main.post(tick);}
    void saveThemeEdit(){
        if(editState==null)return;LinearLayout body=ThemeEditor.column(this);EditText name=new EditText(new ContextThemeWrapper(this,android.R.style.Theme_Material_Dialog_Alert));name.setTextColor(ThemeEditor.FG);name.setSingleLine(true);name.setText(editState.draft.name);name.setContentDescription("Theme name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});body.addView(name);
        TextView save=ThemeEditor.key(this,"SAVE THEME");body.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));Dialog prompt=ThemeEditor.window(this,"NAME YOUR THEME",body);save.setOnClickListener(v->{try{ThemeStore.Draft draft=editState.draft;draft.name=name.getText().toString();new ThemeStore(this).save(draft);String id=draft.id;prompt.dismiss();editState=null;main.removeCallbacks(repaintEdit);selectTheme(id);main.removeCallbacks(tick);main.post(tick);}catch(Exception e){name.setError(e.getMessage());}});ThemeEditor.show(prompt);
    }
    void showSavedThemes(){ThemeLibrary.show(this);}
    void exportTheme(ThemeStore.Draft draft){pendingThemeExport=draft.id;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,draft.name.replaceAll("[^a-zA-Z0-9 _-]","_")+".chiptheme.json"),6);}
    void importTheme(){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),5);}
    private AlertDialog.Builder dialog() {
        Context themed=new ContextThemeWrapper(this,theme.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        return new AlertDialog.Builder(themed) {
            @Override public AlertDialog show() {
                AlertDialog result=super.show();
                result.getWindow().setBackgroundDrawable(skin.chassis(backgroundColor));
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
        dialog().setTitle("ChipAmp9x · v1.20.0").setMessage("A pocket home for retro soundtracks.\n\nOpen SPC tracks, ZIP sets, RAR-based RSN sets PlayStation PSF/minipsf files and Nintendo 64 USF/miniusf files. Import dependent PSF or USF tracks as a ZIP or folder containing their .psflib or .usflib files. MIDI (.mid/.midi) uses the built-in Pixel GM bank or imported .sf2 banks (up to 64 MiB). Open SoundFonts from the menu to manage banks; hold a MIDI track to choose its bank. MIDI type 0/1 with PPQ timing is supported. Game-specific banks may have custom instrument mappings. Also plays NSF/NSFE (NES), GBS (Game Boy), VGM/VGZ (many systems), GYM, HES, KSS, AY and SAP. Multi-song files become individual tracks.\n\nNo ROMs or music are included. Imports are copied into private app storage; no network or broad storage permission is used. Tracks without timing tags play for 3 minutes, followed by a fade. Enable Loop to let an SPC sound program loop naturally without a timed restart. One-shot tracks without a programmed loop may become silent. Shuffle changes the next track, not the current song. The visualizer follows decoded audio and settles when paused.\n\nAudio: Game Music Emu (LGPL 2.1+). PSF: Audio Overload / PeOPS (BSD and GPL 2+). N64: LazyUSF2 / Mupen64Plus (GPL 2+ and CC0). RSN: unarr (LGPL 3). MIDI: TinySoundFont (MIT), original synthetic Pixel GM bank (MIT). Combined build: GPL 3; original app source: MIT. Fonts: Press Start 2P and Doto (SIL OFL). Original pixel art.").setNeutralButton("LICENSES",(d,w)->licenses()).setPositiveButton("LET’S PLAY",null).show();
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
