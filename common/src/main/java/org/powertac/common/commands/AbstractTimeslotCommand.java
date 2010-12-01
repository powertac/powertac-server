package org.powertac.common.commands;

import org.joda.time.LocalDateTime;

/**
 * User: cblock
 * Date: 01.12.10
 * Time: 16:42
 */
public abstract class AbstractTimeslotCommand {
  Long timeslotId;
  LocalDateTime startDate;
  LocalDateTime endDate;
  Boolean enabled;
}
