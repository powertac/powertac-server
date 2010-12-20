package org.powertac.server.module.competitioncontroller;

import org.joda.time.LocalDateTime;
import org.powertac.common.commands.CompetitionChanged;
import org.powertac.common.commands.TimeslotChanged;
import org.powertac.server.module.databaseservice.domain.Competition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CompetitionController {

  final static Logger log = LoggerFactory.getLogger(CompetitionController.class);

  private Competition competition;
  private final Lock cronLock = new ReentrantLock();

  /**
   * Creates the initial competition
   */
  public CompetitionController() {
    setCompetition(new Competition());
  }

  public Competition getCompetition() {
    return competition;
  }

  public void setCompetition(Competition competition) {
    this.competition = competition;
  }

  /**
   * Periodically called cron job that handles competition management
   *
   * @return CompetitionChanged command object used to notify other modules
   */
  public CompetitionChanged processCronRun() {

    CompetitionChanged competitionChangedCommand = null;

    // Account for the case that processing might take longer than the specified cron interval
    if (cronLock.tryLock()) {
      try {
        switch (competition.getCompetitionStatus()) {
          case Idle:
            // Try to load competition configuration from somewhere
            competitionChangedCommand = loadConfiguration();
            break;
          case Scheduled:
            // Try to call the initialize methods of all available modules
            // Q: In which order
            competitionChangedCommand = initializeModules();
            break;
          case Initialized:
            // Start the competition
            competitionChangedCommand = startCompetition();
            break;
          case Running:
            // Nothing needed here - cancel cron job?
            break;
          case Finished:
            // Nothing needed here - cancel cron job?
            break;
          case Interrupted:
            // Nothing needed here - cancel cron job?
            break;
        }
      } finally {
        cronLock.unlock();
      }
    }

    return competitionChangedCommand;
  }

  /**
   * Try to load the competition configuration
   *
   * @return CompetitionChanged command with status set to scheduled if loading was successful. Return null otherwise
   */
  private CompetitionChanged loadConfiguration() {
    return null;
  }

  /**
   * Give all modules a chance to initialize themselves
   *
   * @return CompetitionChanged command with status set to initialized
   */
  private CompetitionChanged initializeModules() {
    return null;
  }

  /**
   * Start the competition
   *
   * @return CompetitionChanged command with status set to running
   */
  private CompetitionChanged startCompetition() {
    return null;
  }

  // demo method
  public TimeslotChanged processTimeslotChange() {
    // Demo implementation. Returns timeslot skeleton.
    Long id = new Long(1);
    LocalDateTime localDateTime = new LocalDateTime();
    Boolean enabled = true;
    TimeslotChanged timeslotChanged = new TimeslotChanged(id, localDateTime, localDateTime, enabled);
    return timeslotChanged;
  }
}
