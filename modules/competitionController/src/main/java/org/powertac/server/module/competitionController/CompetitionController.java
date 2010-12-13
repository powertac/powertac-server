package org.powertac.server.module.competitionController;

import org.joda.time.LocalDateTime;
import org.powertac.common.commands.TimeslotChanged;

public class CompetitionController {

    public TimeslotChanged processTimeslotChange() {
        // Demo implementation. Returns timeslot skeleton.
        Long id = new Long(1);
        LocalDateTime localDateTime = new LocalDateTime();
        Boolean enabled = true;
        TimeslotChanged timeslotChanged = new TimeslotChanged(id, localDateTime, localDateTime, enabled);
        return timeslotChanged;
    }
}
