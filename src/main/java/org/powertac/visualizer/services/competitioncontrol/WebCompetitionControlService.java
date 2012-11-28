package org.powertac.visualizer.services.competitioncontrol;

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.CompetitionSetup;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.visualizer.services.VisualizerServiceEmbedded;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose of this service is to allow (very) primitive control of PowerTAC
 * simulator. Feel free to change this class to match your requirements.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class WebCompetitionControlService
{
  static private Logger log = Logger
          .getLogger(WebCompetitionControlService.class);

  @Autowired
  private CompetitionSetup css;

  @Autowired
  private CompetitionControl competitionControl;

  @Autowired
  private VisualizerServiceEmbedded visualizerService;

  @Autowired
  private GameParametersBean gameParameters;

  @Autowired
  private VisualizerProxy visualizerProxy;

  private Boolean tournamentMode = false;
  private String message;

  public void runSim ()
  {
    // We're in tournament mode
    if (tournamentMode) {
      message = "Unable to run sim in tournament configuration.";
      return;
    }
    // Already running
    else if (competitionControl.isRunning()) {
      message =
          "Unable to run a sim game. The competition is already in progress.";
      return;
    }

    visualizerService.init(visualizerProxy);

    List<String> names = new ArrayList<String>();
    for (FakeBroker type : gameParameters.getBrokers()) {
      names.add(type.getName());
    }
    // web components treat empty forms as "", not null.
    String boot = gameParameters.getBootstrapData().trim()
        .equals("") ? null : gameParameters.getBootstrapData();
    String serverConfig = gameParameters.getServerConfig().trim()
        .equals("") ? null : gameParameters.getServerConfig();
    String jmsUrl = gameParameters.getJmsUrl().trim()
        .equals("") ? null : gameParameters.getJmsUrl();
    String logSuffix = gameParameters.getLogSuffix().trim()
        .equals("") ? null : gameParameters.getLogSuffix();

    String result =
        css.simSession(boot, serverConfig, jmsUrl, logSuffix, names, null);
    if (result == null) {
      message = "Simulation started.";
    }
    else {
      message = "ERROR: " + result;
    }
  }

  public void runBoot ()
  {
    // We're in tournament mode
    if (tournamentMode) {
      message = "Unable to run boot in tournament configuration.";
      return;
    }
    // Already running
    else if (competitionControl.isRunning()) {
      message = "Unable to run a bootstrap game. "
                + "The competition is already in progress.";
      return;
    }

    visualizerService.init(visualizerProxy);

    // web components treat empty forms as "", not null.
    String bootFilename = gameParameters.getBootstrapFilename().trim()
        .equals("") ? null : gameParameters.getBootstrapFilename();
    String serverConfig = gameParameters.getServerConfig().trim()
        .equals("") ? null : gameParameters.getServerConfig();
    String logSuffix = gameParameters.getLogSuffix().trim()
        .equals("") ? null : gameParameters.getLogSuffix();

    String result = css.bootSession(bootFilename, serverConfig, logSuffix);
    if (result == null) {
      message = "Bootstrap mode started.";
    }
    else {
      message = "ERROR: " + result;
    }
  }

  public void shutDown ()
  {
    // We're in tournament mode
    if (tournamentMode) {
      message = "Not allowed in tournament configuration";
    }
    else if (competitionControl.isRunning()) {
      competitionControl.shutDown();
      message = "Shut down is complete.";
    }
    else {
      message = "There is no running game to shut down.";
    }
  }

  public String getMessage ()
  {
    return message;
  }

  public Boolean getTournamentMode() {
    return tournamentMode;
  }
  public void setTournamentMode(Boolean tournamentMode) {
    this.tournamentMode = tournamentMode;
  }
}
