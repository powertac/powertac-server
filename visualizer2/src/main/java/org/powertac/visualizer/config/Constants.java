package org.powertac.visualizer.config;

/**
 * Application constants.
 */
public final class Constants {

    //Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";

    public static final String MODE_RESEARCH = "research";
    public static final String MODE_TOURNAMENT = "tournament";

    private Constants() {
    }
}
