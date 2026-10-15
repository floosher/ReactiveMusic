package circuitlord.reactivemusic;

import rm_javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class RMAudioPlayerFactory {

    public static RMAudioPlayer createPlayer(InputStream inputStream) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(16);
        byte[] header = new byte[4];
        int count = bis.read(header);
        bis.reset();

        if (count >= 4 && header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S') {
            return new OggAudioPlayer(bis);
        } else {
            return new AdvancedPlayer(bis);
        }
    }
}
