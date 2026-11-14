package dev.pixelspc;

/** Immutable audio-worker snapshot. Hardware voices can be reused by many instruments. */
final class ChannelMixer {
    final Library.Track track;
    final String[] names;
    final int muted,solo;
    ChannelMixer(Library.Track track,String[] names,int muted,int solo){
        if(names.length>32)throw new IllegalArgumentException("Too many channels");
        this.track=track;this.names=names.clone();this.muted=muted;this.solo=solo;
    }
    static ChannelMixer empty(){return new ChannelMixer(null,new String[0],0,-1);}
    int mask(){return solo<0?muted:~(1<<solo);}
    boolean muted(int channel){return (muted&(1<<channel))!=0;}
    boolean audible(int channel){return (mask()&(1<<channel))==0;}
    ChannelMixer mute(int channel,boolean value){return new ChannelMixer(track,names,value?muted|(1<<channel):muted&~(1<<channel),solo);}
    ChannelMixer solo(int channel){return new ChannelMixer(track,names,muted,solo==channel?-1:channel);}
    ChannelMixer reset(){return new ChannelMixer(track,names,0,-1);}
}
