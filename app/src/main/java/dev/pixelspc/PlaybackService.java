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
    private MediaSession session;
    private AudioManager manager;
    private AudioFocusRequest focus;
    private boolean resumeOnFocus;
    private final short[] pcm=new short[4096];
    public volatile boolean playing=false,repeat=false,shuffle=false;
    public volatile int position=0,index=-1;
    public volatile String error="";
    public volatile Library.Track current;
    public List<Library.Track> queue=Collections.emptyList();
    public Runnable listener;

    @Override public void onCreate() {
        super.onCreate(); main=new Handler(getMainLooper()); thread=new HandlerThread("SPC audio"); thread.start(); worker=new Handler(thread.getLooper());
        manager=(AudioManager)getSystemService(AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{
            if(change==AudioManager.AUDIOFOCUS_GAIN) { if(resumeOnFocus) { resumeOnFocus=false; toggle(); } }
            else { resumeOnFocus=playing && change!=AudioManager.AUDIOFOCUS_LOSS; if(playing) pause(); }
        },main).build();
        session=new MediaSession(this,"PixelSPC"); session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { if(!playing) toggle(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { next(1); }
            @Override public void onSkipToPrevious() { next(-1); }
            @Override public void onSeekTo(long ms) { seek((int)ms); }
            @Override public void onStop() { pause(); stopForeground(true); stopSelf(); }
        }); session.setActive(true);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("music","Music playback",NotificationManager.IMPORTANCE_LOW));
        registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    private final BroadcastReceiver noisy=new BroadcastReceiver() { public void onReceive(Context c,Intent i) { pause(); } };
    @Override public IBinder onBind(Intent i) { return binder; }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent!=null) { String action=intent.getAction(); if("toggle".equals(action)) toggle(); else if("next".equals(action)) next(1); else if("previous".equals(action)) next(-1); else if("stop".equals(action)) { pause(); stopForeground(true); stopSelf(); } }
        return START_NOT_STICKY;
    }
    public void play(List<Library.Track> tracks,int selected) {
        if(selected<0 || selected>=tracks.size()) return;
        queue=new ArrayList<>(tracks); index=selected; current=queue.get(selected); error="";
        final Library.Track track=current;
        startService(new Intent(this,PlaybackService.class));
        startForeground(1,notification());
        if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { error="Audio is in use by another app"; changed(); return; }
        worker.post(()->{
            worker.removeCallbacks(pump); releaseAudio();
            try {
                handle=Engine.open(track.path); Engine.start(handle,track.index,track.length);
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
        startForeground(1,notification());
        worker.post(()->{ if(audio!=null) { playing=true; audio.play(); worker.removeCallbacks(pump); worker.post(pump); changed(); } });
    }
    public void pause() { worker.post(()->{ playing=false; worker.removeCallbacks(pump); if(audio!=null) audio.pause(); changed(); }); }
    public void next(int delta) {
        if(queue.isEmpty()) return;
        int n=shuffle && delta>0 && queue.size()>1 ? (index+1+new Random().nextInt(queue.size()-1))%queue.size() : (index+delta+queue.size())%queue.size();
        play(queue,n);
    }
    public void seek(int ms) {
        worker.post(()->{
            if(handle==0 || current==null) return;
            try { if(audio!=null) { audio.pause(); audio.flush(); } Engine.seek(handle,Math.max(0,Math.min(ms,current.length))); position=Engine.tell(handle); if(playing && audio!=null) audio.play(); changed(); }
            catch(Exception e) { failed(e); }
        });
    }
    private final Runnable pump=new Runnable() { public void run() {
        if(!playing || handle==0) return;
        try {
            boolean ended=Engine.render(handle,pcm);
            int offset=0; while(offset<pcm.length) { int n=audio.write(pcm,offset,pcm.length-offset); if(n<=0) throw new java.io.IOException("Audio output unavailable ("+n+")"); offset+=n; }
            position=Engine.tell(handle);
            if(ended) { playing=false; main.post(()->{ if(repeat) play(queue,index); else next(1); }); }
            else worker.post(this);
        } catch(Exception e) { failed(e); }
    }};
    private void failed(Exception e) { error="Cannot play this track: "+e.getMessage(); playing=false; releaseAudio(); changed(); }
    private void releaseAudio() { playing=false; if(audio!=null) { audio.pause(); audio.flush(); audio.release(); audio=null; } if(handle!=0) { Engine.close(handle); handle=0; } }
    private void changed() { main.post(()->{
        if(current!=null) {
            session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,current.title).putString(MediaMetadata.METADATA_KEY_ARTIST,current.artist).putString(MediaMetadata.METADATA_KEY_ALBUM,current.game).putLong(MediaMetadata.METADATA_KEY_DURATION,current.length).build());
            session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO).setState(playing?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position,playing?1:0).build());
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1,notification());
        }
        if(listener!=null) listener.run();
    }); }
    private PendingIntent action(String name,int code) { return PendingIntent.getService(this,code,new Intent(this,PlaybackService.class).setAction(name),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT); }
    private Notification notification() {
        return new Notification.Builder(this,"music").setSmallIcon(dev.pixelspc.R.drawable.ic_player).setContentTitle(current==null?"PixelSPC":current.title).setContentText(current==null?"Your retro soundtrack":current.game).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).addAction(android.R.drawable.ic_media_previous,"Previous",action("previous",1)).addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pause":"Play",action("toggle",2)).addAction(android.R.drawable.ic_media_next,"Next",action("next",3)).setDeleteIntent(action("stop",4)).setOngoing(playing).build();
    }
    @Override public void onDestroy() {
        unregisterReceiver(noisy); listener=null; manager.abandonAudioFocusRequest(focus); session.release();
        worker.removeCallbacksAndMessages(null); worker.post(()->{ releaseAudio(); thread.quitSafely(); }); super.onDestroy();
    }
}
