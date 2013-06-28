package org.powertac.visualizer.services.competitioncontrol;

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

    String result =  css.simSession(gameParameters.getBootstrapData(),
                                    gameParameters.getServerConfig(),
                                    gameParameters.getJmsUrl(),
                                    gameParameters.getLogSuffix(),
                                    names,
                                    gameParameters.getSeedName(),
                                    gameParameters.getWeatherName(),
                                    null);
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

    String result = css.bootSession(gameParameters.getBootstrapFilename(),
                                    gameParameters.getServerConfig(),
                                    gameParameters.getLogSuffix(),
                                    gameParameters.getSeedName(),
                                    gameParameters.getWeatherName());
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
