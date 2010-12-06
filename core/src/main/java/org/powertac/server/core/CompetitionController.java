package org.powertac.server.core;

import org.joda.time.LocalDateTime;
import org.powertac.common.commands.TimeslotChangedCommand;

public class CompetitionController {

    public TimeslotChangedCommand processTimeslotChange() {
        // Demo implementation. Returns timeslot skeleton.
        Long id = new Long(1);
        LocalDateTime localDateTime = new LocalDateTime();
        Boolean enabled = true;
        TimeslotChangedCommand timeslotChangedCommand = new TimeslotChangedCommand(id, localDateTime, localDateTime, enabled);
        return timeslotChangedCommand;
    }
}
