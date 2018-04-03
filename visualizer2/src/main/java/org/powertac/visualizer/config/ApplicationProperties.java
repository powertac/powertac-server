package org.powertac.visualizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Visualizer 2.
 *
 * <p>
 *     Properties are configured in the application.yml file.
 * </p>
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
    private String mode = "";
    private int timeslotPause = 1000;
    private final Connect connect = new Connect();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getTimeslotPause() {
        return timeslotPause;
    }

    public void setTimeslotPause(int pause) {
        timeslotPause = pause;
    }

    public Connect getConnect() {
        return connect;
    }

    public static class Connect {
        private String machineName = "";
        private String serverUrl = "";
        private String tournamentUrl = "";
        private String tournamentPath = "";

        public String getMachineName() {
            return machineName;
        }

        public void setMachineName(String machineName) {
            this.machineName = machineName;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getTournamentUrl() {
            return tournamentUrl;
        }

        public void setTournamentUrl(String tournamentUrl) {
            this.tournamentUrl = tournamentUrl;
        }

        public String getTournamentPath() {
            return tournamentPath;
        }

        public void setTournamentLogin(String tournamentPath) {
            this.tournamentPath = tournamentPath;
        }
    }
}
