package org.powertac.common.enumerations;

public enum CompetitionStatus {
  Created(1),
  Scheduled(2),
  Initialized(3),
  Ready(4),
  Running(5),
  Finished(6),
  Interrupted(7);

  private final int idVal;

  CompetitionStatus(int id) {
    this.idVal = id;
  }

  public int getId() {
    return idVal;
  }
}

