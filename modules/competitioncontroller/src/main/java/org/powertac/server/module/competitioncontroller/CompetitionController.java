package org.powertac.server.module.competitioncontroller;

import org.joda.time.LocalDateTime;
import org.powertac.common.commands.TimeslotChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionController {

    final static Logger log = LoggerFactory.getLogger(CompetitionController.class);

    public TimeslotChanged processTimeslotChange() {
        // Demo implementation. Returns timeslot skeleton.
        Long id = new Long(1);
        LocalDateTime localDateTime = new LocalDateTime();
        Boolean enabled = true;
        TimeslotChanged timeslotChanged = new TimeslotChanged(id, localDateTime, localDateTime, enabled);
        return timeslotChanged;
    }
}
