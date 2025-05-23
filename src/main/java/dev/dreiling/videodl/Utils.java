package dev.dreiling.videodl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Utils {

    /**
     * Extracts a native executable from the jar resource /bin to a temporary file
     */
    public static File extractExecutable(String exeName) throws IOException {
        String resourcePath = "/bin/" + exeName;
        InputStream in = Utils.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        // Create a temp file with .exe suffix
        File tempFile = File.createTempFile(exeName, ".exe");
        tempFile.deleteOnExit();

        // Copy resource contents to temp file
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        in.close();

        // Make sure it is executable (Windows normally doesn't need this, but no harm)
        tempFile.setExecutable(true);

        return tempFile;
    }
}
