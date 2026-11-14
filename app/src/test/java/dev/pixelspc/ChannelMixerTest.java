package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChannelMixerTest {
    @Test public void soloTemporarilyOverridesMutesThenRestoresThem(){
        ChannelMixer state=new ChannelMixer(null,new String[]{"Bass","Lead","Noise"},0,-1);
        state=state.mute(0,true);assertEquals(1,state.mask());
        state=state.solo(0);assertTrue(state.audible(0));assertFalse(state.audible(1));
        state=state.mute(2,true).solo(1);assertFalse(state.audible(0));assertTrue(state.audible(1));
        state=state.solo(1);assertEquals(5,state.mask());assertFalse(state.audible(2));
        state=state.reset();assertEquals(0,state.mask());assertEquals(-1,state.solo);
    }
    @Test public void highestVoiceBitAndSnapshotsRemainIndependent(){
        String[] names=new String[32];names[0]="Original";ChannelMixer old=new ChannelMixer(null,names,0,-1);names[0]="Changed";
        ChannelMixer next=old.mute(31,true);assertEquals("Original",old.names[0]);assertEquals(0,old.mask());assertFalse(next.audible(31));assertTrue(next.solo(31).audible(31));
    }
}
