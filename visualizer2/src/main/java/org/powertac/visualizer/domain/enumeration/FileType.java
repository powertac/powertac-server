package org.powertac.visualizer.domain.enumeration;

import java.io.File;

import org.powertac.visualizer.domain.User;

/**
 * The FileType enumeration.
 */
public enum FileType {

    TRACE, STATE, SEED, BOOT, CONFIG, WEATHER, ANY;

    public static final String DIRECTORY_ROOT = "files";
    public static final String DIRECTORY_LOG = "log";
    public static final String DIRECTORY_SEED = "seed";
    public static final String DIRECTORY_BOOT = "boot";
    public static final String DIRECTORY_CONFIG = "config";
    public static final String DIRECTORY_WEATHER = "weather";

    public final String getContentType () {
        if (this.equals(ANY)) {
            throw new IllegalArgumentException();
        }
        switch (this) {
            case TRACE:
            case STATE:
            case SEED:
            case CONFIG:
                return "text/plain";
            default:
                return "application/xml";
        }
    }

    public final File getDirectory (User user) {
        if (this.equals(ANY)) {
            throw new IllegalArgumentException();
        }
        File directory = new File(DIRECTORY_ROOT);
        if (user != null) {
            directory = new File(directory, user.getLogin());
        } else {
            directory = new File(directory, "system");
        }
        switch (this) {
            case TRACE:
            case STATE:
                directory = new File(directory, DIRECTORY_LOG);
                break;
            case SEED:
                directory = new File(directory, DIRECTORY_SEED);
                break;
            case BOOT:
                directory = new File(directory, DIRECTORY_BOOT);
                break;
            case CONFIG:
                directory = new File(directory, DIRECTORY_CONFIG);
                break;
            case WEATHER:
                directory = new File(directory, DIRECTORY_WEATHER);
                break;
            default:
                throw new RuntimeException("Unhandled case " + this);
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public final File getFile (User user, String name) {
        return name == null ? null : new File(this.getDirectory(user), name);
    }

}
