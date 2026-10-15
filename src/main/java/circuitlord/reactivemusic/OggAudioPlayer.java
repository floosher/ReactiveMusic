package circuitlord.reactivemusic;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import rm_javazoom.jl.decoder.JavaLayerException;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.FactoryRegistry;
import rm_javazoom.jl.player.JavaSoundAudioDevice;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class OggAudioPlayer implements RMAudioPlayer {

    private final InputStream stream;
    private AudioDevice audio;
    private long decoder = 0;
    private volatile boolean queuedToStop = false;
    private boolean closed = false;
    private boolean complete = false;

    public OggAudioPlayer(InputStream stream) throws JavaLayerException {
        this(stream, null);
    }

    public OggAudioPlayer(InputStream stream, AudioDevice device) throws JavaLayerException {
        this.stream = stream;
        if (device != null) {
            this.audio = device;
        } else {
            this.audio = FactoryRegistry.systemRegistry().createAudioDevice();
        }
    }

    @Override
    public AudioDevice getAudioDevice() {
        return audio;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void stop() {
        queuedToStop = true;
    }

    @Override
    public void play() throws Exception {
        try {
            byte[] rawData = readAllBytes(stream);
            if (rawData.length == 0) {
                complete = true;
                return;
            }

            ByteBuffer buffer = BufferUtils.createByteBuffer(rawData.length);
            buffer.put(rawData);
            buffer.flip();

            IntBuffer error = BufferUtils.createIntBuffer(1);
            decoder = STBVorbis.stb_vorbis_open_memory(buffer, error, null);
            if (decoder == 0) {
                throw new IOException("Failed to open OGG Vorbis stream, error: " + error.get(0));
            }

            STBVorbisInfo info = STBVorbisInfo.malloc();
            int channels;
            int sampleRate;
            try {
                STBVorbis.stb_vorbis_get_info(decoder, info);
                channels = info.channels();
                sampleRate = info.sample_rate();
            } finally {
                info.free();
            }

            if (audio == null) {
                audio = FactoryRegistry.systemRegistry().createAudioDevice();
            }

            if (audio instanceof JavaSoundAudioDevice) {
                AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
                ((JavaSoundAudioDevice) audio).open(format);
            }

            int chunkSamples = 4096;
            ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(chunkSamples * channels);
            short[] sampleArray = new short[chunkSamples * channels];

            while (!queuedToStop && !closed) {
                pcmBuffer.clear();
                int samplesPerChannel = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcmBuffer);
                if (samplesPerChannel <= 0) {
                    break;
                }

                int totalSamples = samplesPerChannel * channels;
                pcmBuffer.get(sampleArray, 0, totalSamples);

                AudioDevice out = audio;
                if (out != null) {
                    synchronized (this) {
                        if (audio != null && !queuedToStop && !closed) {
                            audio.write(sampleArray, 0, totalSamples);
                        }
                    }
                }
            }

            AudioDevice out = audio;
            if (out != null) {
                out.flush();
                synchronized (this) {
                    complete = (!closed && !queuedToStop);
                    close();
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            AudioDevice out = audio;
            audio = null;
            if (out != null) {
                out.close();
            }
            if (decoder != 0) {
                STBVorbis.stb_vorbis_close(decoder);
                decoder = 0;
            }
            try {
                stream.close();
            } catch (Exception ignored) {}
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
