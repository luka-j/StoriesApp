package rs.lukaj.android.stories.controller;

import java.io.File;
import java.io.IOException;

/**
 * Created by luka on 23.1.16..
 */
public class FileIOException extends IOException {
    public FileIOException(File file, String message) {
        super(file.getAbsolutePath() + ": " + message);
    }
}
