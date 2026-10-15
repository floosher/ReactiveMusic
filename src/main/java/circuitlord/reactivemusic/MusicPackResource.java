package circuitlord.reactivemusic;

import java.io.InputStream;
import java.nio.file.FileSystem;

public class MusicPackResource implements AutoCloseable {

    public FileSystem fileSystem = null;

    public InputStream inputStream = null;

    @Override
    public void close() throws Exception {
        if (inputStream != null) {
            inputStream.close();
        }
        if (fileSystem != null) {
            fileSystem.close();
        }
    }


}
