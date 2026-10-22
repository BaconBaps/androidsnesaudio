package dev.pixelspc;

import java.io.IOException;

/** Bounds each decoder call so the audio worker can process controls between steps. */
final class PlaybackSeek {
    interface Decoder {
        int tell();
        void seek(int ms) throws IOException;
        void restoreMode();
    }
    private int target;
    private boolean active;
    void begin(int ms) { target=Math.max(0,ms); active=true; }
    void cancel() { active=false; }
    boolean active() { return active; }
    void step(Decoder decoder) throws IOException {
        if(!active) return;
        int before=decoder.tell();
        // A backwards seek restarts the emulator. Restore loop/fade state before
        // emulating forward, including when silence detection was disabled.
        int next=target<before?0:(int)Math.min((long)target,(long)before+250);
        decoder.seek(next);
        decoder.restoreMode();
        int after=decoder.tell();
        if(next>before && after<=before) throw new IOException("Decoder seek made no progress");
        if(after>=target) active=false;
    }
}
