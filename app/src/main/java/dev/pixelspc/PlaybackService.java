package dev.pixelspc;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import java.util.*;

/** One worker owns the emulator and AudioTrack. UI operations never race native calls. */
public class PlaybackService extends Service {
    public class LocalBinder extends Binder { public PlaybackService get() { return PlaybackService.this; } }
    private final LocalBinder binder=new LocalBinder();
    private Handler worker,main;
    private HandlerThread thread;
    private AudioTrack audio;
    private long handle;
    private Library.Track loaded;
    private final PlaybackSeek seeking=new PlaybackSeek();
    private int pcmOffset=4096;
    private boolean pcmEnded;
    private long lastSeekUpdate;
    private MediaSession session;
    private AudioManager manager;
    private AudioFocusRequest focus;
    private boolean resumeOnFocus;
    private boolean defaultLoop;
    private volatile long sleepUntil;
    private volatile boolean sleepExpired;
    private volatile boolean destroyed;
    private final short[] pcm=new short[4096];
    private final AudioSpectrum spectrum=new AudioSpectrum();
    public volatile float[] levels=new float[AudioSpectrum.BANDS];
    public volatile boolean playing=false,repeat=false,shuffle=false;
    public volatile int position=0,index=-1,seekTarget=-1;
    public volatile String error="";
    public volatile Library.Track current;
    public List<Library.Track> queue=Collections.emptyList();
    public Runnable listener;

    @Override public void onCreate() {
        super.onCreate(); main=new Handler(getMainLooper()); thread=new HandlerThread("SPC audio"); thread.start(); worker=new Handler(thread.getLooper());
        shuffle=getSharedPreferences("playback",MODE_PRIVATE).getBoolean("shuffle",false);
        repeat=defaultLoop=getSharedPreferences("playback",MODE_PRIVATE).getBoolean("loop",false);
        manager=(AudioManager)getSystemService(AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{
            if(change==AudioManager.AUDIOFOCUS_GAIN) { if(resumeOnFocus) { resumeOnFocus=false; toggle(); } }
            else { resumeOnFocus=playing && change!=AudioManager.AUDIOFOCUS_LOSS; if(playing) pause(); }
        },main).build();
        session=new MediaSession(this,"ChipPlayer 199x"); session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { if(!playing) toggle(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { next(1); }
            @Override public void onSkipToPrevious() { next(-1); }
            @Override public void onSeekTo(long ms) { seek((int)ms); }
            @Override public void onStop() { stopPlayback(null); }
            @Override public void onCustomAction(String action,Bundle extras) { if("exit".equals(action)) exitApp(); }
        }); session.setActive(true);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("music","Music playback",NotificationManager.IMPORTANCE_LOW));
        registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    private final BroadcastReceiver noisy=new BroadcastReceiver() { public void onReceive(Context c,Intent i) { pause(); } };
    @Override public IBinder onBind(Intent i) { return binder; }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent!=null) { String action=intent.getAction(); if("toggle".equals(action)) toggle(); else if("next".equals(action)) next(1); else if("previous".equals(action)) next(-1); else if("stop".equals(action)) stopPlayback(null); else if("exit".equals(action)) exitApp(); }
        return START_NOT_STICKY;
    }
    public void play(List<Library.Track> tracks,int selected) {
        if(selected<0 || selected>=tracks.size()) return;
        sleepExpired=false; queue=new ArrayList<>(tracks); index=selected; current=queue.get(selected); error="";
        final Library.Track track=current;
        repeat=isLooping(track);
        startService(new Intent(this,PlaybackService.class));
        startForeground(1,notification());
        if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { error="Audio is in use by another app"; changed(); return; }
        worker.post(()->{
            worker.removeCallbacks(pump); releaseAudio();
            try {
                loaded=track; handle=Engine.open(track.path); Engine.start(handle,track.index,track.length); Engine.setLoop(handle,repeat,track.length);
                int minimum=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
                audio=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(Math.max(minimum,16384)).setTransferMode(AudioTrack.MODE_STREAM).build();
                position=0; playing=true; audio.play(); worker.post(pump); changed();
            } catch(Exception e) { failed(e); }
        });
    }
    public void toggle() {
        if(current==null) return;
        if(playing) { pause(); return; }
        if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;
        sleepExpired=false; startForeground(1,notification());
        worker.post(()->{
            if(audio==null) { main.post(()->play(queue,index)); return; }
            try { playing=true; audio.play(); worker.removeCallbacks(pump); worker.post(pump); changed(); }
            catch(Exception e) { failed(e); }
        });
    }
    public void pause() { worker.post(()->{ cancelSeek(); playing=false; worker.removeCallbacks(pump); if(audio!=null) audio.pause(); changed(); }); }
    public void stopPlayback(Runnable complete) {
        setSleepTimer(0);
        current=null; queue=Collections.emptyList(); index=-1; playing=false; position=0; seekTarget=-1;
        resumeOnFocus=false; manager.abandonAudioFocusRequest(focus);
        worker.removeCallbacks(pump);
        worker.post(()->{
            releaseAudio(); position=0;
            main.post(()->{
                if(!destroyed) {
                    session.setMetadata(null);
                    session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED,0,0).build());
                    stopForeground(true); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1); stopSelf(); changed();
                }
                if(complete!=null) complete.run();
            });
        });
    }
    private void exitApp() {
        stopPlayback(()->{
            ActivityManager tasks=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
            for(ActivityManager.AppTask task:tasks.getAppTasks()) task.finishAndRemoveTask();
        });
    }
    public void refreshAppearance() { changed(); }
    public long sleepRemainingMs() { return Math.max(0,sleepUntil-SystemClock.elapsedRealtime()); }
    public void setSleepTimer(long durationMs) {
        main.removeCallbacks(sleepCheck); sleepExpired=false;
        sleepUntil=durationMs<=0?0:SystemClock.elapsedRealtime()+Math.min(durationMs,90*60000L);
        if(sleepUntil>0) main.postDelayed(sleepCheck,Math.min(1000,durationMs));
    }
    private final Runnable sleepCheck=new Runnable() { public void run() {
        if(destroyed || sleepUntil==0) return;
        if(sleepRemainingMs()==0) { sleepExpired=true; sleepUntil=0; resumeOnFocus=false; pause(); }
        else main.postDelayed(this,Math.min(1000,sleepRemainingMs()));
    }};
    public void setShuffle(boolean enabled) {
        shuffle=enabled; getSharedPreferences("playback",MODE_PRIVATE).edit().putBoolean("shuffle",enabled).apply(); changed();
    }
    public void setLoop(boolean enabled) {
        defaultLoop=enabled; getSharedPreferences("playback",MODE_PRIVATE).edit().putBoolean("loop",enabled).apply();
        if(current!=null && current.loopLength>0) getSharedPreferences("playback",MODE_PRIVATE).edit().putBoolean(loopKey(current),enabled).apply();
        applyLoop(enabled);
    }
    private String loopKey(Library.Track track) { return "track-loop:"+track.path+":"+track.index; }
    public boolean isLooping(Library.Track track) { return getSharedPreferences("playback",MODE_PRIVATE).getBoolean(loopKey(track),defaultLoop); }
    public void setTrackLoop(Library.Track track,boolean enabled) {
        getSharedPreferences("playback",MODE_PRIVATE).edit().putBoolean(loopKey(track),enabled).apply();
        if(current!=null && current.path.equals(track.path) && current.index==track.index) applyLoop(enabled);
        else changed();
    }
    private void applyLoop(boolean enabled) {
        repeat=enabled;
        worker.post(()->{ if(handle!=0 && loaded!=null) Engine.setLoop(handle,enabled,loaded.length); changed(); });
    }
    public int nextIndex(int delta) {
        if(queue.isEmpty()) return -1;
        return shuffle && delta>0 && queue.size()>1 ? (index+1+new Random().nextInt(queue.size()-1))%queue.size() : (index+delta+queue.size())%queue.size();
    }
    public void next(int delta) {
        int n=nextIndex(delta); if(n>=0) play(queue,n);
    }
    public void seek(int ms) {
        final Library.Track requested=current;
        worker.post(()->{
            if(handle==0 || loaded==null || loaded!=requested) return;
            try {
                cancelSeek(); worker.removeCallbacks(pump);
                if(audio!=null) { audio.pause(); audio.flush(); }
                pcmOffset=pcm.length; pcmEnded=false;
                seekTarget=Math.max(0,Math.min(ms,repeat?Math.max(loaded.length,position):loaded.length));
                seeking.begin(seekTarget); levels=new float[AudioSpectrum.BANDS];
                changed(); worker.post(seekStep);
            } catch(Exception e) { failed(e); }
        });
    }
    private void cancelSeek() { seekTarget=-1; seeking.cancel(); worker.removeCallbacks(seekStep); }
    private final Runnable seekStep=new Runnable() { public void run() {
        if(!seeking.active() || handle==0) return;
        try {
            seeking.step(new PlaybackSeek.Decoder() {
                public int tell() { return Engine.tell(handle); }
                public void seek(int ms) throws java.io.IOException { Engine.seek(handle,ms); }
                public void restoreMode() { Engine.setLoop(handle,repeat,loaded.length); }
            });
            position=Engine.tell(handle);
            if(!seeking.active()) seekTarget=-1;
            long now=SystemClock.elapsedRealtime();
            if(!seeking.active() || now-lastSeekUpdate>=200) { lastSeekUpdate=now; changed(); }
            if(seeking.active()) worker.post(this);
            else if(playing && audio!=null) { audio.play(); worker.post(pump); }
        } catch(Exception e) { failed(e); }
    }};
    private final Runnable pump=new Runnable() { public void run() {
        if(!playing || handle==0) return;
        try {
            if(pcmOffset==pcm.length) {
                pcmEnded=Engine.render(handle,pcm); pcmOffset=0;
                levels=spectrum.analyze(pcm);
            }
            // Never block the control worker waiting for the device's audio sink.
            int n=audio.write(pcm,pcmOffset,pcm.length-pcmOffset,AudioTrack.WRITE_NON_BLOCKING);
            if(n<0) throw new java.io.IOException("Audio output unavailable ("+n+")");
            pcmOffset+=n;
            position=Engine.tell(handle);
            if(pcmOffset==pcm.length && pcmEnded) {
                playing=false;
                main.post(()->{
                    if(sleepExpired) return;
                    if(sleepUntil>0 && sleepRemainingMs()==0) { setSleepTimer(0); sleepExpired=true; pause(); }
                    else if(repeat) play(queue,index); else next(1);
                });
            }
            else if(pcmOffset<pcm.length) worker.postDelayed(this,10);
            else worker.post(this);
        } catch(Exception e) { failed(e); }
    }};
    private void failed(Exception e) { error="Cannot play this track: "+e.getMessage(); playing=false; releaseAudio(); changed(); }
    private void releaseAudio() { cancelSeek(); worker.removeCallbacks(pump); pcmOffset=pcm.length; pcmEnded=false; loaded=null; playing=false; levels=new float[AudioSpectrum.BANDS]; if(audio!=null) { audio.pause(); audio.flush(); audio.release(); audio=null; } if(handle!=0) { Engine.close(handle); handle=0; } }
    private void changed() { main.post(()->{
        if(destroyed) return;
        if(current!=null) {
            session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,current.title).putString(MediaMetadata.METADATA_KEY_ARTIST,current.artist).putString(MediaMetadata.METADATA_KEY_ALBUM,current.game).putLong(MediaMetadata.METADATA_KEY_DURATION,repeat?-1:current.length).build());
            session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO).addCustomAction("exit","Exit app",R.drawable.ic_close).setState(playing?(seekTarget>=0?PlaybackState.STATE_BUFFERING:PlaybackState.STATE_PLAYING):PlaybackState.STATE_PAUSED,position,playing && seekTarget<0?1:0).build());
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1,notification());
        }
        if(listener!=null) listener.run();
    }); }
    private PendingIntent action(String name,int code) { return PendingIntent.getService(this,code,new Intent(this,PlaybackService.class).setAction(name),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT); }
    private Notification notification() {
        return new Notification.Builder(this,"music").setSmallIcon(dev.pixelspc.R.drawable.ic_player).setColorized(false).setSubText("ChipPlayer 199x").setContentTitle(current==null?"ChipPlayer 199x":current.title).setContentText(current==null?"Your retro soundtrack":current.game).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(1,2,3)).addAction(android.R.drawable.ic_media_previous,"Previous",action("previous",1)).addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pause":"Play",action("toggle",2)).addAction(android.R.drawable.ic_media_next,"Next",action("next",3)).addAction(R.drawable.ic_close,"Exit app",action("exit",5)).setDeleteIntent(action("stop",4)).setOngoing(playing).build();
    }
    @Override public void onDestroy() {
        destroyed=true; main.removeCallbacks(sleepCheck); sleepUntil=0;
        unregisterReceiver(noisy); listener=null; manager.abandonAudioFocusRequest(focus); session.release();
        worker.removeCallbacksAndMessages(null); worker.post(()->{ releaseAudio(); thread.quitSafely(); }); super.onDestroy();
    }
}
