package circuitlord.reactivemusic;

import rm_javazoom.jl.player.AudioDevice;

public interface RMAudioPlayer {
    void play() throws Exception;
    void stop();
    boolean isComplete();
    AudioDevice getAudioDevice();
    void close();
}
