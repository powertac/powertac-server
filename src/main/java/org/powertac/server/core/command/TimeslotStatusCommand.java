package org.powertac.server.core.command;

import org.joda.time.LocalDateTime;

import javax.xml.bind.annotation.XmlElement;


public class TimeslotStatusCommand {

    @XmlElement
    private LocalDateTime startDate;
    @XmlElement
    private LocalDateTime endDate;

}
